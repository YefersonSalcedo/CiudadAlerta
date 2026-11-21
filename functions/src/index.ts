import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { setGlobalOptions } from "firebase-functions/v2";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

admin.initializeApp();

// Configuración global para Cloud Functions v2
setGlobalOptions({
  region: "us-central1",
  maxInstances: 3
});

/**
 * Tiempo en horas para requerir la re-verificación de una incidencia activa.
 * Debe coincidir con CiudadAlertaConfig.TIEMPO_REVERIFICACION_X_HORAS en la app Android (48 horas).
 */
const TIEMPO_REVERIFICACION_X_HORAS = 48;

const NOMBRE_ESTADO: Record<string, string> = {
  ACTIVA: "activa",
  RESUELTA: "resuelta",
  ARCHIVADA: "archivada",
};

/**
 * Función 1: notificarSeguidores
 * Trigger: Modificación de un documento en incidencias/{id}
 * Si cambia el estado, o aumenta contadorReportes o contadorConfirmaciones,
 * notifica a seguidoresIds (excluyendo a quien generó el cambio si es deducible).
 */
export const notificarSeguidores = onDocumentUpdated(
  "incidencias/{id}",
  async (event) => {
    const change = event.data;
    if (!change) return;

    const before = change.before.data();
    const after = change.after.data();
    if (!before || !after) return;

    const incidenciaId = event.params.id;

    // Verificar si hubo cambios relevantes en estado, reportes o confirmaciones
    const cambioEstado = before.estado !== after.estado;
    const masReportes = (after.contadorReportes || 0) > (before.contadorReportes || 0);
    const masConfirmaciones = (after.contadorConfirmaciones || 0) > (before.contadorConfirmaciones || 0);

    if (!cambioEstado && !masReportes && !masConfirmaciones) {
      return;
    }

    const seguidoresIds: string[] = after.seguidoresIds || [];
    if (seguidoresIds.length === 0) {
      return;
    }

    // Deducir usuario causante del cambio para no autonotificarlo
    let usuarioExcluido: string | null = null;
    if (masReportes) {
      const reportantesBefore: string[] = before.reportantesIds || [];
      const reportantesAfter: string[] = after.reportantesIds || [];
      const nuevos = reportantesAfter.filter((id) => !reportantesBefore.includes(id));
      if (nuevos.length > 0) {
        usuarioExcluido = nuevos[nuevos.length - 1];
      }
    }

    const destinatarios = seguidoresIds.filter((uid) => uid !== usuarioExcluido);
    if (destinatarios.length === 0) return;

    // Generar título y mensaje
    let titulo = `Actualización: ${after.titulo || "Incidencia"}`;
    let cuerpo = "Hay novedades en una incidencia que sigues.";

    if (cambioEstado) {
          cuerpo = before.estado === "RESUELTA" && after.estado === "ACTIVA"
            ? "Alguien confirmó que sigue ahí: la incidencia se reactivó."
            : `La incidencia ahora está ${NOMBRE_ESTADO[after.estado] ?? after.estado}.`;
    } else if (masConfirmaciones) {
      cuerpo = `Un ciudadano confirmó que la incidencia sigue presente.`;
    } else if (masReportes) {
      cuerpo = `Un nuevo ciudadano reportó este problema.`;
    }

    logger.info(`Notificando a ${destinatarios.length} seguidores de la incidencia ${incidenciaId}`);

    for (const uid of destinatarios) {
      await enviarNotificacionAUsuario(
        uid,
        "INCIDENCIA_ACTUALIZADA",
        incidenciaId,
        titulo,
        cuerpo
      );
    }
  }
);

/**
 * Función 2: verificar48h
 * Trigger: Programado (onSchedule) cada 6 horas
 * Busca incidencias activas cuya ultimaVerificacionNotificada o fechaCreacion supere las 48 horas.
 * Envía alerta de re-verificación a reportantesIds y actualiza ultimaVerificacionNotificada.
 */
export const verificar48h = onSchedule(
  {
    schedule: "0 */6 * * *",
    timeZone: "America/Bogota"
  },
  async () => {
    const ahora = Date.now();
    const limiteMs = TIEMPO_REVERIFICACION_X_HORAS * 3600 * 1000;
    const limiteVerificacion = ahora - limiteMs;

    logger.info(`Ejecutando verificar48h. Límite de verificación: ${new Date(limiteVerificacion).toISOString()}`);

    const snapshot = await admin.firestore()
      .collection("incidencias")
      .where("estado", "==", "ACTIVA")
      .where("ultimaVerificacionNotificada", "<=", limiteVerificacion)
      .get();

    if (snapshot.empty) {
      logger.info("No hay incidencias que requieran solicitud de re-verificación.");
      return;
    }

    for (const doc of snapshot.docs) {
      const inc = doc.data();
      const fechaCreacion = inc.fechaCreacion || 0;

      // Filtrar también en código por fechaCreacion
      if ((ahora - fechaCreacion) < limiteMs) {
        continue;
      }

      const incidenciaId = doc.id;
      const reportantesIds: string[] = inc.reportantesIds || [];
      if (reportantesIds.length === 0) continue;

      const titulo = "¿Sigue presente la incidencia?";
      const cuerpo = `Por favor confirma si "${inc.titulo || "esta incidencia"}" aún requiere atención en tu zona.`;

      logger.info(`Solicitando re-verificación para incidencia ${incidenciaId} a ${reportantesIds.length} reportantes`);

      for (const uid of reportantesIds) {
        await enviarNotificacionAUsuario(
          uid,
          "REVERIFICACION",
          incidenciaId,
          titulo,
          cuerpo
        );
      }

      // Actualizar timestamp para evitar repetir notificación en las próximas 48h
      await doc.ref.update({
        ultimaVerificacionNotificada: Date.now()
      });
    }
  }
);

/**
 * Función auxiliar para guardar el documento de notificación en Firestore y enviar el mensaje Push FCM data-only.
 */
async function enviarNotificacionAUsuario(
  uid: string,
  tipo: "INCIDENCIA_ACTUALIZADA" | "REVERIFICACION",
  incidenciaId: string,
  titulo: string,
  cuerpo: string
): Promise<void> {
  const db = admin.firestore();

  // 1. Guardar la alerta en usuarios/{uid}/notificaciones
  try {
    await db.collection("usuarios").doc(uid).collection("notificaciones").add({
      tipo,
      incidenciaId,
      titulo,
      cuerpo,
      fecha: Date.now(),
      leida: false
    });
  } catch (err) {
    logger.error(`Error guardando notificación Firestore para usuario ${uid}:`, err);
  }

  // 2. Obtener fcmToken del usuario
  try {
    const userDoc = await db.collection("usuarios").doc(uid).get();
    if (!userDoc.exists) return;

    const fcmToken = userDoc.data()?.fcmToken;
    if (!fcmToken || typeof fcmToken !== "string" || fcmToken.trim() === "") {
      return;
    }

    // 3. Enviar mensaje FCM DATA-ONLY (sin el campo "notification")
    const message: admin.messaging.Message = {
      token: fcmToken,
      data: {
        titulo: titulo,
        cuerpo: cuerpo,
        incidenciaId: incidenciaId,
        tipo: tipo
      },
      android: {
        priority: "high"
      }
    };

    await admin.messaging().send(message);
    logger.info(`Notificación Push FCM data-only enviada exitosamente a ${uid}`);
  } catch (error: any) {
    logger.error(`Error enviando FCM a usuario ${uid}:`, error);

    // Si el token es inválido o no está registrado, eliminarlo de la base de datos
    const errorCode = error?.code || error?.errorInfo?.code;
    if (
      errorCode === "messaging/invalid-registration-token" ||
      errorCode === "messaging/registration-token-not-registered"
    ) {
      logger.info(`Eliminando fcmToken inválido para el usuario ${uid}`);
      await db.collection("usuarios").doc(uid).update({
        fcmToken: admin.firestore.FieldValue.delete()
      }).catch((e) => logger.error(`Error al eliminar token de usuario ${uid}:`, e));
    }
  }
}
