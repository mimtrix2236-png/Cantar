# Cantar — Dar la Nota

Aplicación Android de karaoke y entrenamiento vocal. Permite elegir canciones, ajustar la tonalidad, seguir la melodía, medir la afinación en tiempo real y competir por puntos.

## Estado

Primera base Android en Kotlin + Jetpack Compose. La biblioteca inicial contiene canciones de demostración y permite importar archivos locales. La detección de tono y el análisis de pistas se desarrollarán por etapas; no se presenta todavía como motor de afinación terminado.

## Requisitos

- Android Studio reciente
- JDK 17
- Android SDK 35

## Compilar

Abrir el proyecto en Android Studio y ejecutar la tarea `assembleDebug`.

## Privacidad

El micrófono se utilizará para medir la frecuencia fundamental de la voz. La app solicitará permiso antes de escuchar.
