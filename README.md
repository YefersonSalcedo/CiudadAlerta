# CiudadAlerta

Plataforma colaborativa para Android, al estilo de Waze, donde los ciudadanos reportan y confirman **incidencias urbanas no urgentes** (huecos, alumbrado dañado, basuras acumuladas, daños en andenes, etc.) para que toda la comunidad tenga visibilidad de lo que pasa en su ciudad.

> CiudadAlerta **no** es un canal de emergencias. Para situaciones de riesgo inmediato, contacta a las autoridades (en Colombia, línea **123**).

---

## Tabla de contenido

1. [Características](#-características)
2. [Ciclo de vida de una incidencia](#-ciclo-de-vida-de-una-incidencia)
3. [Tecnologías](#-tecnologías)
4. [Requisitos previos](#-requisitos-previos)
5. [Puesta en marcha](#-puesta-en-marcha)
   - [1. Clonar el repositorio](#1-clonar-el-repositorio)
   - [2. Crear el proyecto en la consola de Firebase](#2-crear-el-proyecto-en-la-consola-de-firebase)
   - [3. Registrar la app Android y `google-services.json`](#3-registrar-la-app-android-y-google-servicesjson)
   - [4. Authentication (Google Sign-In)](#4-authentication-google-sign-in)
   - [5. Cloud Firestore](#5-cloud-firestore)
   - [6. Cloud Storage](#6-cloud-storage)
   - [7. App Check y token de depuración](#7-app-check-y-token-de-depuración)
   - [8. API key de Gemini](#8-api-key-de-gemini)
   - [9. Clave del mapa (Google Maps)](#9-clave-del-mapa-google-maps)
   - [10. Ejecutar la app](#10-ejecutar-la-app)

---

## Características

- **Inicio de sesión únicamente con Google** (sin acceso anónimo ni correo/contraseña).
- **Reporte de incidencias con foto**:
  - Foto tomada con la **cámara** --> la ubicación se toma **automáticamente**.
  - Foto elegida de la **galería** --> el usuario **selecciona la ubicación manualmente**.
- **Análisis de la foto con IA (Gemini)**: devuelve un JSON con categoría, estado, motivo de bloqueo, descripción y si se trata de una emergencia.
- **Validación comunitaria**: cualquier usuario puede marcar una incidencia como resuelta.
- **Denuncias de contenido**: se puede denunciar un reporte por inapropiado o porque no representa una incidencia real.
- **Seguimiento automático**: tras un tiempo, se pregunta a quienes reportaron si la incidencia sigue presente.

---

## Ciclo de vida de una incidencia

```
 Reportada ──► Activa ──► (pasado el tiempo X) ──► Se consulta a quienes reportaron
                 │                                         │
                 │                          ¿Sigue ahí? ◄──┴──► ¿Ya se solucionó?
                 │
                 ├──► N marcas de "resuelta" ──► Se muestra como RESUELTA por un tiempo
                 │                                  └──► Desaparece a los pocos días
                 │
                 └──► M denuncias ──► Se oculta para revisión
```

| Parámetro | Significado |
|-----------|-------------|
| **X** | Tiempo tras el cual se pide confirmar a quienes reportaron si la incidencia sigue o se resolvió |
| **N** | Número de marcas de "resuelta" necesarias para mostrarla como resuelta |
| **M** | Número de denuncias necesarias para ocultar una incidencia |


---

## Tecnologías

- **Firebase**
  - Authentication (Google Sign-In)
  - Cloud Firestore (base de datos)
  - Cloud Storage (fotos de los reportes)
  - App Check (protección de backend)
- **Google Gemini API** (análisis de imágenes), usando la API key de Google AI Studio
- **Google Maps Platform** (Maps SDK for Android) para el mapa de incidencias

---

## Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/YefersonSalcedo/CiudadAlerta.git
cd CiudadAlerta
```

Abre la carpeta en Android Studio y espera a que termine la sincronización de Gradle.

### 2. Crear el proyecto en la consola de Firebase

1. Entra a la [consola de Firebase](https://console.firebase.google.com/) con tu cuenta de Google.
2. Haz clic en **Agregar proyecto** y ponle un nombre (por ejemplo, `CiudadAlerta`).
3. Google Analytics es opcional; puedes activarlo o no.
4. Si trabajas en equipo, **un solo integrante crea el proyecto** y agrega a los demás

### 3. Registrar la app Android y `google-services.json`

1. En la consola: **Configuración del proyecto --> General --> Tus apps --> Agregar app --> Android**.
2. Escribe el **nombre del paquete** exactamente como está en `app/build.gradle(.kts)` (`applicationId`).
3. Agrega la **huella digital SHA-1 y SHA-256** de tu clave de depuración (**obligatorio** para Google Sign-In):

   ```bash
   ./gradlew signingReport
   ```

4. Descarga el archivo **`google-services.json`** y colócalo en:

   ```
   app/google-services.json
   ```

   > Si agregas nuevas huellas SHA después de descargarlo, vuelve a descargar el archivo para tenerlo actualizado.

### 4. Authentication (Google Sign-In)

1. En la consola: **Compilación --> Authentication --> Comenzar**.
2. Pestaña **Sign-in method** --> habilita **Google**.
3. Selecciona un **correo de asistencia del proyecto** y guarda.
4. **No habilites** los proveedores *Anónimo* ni *Correo electrónico/contraseña*: la app solo permite Google.
5. Verifica que las huellas SHA-1/SHA-256 estén registradas (paso 3); si no, el inicio de sesión fallará con errores como `DEVELOPER_ERROR` o código `10`.

### 5. Cloud Firestore

1. **Compilación --> Firestore Database --> Crear base de datos**.
2. Elige una **ubicación** cercana a tus usuarios (no se puede cambiar después).
3. Inicia en **modo de producción** y luego publica las reglas de seguridad (paso 4).
4. Reglas utilizadas por el proyecto (pestaña **Reglas** de Firestore):

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {

       // Incidencias del mapa: lectura pública para que todos vean el mapa, escritura autenticada
       match /incidencias/{incidenciaId} {
         allow read: if true;
         allow create, update: if request.auth != null;
       }

       // Perfil del ciudadano
       match /usuarios/{userId} {
         allow read: if request.auth != null;
         allow write: if request.auth != null && request.auth.uid == userId;
       }

       // Demás colecciones
       match /reportes/{reporteId} { allow read, create: if request.auth != null; }
       match /comentarios/{comentarioId} { allow read, create: if request.auth != null; }
       match /marcasResuelto/{marcaId} { allow read, create: if request.auth != null; }
       match /denuncias/{denunciaId} { allow read, create: if request.auth != null; }
     }
   }
   ```

   **Colecciones del proyecto:**

   | Colección | Lectura | Escritura |
   |-----------|---------|-----------|
   | `incidencias` | Pública (cualquiera ve el mapa) | Crear y actualizar: usuarios con sesión |
   | `usuarios` | Usuarios con sesión | Solo el dueño del perfil (`uid == userId`) |
   | `reportes`, `comentarios`, `marcasResuelto`, `denuncias` | Usuarios con sesión | Solo crear (no se pueden editar ni borrar) |

5. Si la app muestra un error indicando que una consulta **requiere un índice**, el mensaje de Logcat incluye un enlace para crearlo automáticamente en la consola. Ábrelo y confirma.

### 6. Cloud Storage

1. **Compilación --> Storage --> Comenzar**.
2. Elige la ubicación y crea el bucket.

   > Según las políticas vigentes de Firebase, crear un bucket puede requerir tener el proyecto en el **plan Blaze** (pago por uso, con una cuota gratuita).

3. Reglas utilizadas por el proyecto (pestaña **Reglas** de Storage):

   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       // Permite guardar y ver imágenes de incidencias de hasta 5MB
       match /incidencias/{allPaths=**} {
         allow read: if request.auth != null;
         allow write: if request.auth != null
                      && request.resource.size < 5 * 1024 * 1024
                      && request.resource.contentType.matches('image/.*');
       }
       // Permite fotos de perfil de hasta 2MB
       match /perfiles/{allPaths=**} {
         allow read: if request.auth != null;
         allow write: if request.auth != null
                      && request.resource.size < 2 * 1024 * 1024
                      && request.resource.contentType.matches('image/.*');
       }
     }
   }
   ```

   | Ruta | Contenido | Límite | Acceso |
   |------|-----------|--------|--------|
   | `incidencias/` | Fotos de los reportes | 5 MB, solo imágenes | Lectura y escritura con sesión |
   | `perfiles/` | Fotos de perfil | 2 MB, solo imágenes | Lectura y escritura con sesión |

  
### 7. App Check y token de depuración

App Check verifica que las peticiones a Firebase provengan de **tu app legítima**.

**En producción** se usa **Play Integrity**; **en desarrollo** (emulador o builds de depuración) se usa el **proveedor de depuración**, que requiere un **token de depuración** registrado.

1. En la consola: **Compilación --> App Check --> Comenzar**.
2. En la pestaña **Apps**, selecciona tu app Android y registra el proveedor **Play Integrity** (necesitarás las huellas SHA-256 ya registradas).
3. Ejecuta la app en modo **debug** (ver paso 9). Con el proveedor de depuración, la app genera un token único por instalación y lo imprime en **Logcat**. Filtra por:

   ```
   DebugAppCheckProvider
   ```

   Verás un mensaje similar a:

   ```
   Enter this debug secret into the allow list in the Firebase Console for your project: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
   ```

4. Copia ese token y regístralo en la consola:
   **App Check --> Apps --> (tu app) --> menú ⋮ --> Administrar tokens de depuración --> Agregar token de depuración**.
5. Vuelve a ejecutar la app: las peticiones a Firebase ya deberían pasar la verificación.

> **Cada integrante del equipo registra su propio token de depuración** (uno por cada dispositivo/emulador y reinstalación de la app, porque el token cambia al borrar los datos de la app).


### 8. API key de Gemini

El análisis de las fotos usa la **API de Gemini directa** (no Firebase AI Logic), con la clave gratuita de Google AI Studio.

1. Entra a [Google AI Studio](https://aistudio.google.com/) con tu cuenta de Google.
2. Ve a **Get API key --> Create API key** y copia la clave.
3. Agrégala a tu archivo **`local.properties`** (en la raíz del proyecto, junto a `settings.gradle`):

   ```properties
   GEMINI_API_KEY=tu_api_key_aqui
   ```

4. `local.properties` **ya está ignorado por Git**; confirma que aparece en `.gitignore`. **Nunca subas tu API key al repositorio.**

### 9. Clave del mapa (Google Maps)

El mapa de incidencias usa **Maps SDK for Android**, que necesita su propia API key. Es **distinta** de la API key de Gemini y de la clave que Firebase crea automáticamente.

1. Entra a la [consola de Google Cloud](https://console.cloud.google.com/) y selecciona **el mismo proyecto** de Firebase (cada proyecto de Firebase es también un proyecto de Google Cloud).
2. Ve a **APIs y servicios --> Biblioteca**, busca **Maps SDK for Android** y pulsa **Habilitar**.
   > Google Maps Platform exige tener una **cuenta de facturación** vinculada al proyecto, aunque incluye una cuota mensual gratuita. Revisa los precios y límites vigentes antes de publicar la app.
3. Ve a **APIs y servicios --> Credenciales --> Crear credenciales --> Clave de API** y copia la clave.
4. **Restringe la clave**:
   - **Restricciones de la aplicación --> Apps para Android**: agrega el **nombre del paquete** y la huella **SHA-1** de cada integrante del equipo.
   - **Restricciones de la API --> Restringir clave**: selecciona solo **Maps SDK for Android**.
5. Guarda la clave en **`local.properties`**, junto a la de Gemini:
```properties
   GEMINI_API_KEY=tu_api_key_de_gemini
   MAPS_API_KEY=tu_api_key_del_mapa
```
> Igual que la de Gemini, **la clave del mapa no se sube al repositorio**. Aunque esté restringida por paquete y SHA-1, mantenla fuera de Git.


### 10. Ejecutar la app

1. Conecta un dispositivo físico (con **depuración USB** activada) o inicia un emulador con Google Play Services.
2. Selecciona la variante de compilación **debug**.
3. Pulsa **Run ▶**.
4. Al abrir la app, inicia sesión con tu cuenta de Google.
5. Si es la primera vez que corres la app en ese dispositivo, registra el **token de depuración** de App Check (paso 7) y vuelve a ejecutarla.

