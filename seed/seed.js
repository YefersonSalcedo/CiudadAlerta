/**
 * Seed de datos de prueba para CiudadAlerta.
 *
 * Uso:
 *   cd seed && npm init -y && npm i firebase-admin ngeohash
 *   (poner serviceAccount.json en esta carpeta; NO subirlo a Git)
 *
 *   MI_UID=<tu uid de Authentication> MI_NOMBRE="Yeferson" \
 *   STORAGE_BUCKET=<tu-proyecto>.firebasestorage.app \
 *   node seed.js            # carga los datos
 *   node seed.js --clean    # borra todo lo que empiece por "demo-"
 *
 * Fotos (opcional): pon .jpg en seed/fotos/ con los nombres usados abajo.
 */
const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore, FieldPath } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const ngeohash = require("ngeohash");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

initializeApp({
  credential: cert(require("./serviceAccount.json")),
  storageBucket: process.env.STORAGE_BUCKET,
});
const db = getFirestore();

// ---- Ajusta estos valores a CiudadAlertaConfig.kt ----
const N = 3;               // MARCAS_PARA_RESOLVER_N
const M = 3;               // DENUNCIAS_PARA_OCULTAR_M
const X_HORAS = 48;        // TIEMPO_REVERIFICACION_X_HORAS
const DIAS_VISIBLE = 3;    // DIAS_VISIBLE_RESUELTA
// -------------------------------------------------------

const ahora = Date.now();
const horas = (x) => ahora - x * 3600e3;
const dias = (x) => ahora - x * 86400e3;

const ME = process.env.MI_UID || null;
const ME_NOMBRE = process.env.MI_NOMBRE || "Yo";

const usuarios = [
  { id: "demo-user-1", nombre: "Laura Gómez", correo: "laura.demo@example.com", puntosCivicos: 40, nivel: 2 },
  { id: "demo-user-2", nombre: "Carlos Ruiz", correo: "carlos.demo@example.com", puntosCivicos: 15, nivel: 1 },
  { id: "demo-user-3", nombre: "Marta Díaz", correo: "marta.demo@example.com", puntosCivicos: 90, nivel: 3 },
  { id: "demo-user-4", nombre: "Andrés Mora", correo: "andres.demo@example.com", puntosCivicos: 5, nivel: 1 },
];
const nombreDe = (uid) =>
  uid === ME ? ME_NOMBRE : (usuarios.find((u) => u.id === uid) || {}).nombre || "Ciudadano";
const [U1, U2, U3, U4] = usuarios.map((u) => u.id);

// Coordenadas base: Tarazá, Antioquia (ajústalas a donde vayas a probar)
const BASE = { lat: 7.5778, lng: -75.4044 };
const pos = (dLat, dLng) => ({ lat: BASE.lat + dLat, lng: BASE.lng + dLng });

// Nombres de categoría EXACTOS de Categoria.kt (se guarda el nombre, no el objeto)
const CAT = {
  vias: "Vías",
  alumbrado: "Alumbrado público",
  residuos: "Residuos y aseo",
  parques: "Parques y zonas verdes",
  semaforos: "Semáforos",
  arboles: "Árboles (sin riesgo inminente)",
  animales: "Animales abandonados",
  otros: "Otros",
};

const DESC_LARGA =
  "Desde hace varias semanas hay varios perros abandonados en el lote junto a la escuela. " +
  "Están flacos, se pelean por la comida y los vecinos ya no se atreven a pasar por la acera. " +
  "Se necesita que la alcaldía o una fundación de protección animal los recoja y les dé atención veterinaria.";

// Cada elemento genera: 1 incidencia + sus reportes + comentarios + marcas + denuncias.
const incidencias = [
  {
    id: "demo-residuos-destacada", titulo: "Acumulación de residuos sólidos y bolsas de basura",
    cat: CAT.residuos, ...pos(0.0010, 0.0008), dir: "Cl. 36 #27-27, Tarazá, Antioquia", sev: "Alta",
    desc: "Bolsas de basura acumuladas en la esquina desde hace días.", foto: "residuos.jpg",
    creada: horas(5), reportantes: [U1, U2, U3, U4], confirmaciones: 2,
    comentarios: [
      [U2, "Sigue igual esta mañana, ya huele muy mal.", horas(4)],
      [U3, "El camión no pasó el martes.", horas(3)],
      [U4, "Hay moscas y perros rompiendo las bolsas.", horas(1)],
    ],
  },
  {
    id: "demo-via-48h", titulo: "Hueco grande en la vía principal",
    cat: CAT.vias, ...pos(-0.0012, 0.0015), dir: "Cra. 30 con Cl. 34, Tarazá, Antioquia", sev: "Media",
    desc: "Hueco de unos 60 cm que daña las llantas de las motos.", foto: "hueco.jpg",
    creada: horas(X_HORAS + 12), verificada: horas(X_HORAS + 12),
    reportantes: ME ? [ME, U1] : [U1], // el aviso de 48 h solo debe verlo quien reportó
    comentarios: [[U1, "Ayer casi se cae una moto aquí.", horas(30)]],
  },
  {
    id: "demo-alumbrado-casi-resuelta", titulo: "Poste de alumbrado sin luz",
    cat: CAT.alumbrado, ...pos(0.0020, -0.0010), dir: "Cl. 38 #29-10, Tarazá, Antioquia", sev: "Media",
    desc: "La luminaria lleva una semana apagada.", foto: "alumbrado.jpg",
    creada: dias(7), reportantes: [U2, U3], marcas: N - 1,
  },
  {
    id: "demo-parque-resuelta", titulo: "Juegos infantiles dañados en el parque",
    cat: CAT.parques, ...pos(-0.0020, -0.0020), dir: "Parque Principal, Tarazá, Antioquia", sev: "Baja",
    desc: "El columpio estaba roto; ya lo repararon.", foto: "parque.jpg",
    creada: dias(6), estado: "RESUELTA", resuelta: dias(1), reportantes: [U1], marcas: N,
  },
  {
    id: "demo-resuelta-vencida", titulo: "Caño tapado en la esquina (ya resuelto)",
    cat: CAT.otros, ...pos(0.0030, 0.0025), dir: "Cl. 40 #25-12, Tarazá, Antioquia", sev: "Baja",
    desc: "Resuelta hace más días de los visibles: NO debe aparecer en el mapa.",
    creada: dias(15), estado: "RESUELTA", resuelta: dias(DIAS_VISIBLE + 5), reportantes: [U3], marcas: N,
  },
  {
    id: "demo-semaforo-casi-oculta", titulo: "Semáforo intermitente",
    cat: CAT.semaforos, ...pos(-0.0030, 0.0030), dir: "Cra. 31 con Cl. 36, Tarazá, Antioquia", sev: "Media",
    desc: "El semáforo parpadea en amarillo todo el día.", creada: horas(20),
    reportantes: [U4], denuncias: M - 1,
  },
  {
    id: "demo-arbol-oculta", titulo: "Contenido denunciado (oculto del mapa)",
    cat: CAT.arboles, ...pos(0.0040, -0.0030), dir: "Cl. 42 #20-05, Tarazá, Antioquia", sev: "Baja",
    desc: "Llegó a M denuncias: NO debe aparecer en el mapa.", creada: horas(40),
    reportantes: [U2], denuncias: M, oculto: true,
  },
  {
    id: "demo-animales-descripcion-larga", titulo: "Perros abandonados junto a la escuela",
    cat: CAT.animales, ...pos(0.0005, -0.0035), dir: "Cl. 33 #26-40, Tarazá, Antioquia", sev: "Alta",
    desc: DESC_LARGA, foto: "animales.jpg", creada: horas(10),
    reportantes: [U1], seguidores: ME ? [ME] : [U2],
  },
  {
    id: "demo-otros-nueva", titulo: "Andén levantado por raíces",
    cat: CAT.otros, ...pos(-0.0005, 0.0040), dir: "Cl. 35 #28-18, Tarazá, Antioquia", sev: "Baja",
    desc: "El andén está levantado y es difícil pasar con coches de bebé.", creada: horas(1),
    reportantes: [ME || U3],
  },
];

async function subirFoto(nombre, incId) {
  if (!nombre || !process.env.STORAGE_BUCKET) return "";
  const local = path.join(__dirname, "fotos", nombre);
  if (!fs.existsSync(local)) return "";
  const bucket = getStorage().bucket();
  const dest = `incidencias/${incId}_${nombre}`;
  const token = crypto.randomUUID();
  await bucket.upload(local, {
    destination: dest,
    metadata: { contentType: "image/jpeg", metadata: { firebaseStorageDownloadTokens: token } },
  });
  return `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(dest)}?alt=media&token=${token}`;
}

async function seed() {
  const bw = db.bulkWriter();

  for (const u of usuarios) {
    const { id, ...data } = u;
    bw.set(db.collection("usuarios").doc(id), { ...data, fotoUrl: null, fcmToken: null });
  }

  for (const s of incidencias) {
    const foto = await subirFoto(s.foto, s.id);
    const estado = s.estado || "ACTIVA";
    const marcas = s.marcas || 0;
    const denuncias = s.denuncias || 0;

    // Mismos campos y tipos que la data class Incidencia (fechas = Long en ms)
    bw.set(db.collection("incidencias").doc(s.id), {
      titulo: s.titulo,
      categoria: s.cat,
      ubicacion: {
        latitud: s.lat,
        longitud: s.lng,
        direccionAproximada: s.dir,
        geohash: ngeohash.encode(s.lat, s.lng, 9), // ajusta la precisión a GeoUtils.encodeGeohash
      },
      contadorReportes: s.reportantes.length,
      contadorConfirmaciones: s.confirmaciones || 0,
      estado,
      fotoPrincipalUrl: foto,
      marcasResuelto: marcas,
      denunciasCount: denuncias,
      reportantesIds: s.reportantes,
      seguidoresIds: s.seguidores || [],
      fechaCreacion: s.creada,
      ...(s.resuelta ? { fechaResuelta: s.resuelta } : {}),
      ultimaVerificacionNotificada: s.verificada || s.creada,
      severidadEstimada: s.sev,
      ocultoDelMapa: !!s.oculto,
      descripcion: s.desc,
    });

    // Un reporte por cada reportante
    s.reportantes.forEach((uid, i) => {
      bw.set(db.collection("reportes").doc(), {
        incidenciaId: s.id,
        usuarioId: uid,
        usuarioNombre: nombreDe(uid),
        fotoUrl: foto,
        descripcion: i === 0 ? s.desc : "Yo también vi este problema.",
        fecha: s.creada + i * 600e3,
      });
    });

    for (const [uid, texto, fecha] of s.comentarios || []) {
      bw.set(db.collection("comentarios").doc(), {
        incidenciaId: s.id, usuarioId: uid, usuarioNombre: nombreDe(uid),
        usuarioFotoUrl: null, texto, fecha, utilCount: 0,
      });
    }

    // IDs de marca/denuncia como en el repositorio: "<usuarioId>_<incidenciaId>"
    usuarios.slice(0, marcas).forEach((u, i) => {
      bw.set(db.collection("marcasResuelto").doc(`${u.id}_${s.id}`), {
        usuarioId: u.id, contenidoId: s.id, fecha: horas(2 + i),
      });
    });
    usuarios.slice(0, denuncias).forEach((u, i) => {
      bw.set(db.collection("denuncias").doc(`${u.id}_${s.id}`), {
        usuarioId: u.id, contenidoId: s.id,
        motivo: i % 2 ? "NO_ES_INCIDENCIA" : "INAPROPIADO", detalles: null, fecha: horas(3 + i),
      });
    });
  }

  await bw.close();
  console.log(`Listo: ${incidencias.length} incidencias de prueba.`);
}

async function limpiar() {
  const bw = db.bulkWriter();
  const porPrefijo = async (col, campo) => {
    const snap = await db.collection(col).where(campo, ">=", "demo-").where(campo, "<", "demo-\uf8ff").get();
    snap.forEach((d) => bw.delete(d.ref));
  };
  await porPrefijo("incidencias", FieldPath.documentId());
  await porPrefijo("usuarios", FieldPath.documentId());
  await porPrefijo("reportes", "incidenciaId");
  await porPrefijo("comentarios", "incidenciaId");
  await porPrefijo("marcasResuelto", "contenidoId");
  await porPrefijo("denuncias", "contenidoId");
  await bw.close();
  if (process.env.STORAGE_BUCKET) {
    await getStorage().bucket().deleteFiles({ prefix: "incidencias/demo-" });
  }
  console.log("Datos demo- eliminados.");
}

(process.argv.includes("--clean") ? limpiar() : seed()).catch((e) => {
  console.error(e);
  process.exit(1);
});
