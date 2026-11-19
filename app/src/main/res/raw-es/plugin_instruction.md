3-Folio EPUB es el plugin de lectura de libros EPUB de AutoJs6 y también funciona como lector independiente. Abre libros EPUB 2 y EPUB 3 con índice, búsqueda de texto completo, marcadores, resaltados y notas, y lectura en voz alta; los scripts también pueden leer el contenido de los libros o abrir el lector.

### Primeros pasos

1. Comprueba que este plugin esté activado en el "Centro de plugins" de AutoJs6.
2. En el gestor de archivos de AutoJs6, toca un archivo EPUB y el libro se abre en el lector; también puedes elegir "Leer EPUB" en el menú de ese archivo.
3. Toca el lado izquierdo o derecho de la página o pulsa una tecla de volumen para pasar página, y toca el centro para mostrar u ocultar la barra de herramientas; el mismo libro se vuelve a abrir donde lo dejaste.
4. Para usarlo sin AutoJs6, abre este plugin desde su icono y toca "Abrir EPUB" para elegir un libro.

### Funciones principales

- **Índice y posición de lectura**: Salta a cualquier capítulo desde "Índice"; cada libro recuerda dónde te quedaste, aunque se mueva o se renombre, y "Empezar desde el principio" borra esa posición.
- **Preferencias de lectura**: En "Preferencias de lectura", ajusta el tamaño y la fuente, el espaciado, la alineación y el paso de página, y elige "Claro", "Sepia", "Oscuro" o "Seguir al host"; "Importar fuente" añade fuentes TTF u OTF.
- **Texto vertical y diseño fijo**: Los libros verticales en chino y japonés se muestran en vertical, y "Dirección del texto" puede forzar texto horizontal o vertical; los libros de diseño fijo ofrecen "Doble página" y zoom con dos dedos.
- **Búsqueda y marcadores**: "Buscar" muestra cada coincidencia del libro con su contexto, y al tocarla saltas a ella; el icono de la barra de herramientas marca la página actual, y "Marcadores" los muestra y elimina.
- **Resaltados y notas**: Selecciona texto y elige "Resaltar" o "Añadir nota", con cinco colores y un estilo subrayado; en "Resaltados y notas" puedes saltar a ellos y editarlos, y "Exportar" los guarda como archivo Markdown.
- **Lectura en voz alta**: "Lectura en voz alta" lee desde la página actual con el motor de texto a voz del sistema, resalta la frase actual y pasa las páginas; ajusta la velocidad, la voz y el "Temporizador de apagado", y activa "Continuar en segundo plano" para seguir tras cerrar el lector.
- **Enlaces e imágenes**: Los enlaces internos permiten volver atrás y las notas al pie se muestran en una ventana emergente; por defecto, los enlaces externos muestran primero la dirección completa y solo pasan al navegador tras confirmar; toca una imagen para verla a pantalla completa.
- **Libros recientes y otras aplicaciones**: El icono del plugin abre tus libros recientes; los EPUB compartidos desde otras aplicaciones se abren directamente, y "Añadir a libros recientes" los conserva en la lista.
- **Apariencia y configuración**: El idioma, el modo nocturno y "Color del tema" siguen a AutoJs6 por defecto; "Configuración" también fija los valores predeterminados de paso de página y lectura en voz alta, y ofrece "Buscar actualizaciones" y "Historial de versiones".

### Uso en scripts

El módulo `epub` de AutoJs6 lo proporciona este plugin. Lee los metadatos, el índice y el texto de un libro sin abrir el lector:

```js
let book = epub.open('./book.epub');
console.log(book.metadata.title, book.metadata.authors.join(', '));
book.toc.forEach(entry => console.log(entry.title));
console.log(book.text(0, { maxChars: 500 }));
book.close();
```

`epub.read()` abre el lector de este plugin y comunica el progreso de lectura al script:

```js
let session = epub.read('./book.epub');
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('error', e => console.warn(e.toString()));
session.on('close', e => console.log('closed:', e.reason));
```

- Las rutas pueden ser absolutas o relativas al directorio de trabajo del script; no se aceptan `content://` ni otras URI.
- Los errores se lanzan como `epub.EpubError`, cuyo `code` indica la causa, por ejemplo `PLUGIN_DISABLED` cuando el plugin está desactivado; compruébalo antes con `epub.isAvailable()`.
- La mayoría de los métodos tienen una forma Async que devuelve una Promise, por ejemplo `epub.openAsync()`.
- AutoJs6 es quien inicia el lector, así que se aplican los límites del sistema para abrir pantallas en segundo plano; ejecuta los scripts que llaman a `epub.read()` con AutoJs6 visible.

### Requisitos

- Android 7.0 o posterior, con el WebView del sistema.
- Abrir libros desde el gestor de archivos y usar el módulo `epub` requieren AutoJs6 6.8.0 o posterior; usarlo por separado desde el icono no requiere AutoJs6.
- La lectura en voz alta necesita un motor de texto a voz con datos de voz para el idioma del libro.
- En Android 13 y posteriores, los controles de lectura en la notificación necesitan el permiso de notificaciones; la lectura en voz alta funciona aunque lo deniegues.

### Preguntas frecuentes

**Motivos por los que "Leer EPUB" no aparece en el gestor de archivos**

Comprueba que AutoJs6 cumple el requisito de versión y que este plugin está activado en el "Centro de plugins"; cuando se cumplen ambos, los archivos EPUB ofrecen "Leer EPUB".

**Formatos de libro compatibles**

Se admiten EPUB 2 y EPUB 3, incluidos los libros de diseño fijo. PDF, MOBI, AZW, CBZ y los audiolibros no se admiten; conviértelos antes a EPUB con una herramienta como Calibre. Los libros protegidos con DRM muestran un aviso y no se pueden leer.

**Casos en que la lectura en voz alta no suena**

Comprueba que el motor de texto a voz del sistema tiene datos de voz para el idioma del libro y sube el volumen multimedia. Si el dispositivo no tiene ningún motor disponible, aparece un aviso tras unos 20 segundos.

**Casos en que este plugin usa la red**

Este plugin nunca sube libros ni datos de lectura; solo se conecta cuando un libro hace referencia a recursos remotos o cuando tocas "Buscar actualizaciones". Los libros pueden contener scripts, así que abre solo libros de confianza.

Consulta la [documentación de AutoJs6](https://docs.autojs6.com/#/epub) para la API de scripts completa, y la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Folio-EPUB) para más detalles.
