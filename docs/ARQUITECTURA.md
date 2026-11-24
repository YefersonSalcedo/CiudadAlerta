# Arquitectura Técnica - CiudadAlerta

Este documento especifica la arquitectura detallada, el diseño de componentes, las decisiones técnicas, el modelo de datos NoSQL y los flujos de ejecución de **CiudadAlerta**.

---

## Visión General de la Arquitectura

CiudadAlerta sigue los principios de **Clean Architecture** y el patrón de diseño **MVVM (Model-View-ViewModel)** en Android, con interfaz de usuario declarativa construida completamente en **Jetpack Compose (Material 3)** y gestión de estado reactivo mediante **Kotlin StateFlow** y **Coroutines**.

```text
┌─────────────────────────────────────────────────────────────┐
│                    Capa de UI (Compose)                      │
│   MapScreen  │ CreateReportScreen │ IncidentDetailScreen    │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow / Events
┌──────────────────────────────▼──────────────────────────────┐
│                  Capa de Presentación (ViewModels)          │
│   MapViewModel │ ReportViewModel │ IncidentDetailViewModel │
└──────────────────────────────┬──────────────────────────────┘
                               │ Suspend functions / Flow
┌──────────────────────────────▼──────────────────────────────┐
│                    Capa de Datos (Repositories)             │
│ AuthRepository │ IncidenciasRepository │ NotificacionesRepo │
└──────┬───────────────────────┬──────────────────────┬───────┘
       │                       │                      │
┌──────▼──────┐         ┌──────▼──────┐        ┌──────▼──────┐
│ Firebase    │         │  Cloud      │        │ Google      │
│ Auth / FCM  │         │ Firestore   │        │ Gemini API  │
└─────────────┘         └─────────────┘        └─────────────┘
```

---

## Componentes y Capas del Sistema

### 1. Capa de Configuración y Modelos (`com.ciudadalerta.data`)
- **`config/CiudadAlertaConfig.kt`**: Constantes globales del sistema de resolución comunitaria:
  - `HORAS_VERIFICACION_X = 48L` ($X$: horas tras las cuales se notifica a los reportantes para re-verificar si el problema sigue presente).
  - `CANTIDAD_MARCAS_RESOLVER_N = 3` ($N$: cantidad de marcas de "resuelto" requeridas para marcar la incidencia como resuelta).
  - `MAX_REPORTES_DIARIOS_USUARIO_M = 5` ($M$: límite de reportes por usuario por día).
  - `DIAS_VISIBILIDAD_RESUELTA = 4L` (días que permanece visible una incidencia resuelta en el mapa antes de ocultarse).
  - `DENUNCIAS_PARA_OCULTAR_M = 3` (cantidad de denuncias para ocultar automáticamente del mapa).
  - `RADIO_AGRUPACION_DEFAULT_METROS = 40.0` (radio en metros para la agrupación y detección de duplicados).
- **`model/IncidenciaModels.kt`**: Clases de datos del dominio (`Incidencia`, `Usuario`, `Reporte`, `Comentario`, `Alerta`, `Denuncia`, `MarcaResuelto`, `Ubicacion`).

### 2. Capa de Repositorios y Servicios (`com.ciudadalerta.data`)
- **`repository/IncidenciasRepository.kt`**: Encapsula las operaciones CRUD en Firestore para las incidencias, comentarios, marcas de resuelto y denuncias. Sube imágenes a Cloud Storage y calcula distancias geográficas.
- **`repository/NotificacionesRepository.kt`**: Administra las funciones para seguir/dejar de seguir incidencias y consulta el listado de alertas comunitarias del usuario en tiempo real.
- **`repository/AuthRepository.kt`**: Gestiona la autenticación con Google mediante `CredentialManager` y mantiene sincronizado el perfil del usuario en Firestore.
- **`ai/GeminiAnalysisService.kt`**: Integra la API de Gemini (`gemini-3.5-flash-lite`) enviando imágenes en Base64 para obtener la categoría, descripción estructurada y filtrado de privacidad/emergencias.
- **`fcm/CiudadAlertaMessagingService.kt`**: Maneja la recepción de tokens FCM (`onNewToken`), el almacenamiento local o sincronización con Firestore (`fcmToken`) y la presentación de notificaciones locales en primer plano.

### 3. Capa de Presentación e Interfaz (`com.ciudadalerta.ui`)
- **`map/`**: Mapa interactivo con Google Maps SDK, filtros por categoría y vista inferior expandible (`NearbyListView`).
- **`report/`**: Flujo asistido por IA para reporte (Tomar/seleccionar foto -> Análisis IA -> Detección de duplicados -> Confirmación).
- **`detail/`**: Detalle completo de incidencia, línea de tiempo de apoyos, sección de comentarios y opciones de resolver o denunciar.
- **`notifications/`**: Centro de notificaciones con estado de lectura e íconos dinámicos por tipo de alerta.
- **`auth/` & `profile/`**: Pantalla de login exclusivo con Google y perfil de usuario con estadísticas.

---

## Esquema de la Base de Datos NoSQL (Cloud Firestore)

```text
Firestore Root
├── incidencias/ {incidenciaId}
│   └── seguidores/ {userId}
├── usuarios/ {userId}
│   └── alertas/ {alertaId}
├── reportes/ {reporteId}
├── comentarios/ {comentarioId}
├── marcasResuelto/ {marcaId}
└── denuncias/ {denunciaId}
```

### Colección Principales:

#### 1. `incidencias/{incidenciaId}`
- `id`: String (`@DocumentId`)
- `titulo`: String
- `categoria`: String
- `ubicacion`: `{ latitud: Double, longitud: Double, direccionAproximada: String, geohash: String }`
- `contadorReportes`: Int
- `contadorConfirmaciones`: Int
- `estado`: String ("ACTIVA" | "RESUELTA" | "ARCHIVADA")
- `fotoPrincipalUrl`: String
- `marcasResuelto`: Int
- `denunciasCount`: Int
- `reportantesIds`: List<String>
- `seguidoresIds`: List<String>
- `fechaCreacion`: Long (ms)
- `fechaResuelta`: Long? (ms)
- `ultimaVerificacionNotificada`: Long (ms)
- `severidadEstimada`: String
- `ocultoDelMapa`: Boolean
- `descripcion`: String

#### 2. `usuarios/{userId}`
- `id`: String (`@DocumentId`)
- `nombre`: String
- `correo`: String
- `fotoUrl`: String?
- `puntosCivicos`: Int
- `nivel`: Int
- `fcmToken`: String?

#### 3. `usuarios/{userId}/alertas/{alertaId}`
- `id`: String (`@DocumentId`)
- `incidenciaId`: String
- `tipo`: String ("CAMBIO_ESTADO" | "COMENTARIO" | "REVERIFICACION" | "APOYO" | "DENUNCIA")
- `mensaje`: String
- `fecha`: Long (ms)
- `leida`: Boolean

---

## Cloud Functions (v2 Serverless)

Las funciones serverless residen en la carpeta `functions/src/index.ts`:

1. **`notificarSeguidores`**:
   - **Trigger:** Modificación en `incidencias/{id}` (`onDocumentUpdated`).
   - **Lógica:** Detecta si cambió el `estado` o aumentaron los reportes/confirmaciones. Notifica a los UIDs en `seguidoresIds` creando una alerta en `usuarios/{uid}/alertas` y despachando un mensaje Push FCM *data-only*.
2. **`verificar48h`**:
   - **Trigger:** Tarea programada (`onSchedule`, cada 6 horas).
   - **Lógica:** Consulta incidencias en estado `ACTIVA` cuya `ultimaVerificacionNotificada` tenga más de 48 horas ($X$). Envía una solicitud de re-verificación a los `reportantesIds` y actualiza el timestamp para prevenir notificaciones duplicadas.

---

## 🔐 Reglas de Seguridad y Protección de Datos

1. **Protección de Credenciales:** `local.properties` almacena las claves de Gemini (`GEMINI_API_KEY`) y Google Maps (`MAPS_API_KEY`), excluyéndolas del control de versiones.
2. **App Check Verification:** Firebase App Check garantiza que únicamente solicitudes firmadas por la aplicación legítima puedan escribir en Firestore y Storage.
3. **Firestore Security Rules:** Lectura abierta de la colección `incidencias` para permitir visualizar el mapa sin restricciones, exigiendo sesión activa de Firebase Auth para escrituras y restringiendo las modificaciones sobre `usuarios/{userId}` exclusivamente al propietario del perfil (`request.auth.uid == userId`).
