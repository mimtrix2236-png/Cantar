# Cantar — Dar la Nota

Aplicación Android de karaoke y entrenamiento vocal, con identidad visual oscura y acentos neón.

## Incluido en esta primera base

- Proyecto Android nativo con Kotlin y Jetpack Compose.
- Pantalla de inicio y marca Cantar.
- Biblioteca de demostración con filtros por estilo e idioma y búsqueda por canción, artista o género.
- Predominio de géneros latinoamericanos: rock nacional, pop latino, cumbia, cuarteto, salsa, bachata, merengue, vallenato, tango, folklore, reguetón y urbano latino.
- Catálogo de estilos internacionales: pop, rock, metal, grunge, jazz, blues, R&B, hip-hop, rap, reggae, electrónica, dance, disco, country, indie, funk, gospel, chanson, K-pop, J-pop, afrobeats, flamenco y clásica.
- Selector inicial de archivos de audio del dispositivo.
- Pantalla de preparación con tono original y controles de transposición de semitonos.
- Pantallas iniciales de práctica y resultados.

## Importante: alcance actual

Los títulos y tonalidades del catálogo son datos de demostración, no una biblioteca musical licenciada ni una conexión a un servicio de streaming. El selector de archivos está integrado como punto de partida, pero la decodificación, reproducción, análisis de la melodía y transposición real del audio todavía deben implementarse. La detección de voz sincronizada, la puntuación fiable y las competencias multijugador también son etapas pendientes. No se debe interpretar la pantalla de práctica actual como un afinador o evaluador terminado.

## Requisitos

- Android Studio reciente
- JDK 17
- Android SDK 35

## Compilar

Abrir el proyecto en Android Studio y ejecutar `:app:assembleDebug`.

## Privacidad

El permiso de micrófono se solicitará cuando se integre la captura de voz. El análisis debe ejecutarse localmente siempre que sea posible y explicar claramente cualquier uso de audio.
