# CiudadAlerta

**CiudadAlerta** es una plataforma colaborativa para Android (estilo Waze cívico) orientada a que los ciudadanos reporten, confirmen y hagan seguimiento en tiempo real de **incidencias urbanas no urgentes** en espacio público (huecos en vías, fallas en alumbrado público, basuras acumuladas, semáforos dañados, entre otros).

> **Aviso de Seguridad:** CiudadAlerta **no** es un canal de atención de emergencias vitales. Para situaciones de riesgo inmediato, contacta a la línea oficial de emergencias (en Colombia, línea **123**).

---

## Servicios Integrados (8 Servicios del Sistema)

CiudadAlerta integra los siguientes **8 servicios principales de Cloud & AI**:

1. **Firebase Authentication (Google Sign-In):** Autenticación única mediante credenciales nativas de Google Credential Manager (sin acceso anónimo ni correos/contraseñas).
2. **Cloud Firestore:** Base de datos NoSQL en tiempo real para almacenar el catálogo de incidencias, comentarios, marcas de resuelto, alertas y perfiles ciudadanos.
3. **Cloud Storage:** Almacenamiento seguro de imágenes adjuntas en los reportes e incidencias (máximo 5 MB por imagen).
4. **Firebase Cloud Messaging (FCM):** Notificaciones Push en segundo plano (*data-only*) para alertar a los seguidores sobre cambios de estado o solicitudes de confirmación.
5. **Firebase App Check:** Protección de infraestructura y backend validando solicitudes legítimas mediante **Play Integrity** en producción y **Debug Provider** en desarrollo.
6. **Google Gemini API (Google AI Studio):** Análisis multimodal con el modelo `gemini-3.5-flash-lite` para categorizar fotos, verificar espacio público, detectar riesgos vitales y filtrar violaciones de privacidad.
7. **Google Maps SDK for Android:** Renderizado del mapa interactivo en tiempo real con marcadores personalizados, ubicación del usuario y agrupación de incidencias cercanas.
8. **Firebase Cloud Functions (v2):** Lógica de servidor serverless en Node.js/TypeScript (`notificarSeguidores` y tarea cron `verificar48h` para re-verificación a las 48 horas).

---

## Estructura de Capas y Carpetas

El proyecto sigue una arquitectura **Clean Architecture / MVVM** con Jetpack Compose y estado reactivo basado en `StateFlow` y `Coroutines`:

```text
CiudadAlerta/
├── app/src/main/java/com/ciudadalerta/
│   ├── data/                           # Capa de Datos y Modelos
│   │   ├── ai/                         # Servicio de integración con Gemini API (GeminiAnalysisService)
│   │   ├── config/                     # Parámetros comunitarios globales (CiudadAlertaConfig)
│   │   ├── fcm/                        # Servicio de notificaciones Push (CiudadAlertaMessagingService)
│   │   ├── model/                      # Clases de datos (Incidencia, Usuario, Alerta, Reporte, etc.)
│   │   ├── repository/                 # Repositorios (Auth, Incidencias, Notificaciones)
│   │   └── util/                       # GeoUtils (geohash, distancias), LocationUtils, TimeUtils
│   ├── ui/                             # Capa de Interfaz de Usuario (Jetpack Compose + Material 3)
│   │   ├── auth/                       # LoginScreen & AuthViewModel
│   │   ├── detail/                     # IncidentDetailScreen & DenounceBottomSheet
│   │   ├── map/                        # MapScreen, NearbyListView & MapViewModel
│   │   ├── myreports/                  # MyReportsScreen & MyReportsViewModel
│   │   ├── navigation/                 # NavGraph & Rutas de navegación
│   │   ├── notifications/              # NotificationsScreen & NotificationsViewModel
│   │   ├── profile/                    # ProfileScreen & gestión de sesión
│   │   ├── report/                     # CreateReport, GeminiAnalysis, DuplicateDetection, ReportConfirmation
│   │   └── theme/                      # Tema Material 3, colores y tipografía
│   └── MainActivity.kt                 # Punto de entrada de la app Android
├── functions/                          # Firebase Cloud Functions (TypeScript v2)
│   ├── src/index.ts                    # Triggers: notificarSeguidores y verificar48h
│   └── package.json
├── seed/                               # Script de poblamiento de datos de prueba
│   ├── seed.js                         # Carga masiva en Firestore y Storage
│   └── fotos/                          # Imágenes de prueba
├── docs/
│   └── ARQUITECTURA.md                 # Documentación detallada de arquitectura
└── README.md                           # Guía principal del proyecto
```

---

## Flujo Completo de Creación de un Reporte

```text
 [ 1. Captura de Foto ]
        │
        ▼
 [ 2. Análisis con Gemini IA ] ──(¿Riesgo Vital / Privacidad?)──► [ Bloqueado con alerta 123 ]
        │
        ▼ (Válido)
 [ 3. Detección de Duplicados ] ──(¿Existe reporte cercano?)───► [ Unirse al reporte existente ]
        │
        ▼ (No existe duplicado)
 [ 4. Confirmación y Publicación en Mapa ] ──► [ Notificación a la Comunidad ]
```

1. **Captura / Selección de Foto:**
   - Toma con la cámara: la ubicación GPS se obtiene **automáticamente**.
   - Selección desde galería: el usuario selecciona la ubicación **manualmente** mediante un selector de mapa.
2. **Análisis con Gemini IA (`GeminiAnalysisService`):**
   - La imagen se envía a Gemini (`gemini-3.5-flash-lite`).
   - Retorna la categoría sugerida, descripción concisa (máx. 2 frases) y verifica privacidad (rostros, placas) o riesgo vital (incendios, cables sueltos). Si detecta riesgo vital, bloquea la app y sugiere llamar al **123**.
3. **Detección de Duplicados Cercanos:**
   - Calcula incidencias de la misma categoría en un radio predeterminado (`RADIO_AGRUPACION_DEFAULT_METROS = 40m`).
   - Permite al usuario **unirse a un reporte existente** (incrementando contadores) o publicar una **nueva incidencia**.
4. **Publicación y Confirmación Comunitaria:**
   - La incidencia se guarda en Firestore (`incidencias/`) con su correspondiente `geohash`.
   - Se muestra en el mapa y queda disponible para confirmación ("Sigue ahí"), resolución comunitaria ($N$ marcas) o denuncias ($M$ denuncias para ocultación).

---

## Configuración del Proyecto

### 1. Requisitos Previos
- Android Studio con JDK 17+.
- Proyecto creado en la [Consola de Firebase](https://console.firebase.google.com/).

### 2. Archivo `google-services.json`
1. Registra el `applicationId` de tu app (`com.ciudadalerta`) en la consola de Firebase.
2. Genera y registra las huellas digitales SHA-1 y SHA-256 ejecutando:
   ```bash
   ./gradlew signingReport
   ```
3. Descarga el archivo `google-services.json` y colócalo en la carpeta `app/google-services.json`.

### 3. Claves de API en `local.properties`
Crea o edita el archivo `local.properties` en la raíz de tu proyecto e incluye las claves necesarias (**nunca las subas a Git**):

```properties
GEMINI_API_KEY=tu_clave_de_google_ai_studio
MAPS_API_KEY=tu_clave_de_google_maps_sdk
```

- **Gemini API Key:** Obtén una clave gratuita en [Google AI Studio](https://aistudio.google.com/).
- **Maps API Key:** Habilita *Maps SDK for Android* en [Google Cloud Console](https://console.cloud.google.com/) para el mismo proyecto de Firebase.

### 4. Configuración en Firebase Console
- **Authentication:** Habilita el proveedor **Google** en Sign-in method.
- **Firestore Database:** Crea la base de datos en modo producción e introduce las reglas de seguridad:
  ```json
   rules_version = '2';
   service cloud.firestore {
   match /databases/{database}/documents {
    // Auxiliar: verifica si la petición proviene de un usuario autenticado
    function isAuthenticated() {
      return request.auth != null;
    }

    // Auxiliar: verifica si el usuario autenticado es el propietario del recurso
    function isOwner(uid) {
      return isAuthenticated() && request.auth.uid == uid;
    }

    // Colección: incidencias
    match /incidencias/{incidenciaId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() &&
        request.auth.uid in request.resource.data.reportantesIds;

      // Permite actualizaciones únicamente sobre los campos modificados por las funciones del cliente
      allow update: if isAuthenticated() &&
        request.resource.data.diff(resource.data).affectedKeys().hasOnly([
          'contadorReportes',
          'reportantesIds',
          'marcasResuelto',
          'estado',
          'fechaResuelta',
          'contadorConfirmaciones',
          'ultimaVerificacionNotificada',
          'denunciasCount',
          'ocultoDelMapa',
          'seguidoresIds'
        ]);

      allow delete: if false;
    }

    // Colección: reportes
    match /reportes/{reporteId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && request.resource.data.usuarioId == request.auth.uid;
      allow update, delete: if false;
    }

    // Colección: comentarios
    match /comentarios/{comentarioId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && request.resource.data.usuarioId == request.auth.uid;
      // Marcar "útil": solo puede cambiar utilCount
      allow update: if isAuthenticated() &&
        request.resource.data.diff(resource.data).affectedKeys().hasOnly(['utilCount']);
      // Borrar: solo el autor (quítalo si no se usa)
      allow delete: if isAuthenticated() && resource.data.usuarioId == request.auth.uid;
    }

    // Colección: marcasResuelto (ID formato: {usuarioId}_{contenidoId})
    match /marcasResuelto/{docId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && docId == request.auth.uid + "_" + request.resource.data.contenidoId;
      allow update, delete: if false;
    }

    // Colección: denuncias (ID formato: {usuarioId}_{contenidoId})
    match /denuncias/{docId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && docId == request.auth.uid + "_" + request.resource.data.contenidoId;
      allow update, delete: if false;
    }

    // Colección: usuarios y subcolección de notificaciones
    match /usuarios/{uid} {
      allow read, write: if isOwner(uid);

      match /notificaciones/{notificacionId} {
        allow read: if isOwner(uid);
        // El cliente solo puede modificar el campo "leida"; nadie del cliente crea ni elimina
        allow update: if isOwner(uid) &&
          request.resource.data.diff(resource.data).affectedKeys().hasOnly(['leida']);
        allow create, delete: if false;
      }
    }
}
}

  ```
- **App Check:** Registra el token de depuración impreso en Logcat (`DebugAppCheckProvider`) para probar en emuladores o dispositivos en modo debug.

---

## Cloud Functions y Poblamiento de Datos (Seed)

### 1. Ejecución y Despliegue de Cloud Functions

Las funciones en segundo plano se encuentran en la carpeta `functions/`.

```bash
cd functions
npm install
npm run build
firebase deploy --only functions
```

- `notificarSeguidores`: Trigger cuando cambia un documento en `incidencias/{id}`. Envía alertas FCM a los seguidores.
- `verificar48h`: Tarea cron (cada 6 horas) que evalúa si una incidencia activa superó las 48 horas sin actualización y solicita re-verificación.

### 2. Script de Poblamiento (Seed Script)

Para cargar datos de prueba reales en el mapa y la base de datos:

```bash
cd seed
npm install
```

Coloca el archivo de credencial administrativa de Firebase `serviceAccount.json` en la carpeta `seed/` (descárgalo desde *Firebase Console -> Configuración de proyecto -> Cuentas de servicio -> Generar nueva clave privada*).

Ejecuta el script configurando las variables de entorno:

```bash
# Cargar incidencias, comentarios y reportes de prueba:
MI_UID="tu_uid_de_firebase_auth" STORAGE_BUCKET="tu-proyecto.firebasestorage.app" node seed.js

# Para limpiar todos los datos de prueba creados (prefijo "demo-"):
node seed.js --clean
```

---

## Limitaciones Conocidas

1. **Contadores actualizados desde el cliente:**
   - Los incrementos de `contadorReportes`, `marcasResuelto` y `denunciasCount` se gestionan mediante transacciones y batches directamente desde la aplicación Android. En un entorno de producción a gran escala, es recomendable delegar estas validaciones a Cloud Functions o reglas de Firestore avanzadas para evitar manipulación por clientes modificados.
2. **Notificaciones por seguimiento y no por zona:**
   - Las alertas y notificaciones Push FCM se envían exclusivamente a los usuarios registrados en los arreglos `reportantesIds` o `seguidoresIds` de cada incidencia. Actualmente no se calculan notificaciones en tiempo real por geofencing / ubicación dinámica del usuario.
3. **Sistema de puntos y niveles no implementado:**
   - Aunque el modelo de datos `Usuario` define las propiedades `puntosCivicos` y `nivel`, la lógica de gamificación (acumulación automática de puntos por reportar o confirmar incidencias y subir de nivel) aún no está activamente integrada en la experiencia de usuario.
