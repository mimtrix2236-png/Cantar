# Cantar — Dar la Nota

Aplicación Android nativa de karaoke y entrenamiento vocal, con interfaz azul oscuro y acentos neón.

## Funciones disponibles en esta base

- Interfaz Android con Jetpack Compose, navegación atrás y confirmación de salida.
- Perfiles locales, favoritos, modo Solo y modo Duelo por turnos en un solo teléfono.
- Biblioteca de ejemplos con filtros por idioma y géneros latinoamericanos e internacionales. Los ejemplos no incluyen grabaciones ni licencias.
- Búsqueda de letras en LRCLIB sin API key. LRCLIB proporciona metadatos y letras; no entrega audio.
- Búsqueda externa de pistas karaoke en YouTube. La app abre la búsqueda, pero no extrae ni reproduce el audio de YouTube.
- Importación de archivos de audio propios mediante el selector de documentos de Android.
- Reproducción local con AndroidX Media3 ExoPlayer.
- Transposición de -12 a +12 semitonos mediante PlaybackParameters, manteniendo speed = 1.0 y modificando el factor de pitch.
- Captura de micrófono con AudioRecord y estimación de frecuencia fundamental mediante YIN.
- Importación de una melodía de referencia CSV, sincronizada por milisegundos desde el inicio del archivo de audio.
- Importación de MIDI estándar tipo 0/1 con tiempo PPQ; permite elegir la pista y convertirla a una guía monofónica usando la nota más aguda activa cuando hay acordes.
- Captura vocal que prefiere entrada sin procesar cuando el teléfono la declara compatible y activa cancelación de eco/reducción de ruido solo si el dispositivo ofrece esos efectos.
- Gráfico de notas objetivo y trazo vocal; indicador verde dentro de ±50 cents y ámbar fuera de tolerancia.
- Puntuación final normalizada de 0 a 1.000 puntos, calculada nota por nota a partir del error mediano de afinación de las muestras detectadas.
- Historial y mejores marcas guardados localmente.

## Preparar una sesión puntuable

Para que la puntuación sea real y no una estimación inventada, hacen falta dos archivos del mismo arreglo:

1. Un archivo de audio que el usuario tenga derecho a reproducir.
2. Una referencia melódica CSV o un archivo MIDI compatible con tiempos musicales. El MIDI se convierte a milisegundos usando su tempo y resolución PPQ.

Formato requerido:

```csv
start_ms,duration_ms,midi_note
0,420,60
500,300,62
850,500,64
```

Las tres filas numéricas anteriores son un ejemplo didáctico de formato, no corresponden a ninguna canción. La app no incluye melodías precargadas ni atribuye esos datos a canciones del catálogo.

- `start_ms`: comienzo de la nota desde el inicio del audio.
- `duration_ms`: duración de la nota.
- `midi_note`: número MIDI entero entre 0 y 127.
- Las notas deben estar ordenadas y no superponerse.
- También se acepta el encabezado `start_ms,end_ms,midi_note`.

Al importar el CSV, Cantar ajusta las notas objetivo con la misma transposición aplicada al audio. Hay una plantilla en `docs/melody-reference-template.csv`. En MIDI polifónico, la app sigue la nota más aguda activa de la pista elegida; esto es una reducción práctica, no una transcripción inteligente de la melodía. Si falta el audio o la referencia, la aplicación no presenta una puntuación como si fuera una evaluación real.

## Compatibilidad y permisos

- Android mínimo: API 26.
- Compilación: JDK 17, Android Gradle Plugin y Gradle 8.13.
- compileSdk: Android 36.
- `RECORD_AUDIO`: se solicita en tiempo de ejecución al activar el micrófono.
- `INTERNET`: búsqueda de letras en LRCLIB.
- Los archivos se seleccionan mediante Storage Access Framework; no se solicita acceso general al almacenamiento.

## Smart TV

El botón de TV abre los ajustes de transmisión de pantalla de Android. La duplicación depende de la compatibilidad del teléfono y del televisor. Todavía no hay un receptor Google Cast propio ni una segunda interfaz de TV independiente.

## Compilar

Abrir el proyecto en Android Studio y ejecutar la tarea `:app:assembleDebug` desde la ventana Gradle. Este repositorio todavía no incluye los scripts `gradlew`; desde una terminal con Gradle 8.13 instalado se puede ejecutar:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions ejecuta `:app:testDebugUnitTest` y `:app:assembleDebug` en cada push a `main`, sin publicar ni adjuntar el APK como artefacto. Las pruebas unitarias cubren señales de 440 Hz y 110 Hz a 48 kHz con segundo armónico dominante, silencio, puntuación robusta por mediana, importación CSV/MIDI, tempo MIDI, reducción polifónica, tolerancia de afinación y transposición con velocidad 1×. La existencia de un workflow no sustituye las pruebas en un teléfono real.

## Alcance y limitaciones conocidas

- El catálogo local es una lista de ejemplos para filtros; no representa un catálogo mundial ni una biblioteca musical licenciada.
- LRCLIB puede no tener letra sincronizada para una canción; una letra sin tiempos no permite sincronizar el karaoke.
- La melodía de referencia se debe proporcionar en CSV; no se infiere automáticamente desde el audio ni desde la letra.
- YIN estima una frecuencia fundamental monofónica; voces superpuestas, acompañamiento fuerte, reverberación y micrófonos distintos pueden afectar la precisión.
- La puntuación debe validarse con canciones y dispositivos reales antes de considerarse definitiva.
- Los perfiles y resultados son locales al dispositivo; no hay cuentas en la nube ni duelo online.

## Privacidad

La captura de voz se procesa localmente mediante AudioRecord. La cancelación de eco (AEC) y la reducción de ruido (NS) se activan únicamente si el dispositivo las implementa; no todos los teléfonos ofrecen esos efectos. Para evitar que el acompañamiento se cuele por el micrófono, se recomiendan auriculares cuando la TV o el teléfono reproducen por altavoz. El audio no se sube a LRCLIB ni a YouTube desde el analizador vocal.
