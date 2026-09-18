Usar Readium EPUB Reader desde el administrador de archivos de AutoJs6:

1. Instale y active el complemento `Readium EPUB Reader`.
2. Toque un archivo `.epub`, o abra su menú y elija `Leer EPUB`.
3. El libro se abre en un lector basado en Readium Kotlin Toolkit.

El complemento recibe acceso temporal de lectura al archivo seleccionado y a su carpeta mediante content URI. Nunca recibe una ruta del sistema de archivos, nunca copia el libro al almacenamiento y lee el contenedor EPUB directamente a través del descriptor de archivo concedido.

Etapa actual: el lector muestra el libro con la configuración predeterminada de Readium y un índice. La posición de lectura, los marcadores, las preferencias, la búsqueda, la lectura en voz alta, el diseño fijo, la importación de fuentes, la entrada independiente desde el lanzador y la API de scripts `epub` se siguen en ROADMAP.md y llegarán en versiones posteriores.

Los libros pueden contener scripts y recursos remotos; el complemento mantiene el comportamiento predeterminado de Readium y no los bloquea, incluidos los recursos `http://` sin cifrar. Abra solo libros de confianza.

Explorer Action v2 admite tanto el botón principal como el menú de un solo archivo. Se requiere AutoJs6 build 5269 o posterior.
