# Reglas de Negocio de CiudadAlerta

Este documento especifica el funcionamiento del modelo de datos, la gestión de estados de incidencias y las constantes de configuración de la plataforma **CiudadAlerta**.

---

## 1. Comportamiento de "Sigue Ahí" en Incidencias RESUELTAS

Cuando un usuario interactúa con la opción **"Sigue ahí"** (`marcarSigueAhi`) sobre una incidencia que actualmente tiene el estado `RESUELTA`:

1. **Reactivación del Estado:** El estado de la incidencia pasa inmediatamente de `RESUELTA` a `ACTIVA`.
2. **Reinicio de Marcas de Resuelto:** Se restablece el contador `marcasResuelto` a `0`.
3. **Eliminación del Timestamp de Resuelto:** Se remueve el campo `fechaResuelta` del documento en Firestore.
4. **Incremento de Confirmaciones:** Se incrementa en `1` el contador `contadorConfirmaciones`.
5. **Actualización de Verificación:** Se actualiza `ultimaVerificacionNotificada` con la fecha y hora actual (`System.currentTimeMillis()`).

---

## 2. Parámetros y Constantes del Sistema (`CiudadAlertaConfig`)

- **X (`TIEMPO_REVERIFICACION_X_HORAS = 48L`):**  
  Tiempo límite en horas transcurrido desde la creación o la última verificación notificada de una incidencia activa. Superado este lapso, las Cloud Functions programadas enviarán una solicitud de confirmación a los reportantes.

- **N (`MARCAS_PARA_RESOLVER_N = 3`):**  
  Número mínimo de marcas independientes de "marcar resuelto" requeridas para que una incidencia pase automáticamente de estado `ACTIVA` a `RESUELTA`.

- **M (`DENUNCIAS_PARA_OCULTAR_M = 3`):**  
  Número de denuncias independientes requeridas para que una incidencia se marque como `ocultoDelMapa = true` y no sea visible para los usuarios en el mapa.

- **Días de Visibilidad Resuelta (`DIAS_VISIBLE_RESUELTA = 4L`):**  
  Tiempo en días durante el cual una incidencia en estado `RESUELTA` permanece visible en el mapa. Cumplido este periodo (`ahora - fechaResuelta > 4 días`), deja de mostrarse en las consultas del cliente.

- **Límite Diario de Reportes (`MAX_REPORTES_POR_USUARIO_DIA = 5`):**  
  Cantidad máxima de incidencias que un mismo usuario puede reportar dentro de una ventana de 24 horas.
