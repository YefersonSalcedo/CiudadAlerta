# CiudadAlerta - Firebase Cloud Functions (v2)

Módulo de funciones de servidor en segundo plano e integración con Firebase Cloud Messaging (FCM) para la aplicación **CiudadAlerta**.

## Arquitectura y Funciones

### 1. `notificarSeguidores`
- **Tipo:** Trigger de Firestore (`onDocumentUpdated("incidencias/{id}")`).
- **Comportamiento:**
  - Se activa cuando cambia una incidencia existente.
  - Comprueba si cambió el `estado`, o si aumentaron los contadores de `contadorReportes` o `contadorConfirmaciones`.
  - Excluye al usuario que provocó el cambio si es deducible.
  - Crea un documento en la subcolección `usuarios/{uid}/notificaciones` para cada seguidor.
  - Envía un mensaje Push FCM **data-only** (`titulo`, `cuerpo`, `incidenciaId`, `tipo`) para su procesamiento por `CiudadAlertaMessagingService`.

### 2. `verificar48h`
- **Tipo:** Tarea programada (`onSchedule`, cada 6 horas).
- **Comportamiento:**
  - Identifica incidencias activas cuya `ultimaVerificacionNotificada` (y `fechaCreacion`) sea mayor o igual a 48 horas (definido en `TIEMPO_REVERIFICACION_X_HORAS`).
  - Notifica a `reportantesIds` solicitando confirmación si la incidencia sigue presente.
  - Actualiza `ultimaVerificacionNotificada = Date.now()` para prevenir re-notificaciones en las siguientes 48 horas.

---

## Estructura de Proyecto

```text
CiudadAlerta/
├── firebase.json
├── .firebaserc
└── functions/
    ├── package.json
    ├── tsconfig.json
    ├── README.md
    └── src/
        └── index.ts
```

---

## Instrucciones para el Administrador / Despliegue

1. Configurar el ID del proyecto en `.firebaserc`:
   Reemplazar `"ciudad-alerta-placeholder"` por el ID real de tu proyecto en Firebase Console.

2. Instalar dependencias e instalar TypeScript:
   ```bash
   cd functions
   npm install
   ```

3. Compilar TypeScript:
   ```bash
   npm run build
   ```

4. Desplegar funciones con Firebase CLI:
   ```bash
   firebase deploy --only functions
   ```
