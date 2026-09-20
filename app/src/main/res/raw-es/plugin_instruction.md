Usar Readium EPUB Reader desde el administrador de archivos de AutoJs6:

1. Instale y active el complemento `Readium EPUB Reader`.
2. Toque un archivo `.epub`, o abra su menú y elija `Leer EPUB`.
3. El libro se abre en un lector basado en Readium Kotlin Toolkit.

Toque el tercio izquierdo o derecho de la página o pulse las teclas de volumen para pasar páginas; toque el centro para ocultar o mostrar la barra de herramientas. La posición de lectura se guarda por libro y se restaura en la siguiente apertura; elija `Empezar desde el principio` en el menú para borrarla.

El complemento recibe acceso temporal de lectura al archivo seleccionado y a su carpeta mediante content URI. Nunca recibe una ruta del sistema de archivos, nunca copia el libro al almacenamiento y lee el contenedor EPUB directamente a través del descriptor de archivo concedido.

Etapa actual: el lector muestra el libro con la configuración predeterminada de Readium, ofrece un índice, recuerda la posición de lectura de cada libro, proporciona modo de desplazamiento, zonas de toque, teclas de volumen y modo inmersivo, y cuenta con un panel de preferencias para el tamaño del texto, la fuente, los espaciados, la alineación, las columnas y los temas, importa sus propias fuentes TTF u OTF y admite libros CJK verticales y de derecha a izquierda, y muestra los libros de diseño fijo a una o dos páginas, y busca en todo el libro, y guarda marcadores, y gestiona enlaces internos, notas e imágenes con zonas de toque configurables y teclado, y lee en voz alta con el motor de texto a voz del sistema. El icono de la aplicación abre un lanzador independiente con los libros recientes y el selector de documentos del sistema, y otras aplicaciones pueden entregar un EPUB mediante `ACTION_VIEW`. La página de ajustes y la API de scripts `epub` se siguen en ROADMAP.md y llegarán en compilaciones posteriores.

Los libros pueden contener scripts y recursos remotos; el complemento mantiene el comportamiento predeterminado de Readium y no los bloquea, incluidos los recursos `http://` sin cifrar. Abra solo libros de confianza.

Explorer Action v2 admite tanto el botón principal como el menú de un solo archivo. Se requiere AutoJs6 build 5269 o posterior.
