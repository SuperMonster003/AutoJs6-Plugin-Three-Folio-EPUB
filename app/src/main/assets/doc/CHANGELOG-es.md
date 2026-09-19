******

### Historial de versiones

******

# v1.0.0

###### 2026/09/19

* `Aviso` Compilación de desarrollo: las fases P0 de la hoja de ruta (esqueleto, validación de Readium, muestras de prueba) están en curso; la primera versión pública llega con la fase P8
* `Función` Un botón principal `Leer EPUB` y una acción de menú para archivos `.epub` en el administrador de archivos de AutoJs6 (ID de complemento `readium-epub-reader`, Explorer Action v2); los archivos con extensión `.epub` que el host informa como `application/zip` también se aceptan
* `Función` Base del lector: los libros EPUB 2 y EPUB 3 se representan con el navegador de Readium, con índice y enlaces externos confirmados
* `Función` Memoria de la posición de lectura: la última posición de cada libro se guarda bajo la huella de su contenido (clave rápida al abrir, luego SHA-256 del archivo completo) y se restaura en la siguiente apertura; `Empezar desde el principio` la borra
* `Función` Interfaz del lector: título del libro y capítulo actual en la barra de herramientas, barra de progreso con posición sintética y porcentaje, modo inmersivo con un toque en el centro, zonas de toque y teclas de volumen para pasar páginas, y conmutador de modo de desplazamiento
* `Función` Los libros se leen en su lugar a través del descriptor de archivo concedido, con lecturas posicionales; nada se copia ni se extrae al almacenamiento
* `Función` Interfaz, instrucciones, README y changelog en 10 idiomas
* `Dependencia` Se añade Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
