# Vinilos - Dream Team

Proyecto del curso de Desarrollo de Aplicaciones Móviles (MISO, Universidad de los Andes).

Aplicación Android desarrollada en **Kotlin** para consultar y gestionar álbumes, artistas y coleccionistas de música.

## Backend

La app consume el API REST de [BackVynils](https://github.com/TheSoftwareDesignLab/BackVynils). Por defecto usa la API publicada en Render (`https://backvynils-q6yc.onrender.com/`).

### Usar la API local de Docker

En debug la app puede usar BackVynils corriendo en Docker en tu computador. Release y el CI siempre usan Render.

1. Levantar BackVynils en Docker y revisar que responda en `http://localhost:3000/albums`.
2. Agregar la URL en `local.properties`, en la raíz del proyecto. Ese archivo no se sube al repositorio, así que cada uno pone la suya:
   - **Emulador:** `apiBaseUrl=http://10.0.2.2:3000/`. Desde el emulador, `10.0.2.2` es tu computador.
   - **Teléfono por USB:** `apiBaseUrl=http://localhost:3000/` y ejecutar `adb reverse tcp:3000 tcp:3000` cada vez que conectes el teléfono.
3. Sincronizar Gradle y **volver a instalar la app** (▶ *Run* en Android Studio, o `./gradlew installDebug`). La URL queda fija en la app al compilarla, así que si la cambias tienes que reinstalar.

También se puede pasar sin tocar `local.properties`, por ejemplo `./gradlew installDebug -PapiBaseUrl=http://10.0.2.2:3000/`.

Para volver a Render, borrar o comentar la línea `apiBaseUrl` y reinstalar.

Android bloquea `http` sin cifrar. Solo en debug se permite hacia `10.0.2.2`, `localhost` y `127.0.0.1` (ver `app/src/debug/res/xml/network_security_config.xml`).

## Pruebas E2E

Las pruebas E2E están hechas con Espresso, en `app/src/androidTest/java/com/equipo2/vinilos`, y usan la API real de BackVynils: la de Render, o la de Docker si configuraste `apiBaseUrl` (ver [Usar la API local de Docker](#usar-la-api-local-de-docker)). Están organizadas así:

- `support/`: lo que comparten las pruebas de todas las historias, como la regla que abre la app, la espera a la API y las llamadas directas a BackVynils.
- Una carpeta por historia de usuario, por ejemplo `hu07/`, con sus clases de prueba y las funciones que solo usa esa historia.

Los datos que crean las pruebas en la API se borran al terminar.

### Preparación

Abrir un emulador o conectar un teléfono con Android 5.0 (API 21) o superior, con la depuración USB activa, y revisar que aparezca con `adb devices`. Las pruebas apagan las animaciones del dispositivo mientras corren y al terminar las dejan como estaban.

### Desde Android Studio

En el panel *Project*, clic derecho sobre `app/src/androidTest/java/com.equipo2.vinilos` → *Run 'Tests in com.equipo2.vinilos'*.

Los resultados aparecen en el panel *Run*, abajo. Android Studio ejecuta las pruebas con Gradle.

### Por línea de comandos (todas las versiones de Android)

Desde la carpeta del proyecto se instalan la app y la app de pruebas, y luego se ejecutan las pruebas en el dispositivo:

```
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w com.equipo2.vinilos.test/androidx.test.runner.AndroidJUnitRunner
```

En Windows se usa `gradlew.bat` en lugar de `./gradlew`.

## Pruebas unitarias

Las pruebas unitarias están en `app/src/test/java/com/equipo2/vinilos`, en las mismas carpetas que el código que prueban, y `support/` tiene lo que comparten, como los repositorios falsos. Se ejecutan en el computador, sin emulador ni conexión a la API:

- **Desde Android Studio:** clic derecho sobre `app/src/test/java/com.equipo2.vinilos` → *Run 'Tests in com.equipo2.vinilos'*.
- **Por línea de comandos:** `./gradlew testDebugUnitTest` desde la carpeta del proyecto (en Windows, `gradlew.bat testDebugUnitTest`). El reporte queda en `app/build/reports/tests/testDebugUnitTest/index.html`.

## Integrantes

- Juan Pablo Vargas
- Juan David Zea
- Juan Felipe Quiñonez
- Jesús Gómez
