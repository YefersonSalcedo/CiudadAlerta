/**
 * Seed v2 de CiudadAlerta: muchos datos de prueba con fotos reales.
 *
 * Preparación (una vez):
 *   cd seed
 *   npm init -y
 *   npm install firebase-admin ngeohash
 *   (poner serviceAccount.json aquí; NO subirlo a Git)
 *
 * Fotos: crea carpetas con imágenes reales (.jpg/.jpeg/.png/.webp), ideal 1280 px y < 400 KB cada una:
 *   fotos/vias/  fotos/alumbrado/  fotos/residuos/  fotos/parques/
 *   fotos/semaforos/  fotos/arboles/  fotos/animales/  fotos/otros/
 *   Con 5 a 10 fotos por carpeta alcanza: se reutilizan.
 *
 * Variables de entorno (PowerShell):
 *   $env:MI_UID="..."; $env:MI_NOMBRE="Yeferson"; $env:STORAGE_BUCKET="tu-proyecto.firebasestorage.app"
 *
 * Uso:
 *   node seed.js --dry            # no escribe nada: muestra qué generaría (añade --ids para listar cada incidencia)
 *   --omitir-sin-fotos            # no genera incidencias de categorías cuya carpeta de fotos esté vacía
 *   node seed.js                  # carga 60 incidencias (cambia con --n=100)
 *   node seed.js --clean          # borra solo lo que empiece por "demo-"
 *   node seed.js --clean-all --yes   # BORRA TODAS las incidencias, reportes, comentarios, marcas,
 *                                    # denuncias, alertas y fotos de incidencias (incluye datos reales)
 */
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const ngeohash = require("ngeohash");

const args = process.argv.slice(2);
const flag = (n) => args.includes(`--${n}`);
const arg = (n, d) => {
  const a = args.find((x) => x.startsWith(`--${n}=`));
  return a ? a.split("=")[1] : d;
};

// ---- Ajusta a CiudadAlertaConfig.kt ----
const N = 3;            // marcas para resolver
const M = 3;            // denuncias para ocultar
const X_HORAS = 48;     // re-verificación
const DIAS_VISIBLE = 3; // días visibles de una resuelta
const MIN_DESTACADA = 3;
// ----------------------------------------

const ME = process.env.MI_UID || null;
const ME_NOMBRE = process.env.MI_NOMBRE || "Yo";
const TOTAL = parseInt(arg("n", "60"), 10);

// PRNG determinista: mismos datos en cada ejecución
let _s = 20261124;
const rnd = () => {
  _s |= 0; _s = (_s + 0x6d2b79f5) | 0;
  let t = Math.imul(_s ^ (_s >>> 15), 1 | _s);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};
const int = (a, b) => a + Math.floor(rnd() * (b - a + 1));
const pick = (arr) => arr[Math.floor(rnd() * arr.length)];
const shuffle = (arr) => arr.map((v) => [rnd(), v]).sort((x, y) => x[0] - y[0]).map((x) => x[1]);

const ahora = Date.now();
const horas = (x) => Math.round(ahora - x * 3600e3);
const dias = (x) => Math.round(ahora - x * 86400e3);

const usuarios = [
  ["Laura Gómez", 40, 2], ["Carlos Ruiz", 15, 1], ["Marta Díaz", 90, 3], ["Andrés Mora", 5, 1],
  ["Sofía Herrera", 22, 1], ["Julián Castaño", 60, 2], ["Valentina Ríos", 12, 1], ["Camilo Arango", 33, 2],
].map(([nombre, puntosCivicos, nivel], i) => ({
  id: `demo-user-${i + 1}`, nombre, puntosCivicos, nivel,
  correo: `${nombre.split(" ")[0].toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "")}.demo@example.com`,
}));
const nombreDe = (uid) => (uid === ME ? ME_NOMBRE : (usuarios.find((u) => u.id === uid) || {}).nombre || "Ciudadano");

// Categorías: carpeta de fotos, nombre EXACTO de Categoria.kt, textos de ejemplo
const CATEGORIAS = [
  { key: "vias", nombre: "Vías", sev: ["Alta", "Media"],
    titulos: ["Hueco grande en la vía", "Bache lleno de agua", "Pavimento levantado", "Vía sin pavimentar en mal estado"],
    descs: ["Hueco de unos 60 cm que daña las llantas de las motos.", "Bache profundo que se llena de agua cuando llueve.", "El pavimento está agrietado y se desprende en pedazos.", "La vía se vuelve un lodazal y los carros se quedan atascados."] },
  { key: "alumbrado", nombre: "Alumbrado público", sev: ["Media", "Baja"],
    titulos: ["Poste de alumbrado sin luz", "Calle oscura por varias luminarias dañadas", "Luminaria parpadeando", "Poste inclinado con cables expuestos"],
    descs: ["La luminaria lleva más de una semana apagada.", "Tres postes seguidos sin luz; la cuadra queda muy oscura en la noche.", "La lámpara prende y apaga todo el tiempo.", "El poste está inclinado y los cables cuelgan a poca altura."] },
  { key: "residuos", nombre: "Residuos y aseo", sev: ["Alta", "Media"],
    titulos: ["Acumulación de basura en la esquina", "Escombros abandonados en el andén", "Bolsas rotas por animales", "Punto crítico de basuras"],
    descs: ["Bolsas de basura acumuladas desde hace días.", "Dejaron escombros de una obra ocupando el andén.", "Los perros rompen las bolsas y la basura queda regada.", "El camión no pasa y la gente deja la basura en el lote."] },
  { key: "parques", nombre: "Parques y zonas verdes", sev: ["Baja", "Media"],
    titulos: ["Juegos infantiles dañados", "Zona verde sin mantenimiento", "Banca rota en el parque", "Maleza alta en el separador"],
    descs: ["El columpio está roto y es peligroso para los niños.", "El pasto está muy alto y se acumula basura.", "Las bancas del parque están rotas.", "La maleza tapa la visibilidad en la esquina."] },
  { key: "semaforos", nombre: "Semáforos", sev: ["Media", "Alta"],
    titulos: ["Semáforo intermitente", "Semáforo apagado", "Señal de pare caída", "Semáforo peatonal sin funcionar"],
    descs: ["El semáforo parpadea en amarillo todo el día.", "No enciende ninguna luz en la intersección.", "La señal de pare está en el suelo desde la tormenta.", "El botón peatonal no responde y los carros no se detienen."] },
  { key: "arboles", nombre: "Árboles (sin riesgo inminente)", sev: ["Baja", "Media"],
    titulos: ["Árbol con ramas sobre el andén", "Ramas tapando el alumbrado", "Raíces que levantan el andén", "Árbol seco en el parque"],
    descs: ["Las ramas bajas estorban a los peatones.", "Las ramas cubren la luminaria y la calle queda oscura.", "Las raíces levantaron el andén y es difícil pasar.", "El árbol está seco y conviene podarlo."] },
  { key: "animales", nombre: "Animales abandonados", sev: ["Alta", "Media"],
    titulos: ["Perros abandonados junto a la escuela", "Gato herido en la esquina", "Animales deambulando en el parque", "Caballo suelto en la vía"],
    descs: ["Varios perros flacos se pelean por comida; se necesita ayuda veterinaria.", "Hay un gato con una pata lastimada que no se deja acercar.", "Los animales rompen bolsas y asustan a los niños.", "Un caballo suelto camina por la vía y puede causar un accidente."] },
  { key: "otros", nombre: "Otros", sev: ["Baja", "Media"],
    titulos: ["Andén levantado", "Caño tapado", "Alcantarilla sin tapa", "Poste de publicidad caído"],
    descs: ["El andén está levantado y es difícil pasar con coches de bebé.", "El caño está tapado y el agua se desborda al llover.", "La alcantarilla no tiene tapa y es un riesgo para los peatones.", "Un aviso quedó caído y bloquea parte del andén."] },
];
const COMENT = ["Sigue igual esta mañana.", "Pasé por aquí ayer y continúa el problema.", "Ya hay varios vecinos reportándolo.", "Ojalá lo arreglen pronto, es peligroso.", "Confirmo, sigue ahí.", "Hoy empeoró con la lluvia.", "Llevo semanas viéndolo así.", "Gracias por reportarlo."];
const REPORTE_TXT = ["Yo también vi este problema.", "Sigue sin arreglarse.", "Confirmo el reporte, es peligroso.", "Lo vi esta mañana, continúa igual."];

const ZONAS = [
  { ciudad: "Tarazá", depto: "Antioquia", lat: 7.5778, lng: -75.4044, radio: 0.011, peso: 0.5 },
  { ciudad: "Medellín", depto: "Antioquia", lat: 6.2442, lng: -75.5812, radio: 0.035, peso: 0.33 },
  { ciudad: "Caucasia", depto: "Antioquia", lat: 7.9847, lng: -75.1981, radio: 0.013, peso: 0.17 },
];
const TIPOS_VIA = ["Cra.", "Cl.", "Av.", "Dg."];
const direccion = (z) => `${pick(TIPOS_VIA)} ${int(10, 80)} # ${int(10, 70)}-${int(1, 99)}, ${z.ciudad}, ${z.depto}, Colombia`;

// Escenarios para cubrir todos los casos de la app
const ESCENARIOS = ["nueva", "normal", "destacada", "normal", "h48", "casiResuelta", "resuelta", "destacada", "casiOculta", "normal", "vencida", "oculta"];

function fotosDe(key) {
  const dir = path.join(__dirname, "fotos", key);
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir).filter((f) => /\.(jpe?g|png|webp)$/i.test(f)).sort().map((f) => path.join(dir, f));
}

function generar() {
  const incidencias = [], reportes = [], comentarios = [], marcas = [], denuncias = [];
  const pool = Object.fromEntries(CATEGORIAS.map((c) => [c.key, fotosDe(c.key)]));
  // --omitir-sin-fotos: no genera incidencias de las categorías cuya carpeta de fotos está vacía
  const cats = flag("omitir-sin-fotos") ? CATEGORIAS.filter((c) => pool[c.key].length > 0) : CATEGORIAS;
  if (!cats.length) throw new Error("No hay fotos en ninguna carpeta de seed/fotos/");
  const cuposZona = ZONAS.map((z) => Math.round(TOTAL * z.peso));
  let idx = 0;

  ZONAS.forEach((z, zi) => {
    for (let k = 0; k < cuposZona[zi]; k++, idx++) {
      const cat = cats[(idx + Math.floor(idx / ESCENARIOS.length)) % cats.length];
      const esc = ESCENARIOS[idx % ESCENARIOS.length];
      const id = `demo-${String(idx + 1).padStart(3, "0")}-${cat.key}`;
      const lat = z.lat + (rnd() - 0.5) * 2 * z.radio;
      const lng = z.lng + (rnd() - 0.5) * 2 * z.radio;
      const fotos = pool[cat.key];
      const fotoLocal = fotos.length ? fotos[(idx * 7 + k) % fotos.length] : null;

      let creada = horas(int(2, 24 * 10));
      let estado = "ACTIVA", resuelta = null, nReport = int(1, 2), nMarcas = 0, nDen = 0, oculto = false, nConf = int(0, 3);
      if (esc === "nueva") creada = horas(int(1, 6));
      if (esc === "destacada") { nReport = int(MIN_DESTACADA, 5); nConf = int(2, 5); creada = horas(int(5, 70)); }
      if (esc === "h48") creada = horas(X_HORAS + int(6, 40));
      if (esc === "casiResuelta") { nMarcas = N - 1; creada = dias(int(3, 8)); }
      if (esc === "resuelta") { estado = "RESUELTA"; nMarcas = N; creada = dias(int(3, 6)); resuelta = horas(int(2, DIAS_VISIBLE * 24 - 6)); }
      if (esc === "vencida") { estado = "RESUELTA"; nMarcas = N; creada = dias(20); resuelta = dias(DIAS_VISIBLE + int(3, 10)); }
      if (esc === "casiOculta") { nDen = M - 1; creada = horas(int(10, 60)); }
      if (esc === "oculta") { nDen = M; oculto = true; creada = horas(int(10, 80)); }

      // En Tarazá, parte de las incidencias son "mías" para probar Mis reportes
      const mia = ME && zi === 0 && idx % 4 === 0;
      const otros = shuffle(usuarios.map((u) => u.id));
      const reportantes = mia ? [ME, ...otros.slice(0, nReport - 1)] : otros.slice(0, nReport);
      const seguidores = [];
      if (ME && !mia && zi === 0 && (esc === "destacada" || idx % 5 === 1)) seguidores.push(ME);
      if (rnd() < 0.3) seguidores.push(otros[nReport % otros.length]);

      const iTexto = int(0, cat.titulos.length - 1);
      const dir = direccion(z);
      const verificada = esc === "h48" ? creada : Math.max(creada, horas(int(1, 20)));
      const seguida = !!(ME && seguidores.includes(ME));
      incidencias.push({
        id, fotoLocal, meta: { esc, ciudad: z.ciudad, mia: !!mia, seguida, nRep: reportantes.length }, data: {
          titulo: cat.titulos[iTexto],
          categoria: cat.nombre,
          ubicacion: { latitud: lat, longitud: lng, direccionAproximada: dir, geohash: ngeohash.encode(lat, lng, 9) },
          contadorReportes: reportantes.length,
          contadorConfirmaciones: nConf,
          estado,
          fotoPrincipalUrl: "",
          marcasResuelto: nMarcas,
          denunciasCount: nDen,
          reportantesIds: reportantes,
          seguidoresIds: [...new Set(seguidores)],
          fechaCreacion: creada,
          ...(resuelta ? { fechaResuelta: resuelta } : {}),
          ultimaVerificacionNotificada: esc === "h48" ? creada : verificada,
          severidadEstimada: pick(cat.sev),
          ocultoDelMapa: oculto,
          descripcion: cat.descs[iTexto],
        },
      });

      reportantes.forEach((uid, i) => {
        reportes.push({
          incidenciaId: id, usuarioId: uid, usuarioNombre: nombreDe(uid),
          fotoLocal: i === 0 ? fotoLocal : (fotos.length ? fotos[(idx * 7 + k + i * 3) % fotos.length] : null),
          descripcion: i === 0 ? cat.descs[iTexto] : pick(REPORTE_TXT),
          fecha: creada + i * 3600e3 * int(1, 5),
        });
      });
      const nCom = esc === "destacada" ? int(2, 4) : (rnd() < 0.4 ? int(1, 2) : 0);
      for (let c = 0; c < nCom; c++) {
        const uid = pick(usuarios).id;
        comentarios.push({ incidenciaId: id, usuarioId: uid, usuarioNombre: nombreDe(uid), usuarioFotoUrl: null,
          texto: pick(COMENT), fecha: Math.min(ahora - 60e3, creada + (c + 1) * 3600e3 * int(1, 6)), utilCount: 0 });
      }
      usuarios.slice(0, nMarcas).forEach((u, i) => marcas.push({ docId: `${u.id}_${id}`, usuarioId: u.id, contenidoId: id, fecha: horas(2 + i) }));
      usuarios.slice(0, nDen).forEach((u, i) => denuncias.push({ docId: `${u.id}_${id}`, usuarioId: u.id, contenidoId: id,
        motivo: i % 2 ? "NO_ES_INCIDENCIA" : "INAPROPIADO", detalles: null, fecha: horas(3 + i) }));
    }
  });
  return { incidencias, reportes, comentarios, marcas, denuncias, pool };
}

// ---------------- Firebase ----------------
let db, bucket, FieldPath;
function iniciarFirebase() {
  const { initializeApp, cert } = require("firebase-admin/app");
  const { getFirestore, FieldPath: FP } = require("firebase-admin/firestore");
  const { getStorage } = require("firebase-admin/storage");
  initializeApp({ credential: cert(require("./serviceAccount.json")), storageBucket: process.env.STORAGE_BUCKET });
  db = getFirestore();
  FieldPath = FP;
  bucket = process.env.STORAGE_BUCKET ? getStorage().bucket() : null;
}

const cacheFotos = new Map();
async function subirFoto(local) {
  if (!local || !bucket) return "";
  if (cacheFotos.has(local)) return cacheFotos.get(local);
  const key = path.basename(path.dirname(local));
  const dest = `incidencias/demo-${key}-${path.basename(local).replace(/[^\w.\-]/g, "_")}`;
  const token = crypto.randomUUID();
  const ext = path.extname(local).toLowerCase();
  const contentType = ext === ".png" ? "image/png" : ext === ".webp" ? "image/webp" : "image/jpeg";
  await bucket.upload(local, { destination: dest, metadata: { contentType, metadata: { firebaseStorageDownloadTokens: token } } });
  const url = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(dest)}?alt=media&token=${token}`;
  cacheFotos.set(local, url);
  return url;
}

async function cargar(plan) {
  if (!bucket) console.warn("AVISO: sin STORAGE_BUCKET no se suben fotos; fotoPrincipalUrl quedará vacío.");
  const bw = db.bulkWriter();
  for (const u of usuarios) {
    const { id, ...d } = u;
    bw.set(db.collection("usuarios").doc(id), { ...d, fotoUrl: null, fcmToken: null });
  }
  let n = 0;
  for (const inc of plan.incidencias) {
    const url = await subirFoto(inc.fotoLocal);
    bw.set(db.collection("incidencias").doc(inc.id), { ...inc.data, fotoPrincipalUrl: url });
    if (++n % 10 === 0) console.log(`  ${n}/${plan.incidencias.length} incidencias...`);
  }
  for (const r of plan.reportes) {
    const { fotoLocal, ...d } = r;
    bw.set(db.collection("reportes").doc(), { ...d, fotoUrl: await subirFoto(fotoLocal) });
  }
  for (const c of plan.comentarios) bw.set(db.collection("comentarios").doc(), c);
  for (const m of plan.marcas) { const { docId, ...d } = m; bw.set(db.collection("marcasResuelto").doc(docId), d); }
  for (const d0 of plan.denuncias) { const { docId, ...d } = d0; bw.set(db.collection("denuncias").doc(docId), d); }
  await bw.close();
}

async function limpiarDemo() {
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
  if (bucket) await bucket.deleteFiles({ prefix: "incidencias/demo-" });
  console.log("Datos demo- eliminados.");
}

async function limpiarTodo() {
  for (const col of ["incidencias", "reportes", "comentarios", "marcasResuelto", "denuncias"]) {
    await db.recursiveDelete(db.collection(col));
    console.log(`  colección ${col} vaciada`);
  }
  // Alertas de todos los usuarios (los perfiles NO se borran, para no perder fcmToken ni sesiones)
  const bw = db.bulkWriter();
  const alertas = await db.collectionGroup("notificaciones").get();
  alertas.forEach((d) => bw.delete(d.ref));
  await bw.close();
  console.log(`  ${alertas.size} alertas eliminadas`);
  const demo = await db.collection("usuarios").where(FieldPath.documentId(), ">=", "demo-").where(FieldPath.documentId(), "<", "demo-\uf8ff").get();
  const bw2 = db.bulkWriter();
  demo.forEach((d) => bw2.delete(d.ref));
  await bw2.close();
  if (bucket) { await bucket.deleteFiles({ prefix: "incidencias/" }); console.log("  fotos de incidencias eliminadas"); }
  else console.warn("AVISO: sin STORAGE_BUCKET no se borraron las fotos de Storage.");
  console.log("Listo: se borró todo.");
}

(async () => {
  if (flag("dry")) {
    const plan = generar();
    const porEsc = {};
    plan.incidencias.forEach((i, n) => { const e = ESCENARIOS[n % ESCENARIOS.length]; porEsc[e] = (porEsc[e] || 0) + 1; });
    console.log(`Incidencias: ${plan.incidencias.length} | reportes: ${plan.reportes.length} | comentarios: ${plan.comentarios.length} | marcas: ${plan.marcas.length} | denuncias: ${plan.denuncias.length}`);
    console.log("Escenarios (aprox.):", porEsc);
    if (flag("ids")) {
      console.log("\nID | zona | escenario | reportes | mía | la sigues");
      plan.incidencias.forEach((i) => console.log(`${i.id} | ${i.meta.ciudad} | ${i.meta.esc} | ${i.meta.nRep} | ${i.meta.mia ? "sí" : "-"} | ${i.meta.seguida ? "sí" : "-"}`));
    }
    console.log("Fotos encontradas por categoría:", Object.fromEntries(Object.entries(plan.pool).map(([k, v]) => [k, v.length])));
    return;
  }
  iniciarFirebase();
  if (flag("clean-all")) {
    if (!flag("yes")) { console.error("Esto borra TODO (incluye datos reales). Repite con --yes para confirmar."); process.exit(1); }
    return limpiarTodo();
  }
  if (flag("clean")) return limpiarDemo();
  await cargar(generar());
  console.log(`Listo: ${TOTAL} incidencias de prueba con sus reportes, comentarios, marcas y denuncias.`);
})().catch((e) => { console.error(e); process.exit(1); });
