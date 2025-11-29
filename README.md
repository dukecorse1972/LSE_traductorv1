# 🤟 MAS-CA Gestures: Puente de Comunicación Inclusiva

![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?style=for-the-badge&logo=kotlin)
![Android](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)
![AI](https://img.shields.io/badge/AI-TensorFlow%20Lite-orange?style=for-the-badge&logo=tensorflow)
![Status](https://img.shields.io/badge/Status-Prototipo%20Audi%20Challenge-blue?style=for-the-badge)

> **Proyecto finalista del Audi Creativity Challenge 2025.**
> Una herramienta de traducción bidireccional en tiempo real para romper las barreras comunicativas entre personas sordas y oyentes.

---

## 📱 Descripción del Proyecto

**MAS-CA Gestures** no es solo un traductor, es una herramienta de inclusión social. La mayoría de soluciones actuales se centran en traducir texto a voz, olvidando que la comunicación es un proceso de dos vías.

Nuestra aplicación utiliza **Inteligencia Artificial en el dispositivo (Edge AI)** y conectividad en la nube para ofrecer:
1.  **De Sordo a Oyente:** Interpretación de gestos LSE (Lengua de Signos Española) a texto/voz en tiempo real.
2.  **De Oyente a Sordo:** Traducción de voz a vídeo-signos, conectado a una base de datos global.

## ✨ Funcionalidades Clave

### 1. 📷 Modo Traductor (LSE -> Texto)
* **Reconocimiento Biométrico:** Utiliza **MediaPipe** para extraer 21 puntos clave de la mano en 3D.
* **Análisis Temporal:** No analiza fotos estáticas. Implementa una red neuronal **LSTM (Long Short-Term Memory)** que procesa secuencias de 60 frames para entender el movimiento y el contexto.
* **Privacidad Total:** Todo el cálculo matemático se realiza en el móvil con **TensorFlow Lite**. No se envían imágenes a la nube.

### 2. 🗣️ Modo Oyente (Voz -> Signos)
* **Traductor Global:** Integra **Google ML Kit** para traducir la voz del usuario a múltiples idiomas (Inglés, Francés, Alemán...) antes de buscar el signo.
* **Base de Datos Infinita:** Conexión en tiempo real mediante *Web Scraping* ético con **SpreadTheSign**, permitiendo el acceso a miles de vídeos sin aumentar el tamaño de la app.
* **Interfaz Adaptativa:** Reproductor de vídeo integrado con modo pantalla completa.

### 3. 🎨 Interfaz "Cyberpunk Social"
* Diseño moderno desarrollado en **Jetpack Compose**.
* Navegación por gestos (Swipe) y animaciones holográficas en el fondo.
* Feedback visual y sonoro para confirmar la comprensión del mensaje.

---

## 🛠️ Arquitectura y Tecnologías

Este proyecto combina múltiples tecnologías avanzadas en un ecosistema Android nativo:

* **Lenguaje:** Kotlin.
* **UI Toolkit:** Jetpack Compose (Material 3).
* **Visión Artificial:** Google MediaPipe (Hand Tracking).
* **Machine Learning:** TensorFlow Lite (Modelo LSTM personalizado).
* **Procesamiento de Lenguaje Natural:** Google ML Kit (On-device Translation).
* **Conectividad:** Jsoup (para conexión con bases de datos web) & Corrutinas para asincronía.
* **Cámara:** CameraX con análisis de imagen optimizado.

---

## 🚀 Instalación y Pruebas

1.  Clona este repositorio.
2.  Abre el proyecto en **Android Studio Koala** (o superior).
3.  Sincroniza las dependencias de Gradle.
4.  Conecta un dispositivo físico Android (Recomendado debido al uso intensivo de NPU/GPU para la IA).
5.  Ejecuta la aplicación.

> **Nota:** Se requieren permisos de **Cámara** (para ver los gestos) e **Internet** (para el modo Oyente/Diccionario online).

---

## 🤝 Créditos y Equipo

Desarrollado con ❤️ por el equipo **MAS-CA** del **I.E.S Hermanos Amorós** (2º Bachillerato):

* **Darío** - Lead Developer & AI Engineer
* **Manuel** - UX/UI Design & Concept
* **Raúl** - Documentation & Research
* **Wladimir López de Zamora** - Driver & Mentor

---

## ⚖️ Aviso Legal y Fuentes de Datos

Este software es un **prototipo educativo y de investigación** desarrollado exclusivamente para el *Audi Creativity Challenge*.

* **Reconocimiento de Gestos:** El modelo neuronal ha sido entrenado con un dataset propio creado por el equipo.
* **Vídeos de Signos:** El "Modo Oyente" utiliza materiales de [SpreadTheSign](https://www.spreadthesign.com/) mediante técnicas de scraping en tiempo real para demostrar la viabilidad técnica de una conexión a bases de datos globales. **Todo el contenido de vídeo pertenece al Centro Europeo de la Lengua de Signos.**
* El equipo MAS-CA ha contactado con la organización para proponer una colaboración futura y respetar la propiedad intelectual en caso de un lanzamiento comercial.

---

<p align="center">
  <i>"La tecnología no debería crear barreras, sino derribarlas." - Equipo MAS-CA</i>
</p>
