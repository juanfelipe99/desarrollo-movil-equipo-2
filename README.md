# Vinilos · Equipo 2

Aplicación Android nativa en Kotlin para consultar álbumes, artistas y coleccionistas de música en vinilo, y para que los coleccionistas registren álbumes nuevos, les agreguen tracks y sumen músicos a las bandas. Proyecto del curso Ingeniería de Software para Aplicaciones Móviles (MISO, Universidad de los Andes). Funciona desde Android 5.0 (API 21).

## API

La app consume el API REST de [BackVynils](https://github.com/TheSoftwareDesignLab/BackVynils), publicada en:

```
https://back-vynils-api--vargasjuanp.replit.app/api/
```

Para usar otra API, cambiar `publishedApiBaseUrl` en `app/build.gradle.kts` y volver a instalar la app, porque la URL queda fija al compilar.

## Instalar el APK

El APK de cada versión está en [Releases](https://github.com/juanfelipe99/desarrollo-movil-equipo-2/releases). Se descarga en el teléfono y se abre, o se instala con el dispositivo conectado por USB:

```
adb install vinilos-v2.0.0.apk
```

## Construir la app

1. Clonar el repositorio.
   ```
   git clone https://github.com/juanfelipe99/desarrollo-movil-equipo-2.git
   ```
2. Abrirlo en Android Studio con *File → Open* y esperar a que termine la sincronización de Gradle. La primera vez se necesita internet para descargar las dependencias.
3. Elegir un emulador o un teléfono conectado y presionar *Run 'app'*.

Por línea de comandos, desde la carpeta del proyecto (en Windows, `gradlew.bat` en lugar de `./gradlew`):

```
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`, y `./gradlew installDebug` lo instala en el dispositivo conectado.

## Compilación para perfilar

La variante `perfil` es la versión release, con R8 activado, marcada como perfilable para que el Profiler de Android Studio pueda conectarse sin modo de depuración. Se usa para medir el desempeño en teléfonos físicos.

```
./gradlew assemblePerfil
```

El APK queda en `app/build/outputs/apk/perfil/app-perfil.apk`.

## Pruebas

### E2E con Espresso

Están en `app/src/androidTest/java/com/equipo2/vinilos`, con una carpeta por historia de usuario, y usan la API configurada en la app. Necesitan un emulador o un teléfono con Android 5.0 o superior conectado.

- **Android Studio:** clic derecho sobre `app/src/androidTest/java/com.equipo2.vinilos` → *Run 'Tests in com.equipo2.vinilos'*.
- **Línea de comandos:**
  ```
  ./gradlew installDebug installDebugAndroidTest
  adb shell am instrument -w com.equipo2.vinilos.test/androidx.test.runner.AndroidJUnitRunner
  ```

### Unitarias

Están en `app/src/test/java/com/equipo2/vinilos` y no necesitan emulador ni API.

- **Android Studio:** clic derecho sobre `app/src/test/java/com.equipo2.vinilos` → *Run 'Tests in com.equipo2.vinilos'*.
- **Línea de comandos:** `./gradlew testDebugUnitTest`.

## Documentación

La [wiki](https://github.com/juanfelipe99/desarrollo-movil-equipo-2/wiki) tiene la estrategia y los resultados de pruebas, el diseño arquitectónico, el perfilamiento y las micro-optimizaciones, las actas de reuniones y las retrospectivas de cada sprint.

## Integrantes

- Juan Pablo Vargas
- Juan David Zea
- Juan Felipe Quiñonez
- Jesús Gómez
