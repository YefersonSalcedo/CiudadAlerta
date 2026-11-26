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
 *   $env:MI_ZONA="Tarazá"           # (opcional) municipio donde quedan tus incidencias "mías"
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
const DIAS_VISIBLE = 4; // días visibles de una resuelta (DIAS_VISIBILIDAD_RESUELTA)
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
  { key: "arboles", nombre: "Árboles", sev: ["Baja", "Media"],
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

// Solo casco urbano. Cada zona tiene "sectores" (centro + radio en grados; 0.001° ≈ 110 m).
// "peso" reparte las incidencias entre municipios y sectores (NO es simétrico a propósito).
// "sesgo" multiplica el peso de cada categoría en ese municipio (1 = normal, >1 más frecuente, <1 menos).
// Coordenadas aproximadas: verifícalas con `node seed.js --dry --ids` (imprime un enlace a Google Maps por incidencia).
const ZONAS = [
  { ciudad: "Tarazá", depto: "Antioquia", peso: 0.34,
    sesgo: { vias: 1.9, animales: 1.6, residuos: 1.2, arboles: 0.7, semaforos: 0.4 },
    sectores: [
      { nombre: "Centro", lat: 7.5881, lng: -75.4028, r: 0.0040, peso: 3 },
      { nombre: "Sur", lat: 7.5778, lng: -75.4044, r: 0.0035, peso: 2 },
    ] },
  { ciudad: "El Retiro", depto: "Antioquia", peso: 0.24,
    sesgo: { parques: 2.0, arboles: 2.0, animales: 1.2, residuos: 0.7, semaforos: 0.3 },
    sectores: [
      { nombre: "Centro", lat: 6.0600, lng: -75.5035, r: 0.0035, peso: 1 },
    ] },
  { ciudad: "Medellín", depto: "Antioquia", peso: 0.42,
    sesgo: { semaforos: 2.0, alumbrado: 1.3, residuos: 1.3, otros: 1.4, animales: 0.5 },
    sectores: [
      { nombre: "Laureles", lat: 6.2447, lng: -75.5905, r: 0.0035, peso: 3 },
      { nombre: "Centro", lat: 6.2518, lng: -75.5636, r: 0.0030, peso: 3 },
      { nombre: "Belén", lat: 6.2336, lng: -75.6003, r: 0.0035, peso: 2 },
      { nombre: "El Poblado", lat: 6.2086, lng: -75.5659, r: 0.0030, peso: 2 },
      { nombre: "Castilla", lat: 6.2915, lng: -75.5710, r: 0.0030, peso: 1 },
      { nombre: "Robledo", lat: 6.2745, lng: -75.5945, r: 0.0030, peso: 1 },
    ] },
];
const ZONA_MIA = process.env.MI_ZONA || "Tarazá"; // municipio donde viven las incidencias "mías" (Mis reportes / seguidas)
const TIPOS_VIA = ["Cra.", "Cl.", "Av.", "Dg."];
const direccion = (z) => `${pick(TIPOS_VIA)} ${int(10, 80)} # ${int(10, 70)}-${int(1, 99)}, ${z.ciudad}, ${z.depto}, Colombia`;

// Frecuencia base por categoría (asimétrica) y por escenario
const PESO_CAT = { vias: 26, residuos: 20, alumbrado: 15, semaforos: 9, parques: 8, arboles: 7, animales: 6, otros: 5 };
const pickW = (items, w) => {
  const tot = items.reduce((a, x) => a + w(x), 0);
  let r = rnd() * tot;
  for (const x of items) { r -= w(x); if (r < 0) return x; }
  return items[items.length - 1];
};

// Escenarios para cubrir todos los casos de la app (pesos: la mayoría son incidencias normales)
const PESO_ESC = { nueva: 9, normal: 38, destacada: 12, h48: 8, casiResuelta: 7, resuelta: 9, vencida: 4, casiOculta: 5, oculta: 4 };
const ESCENARIOS = Object.keys(PESO_ESC);

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
  const cuposZona = ZONAS.map((z) => Math.floor(TOTAL * z.peso));
  cuposZona[cuposZona.length - 1] += TOTAL - cuposZona.reduce((a, b) => a + b, 0); // el resto va al último municipio
  let idx = 0;

  ZONAS.forEach((z, zi) => {
    for (let k = 0; k < cuposZona[zi]; k++, idx++) {
      const cat = pickW(cats, (c) => (PESO_CAT[c.key] || 5) * ((z.sesgo || {})[c.key] || 1));
      const esc = pickW(ESCENARIOS, (e) => PESO_ESC[e]);
      const id = `demo-${String(idx + 1).padStart(3, "0")}-${cat.key}`;
      const sec = pickW(z.sectores, (x) => x.peso);
      const ang = rnd() * 2 * Math.PI, rad = sec.r * Math.sqrt(rnd());
      const lat = sec.lat + rad * Math.sin(ang);
      const lng = sec.lng + (rad * Math.cos(ang)) / Math.cos((sec.lat * Math.PI) / 180);
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

      // En el municipio ZONA_MIA (por defecto Tarazá), parte de las incidencias son "mías" para probar Mis reportes
      const esMiZona = z.ciudad === ZONA_MIA;
      const mia = ME && esMiZona && k % 4 === 0;
      const otros = shuffle(usuarios.map((u) => u.id));
      const reportantes = mia ? [ME, ...otros.slice(0, nReport - 1)] : otros.slice(0, nReport);
      const seguidores = [];
      if (ME && !mia && esMiZona && (esc === "destacada" || idx % 5 === 1)) seguidores.push(ME);
      if (rnd() < 0.3) seguidores.push(otros[nReport % otros.length]);

      const iTexto = int(0, cat.titulos.length - 1);
      const dir = direccion(z);
      const verificada = esc === "h48" ? creada : Math.max(creada, horas(int(1, 20)));
      const seguida = !!(ME && seguidores.includes(ME));
      incidencias.push({
        id, fotoLocal, meta: { esc, cat: cat.key, ciudad: z.ciudad, sector: sec.nombre, lat, lng, mia: !!mia, seguida, nRep: reportantes.length }, data: {
          titulo: cat.titulos[iTexto],
          categoria: cat.nombre,
          ciudad: z.ciudad,
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
  // (se revisan ambos nombres porque README usa "notificaciones" y ARQUITECTURA "alertas")
  let nAlertas = 0;
  for (const sub of ["alertas", "notificaciones"]) {
    const snap = await db.collectionGroup(sub).get();
    snap.forEach((d) => bw.delete(d.ref));
    nAlertas += snap.size;
  }
  await bw.close();
  console.log(`  ${nAlertas} alertas eliminadas`);
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
    plan.incidencias.forEach((i) => { const e = i.meta.esc; porEsc[e] = (porEsc[e] || 0) + 1; });
    console.log(`Incidencias: ${plan.incidencias.length} | reportes: ${plan.reportes.length} | comentarios: ${plan.comentarios.length} | marcas: ${plan.marcas.length} | denuncias: ${plan.denuncias.length}`);
    console.log("Escenarios:", porEsc);
    const tabla = {};
    plan.incidencias.forEach((i) => { tabla[i.meta.ciudad] = tabla[i.meta.ciudad] || { total: 0 }; tabla[i.meta.ciudad][i.meta.cat] = (tabla[i.meta.ciudad][i.meta.cat] || 0) + 1; tabla[i.meta.ciudad].total++; });
    console.log("Por municipio y categoría:"); console.table(tabla);
    if (flag("ids")) {
      console.log("\nID | zona | sector | escenario | reportes | mía | la sigues | mapa");
      plan.incidencias.forEach((i) => console.log(`${i.id} | ${i.meta.ciudad} | ${i.meta.sector} | ${i.meta.esc} | ${i.meta.nRep} | ${i.meta.mia ? "sí" : "-"} | ${i.meta.seguida ? "sí" : "-"} | https://www.google.com/maps?q=${i.meta.lat.toFixed(6)},${i.meta.lng.toFixed(6)}`));
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
