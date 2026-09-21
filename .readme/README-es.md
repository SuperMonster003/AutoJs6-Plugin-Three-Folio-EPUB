<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="readium-epub-reader-ic-launcher" border="0" width="128" />
  </p>

  <p>Lee libros EPUB con navegación, búsqueda, lectura en voz alta y acceso desde scripts</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas (Languages)

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/.readme/README-ar.md)

******

### Introducción

******

Lectura con un toque: abra un archivo `.epub` directamente desde el administrador de archivos de AutoJs6, con el botón principal `Leer EPUB` o desde el menú contextual. El lector se basa en [Readium Kotlin Toolkit](https://github.com/readium/kotlin-toolkit) 3.4.0, el motor de código abierto que usan muchos lectores comerciales.

El complemento lee el libro directamente a través del descriptor de archivo temporal concedido por el anfitrión. Nunca recibe una ruta del sistema de archivos, nunca copia el libro y nunca lo extrae al almacenamiento.

> 1.0.0 es la primera versión. El lector abre libros EPUB 2 y EPUB 3 con un índice, recuerda la posición de lectura de cada libro, ofrece modo de desplazamiento, zonas de toque, teclas de volumen y modo inmersivo, un panel de preferencias (tamaño del texto, fuente, espaciados, alineación, columnas y temas que pueden seguir el modo nocturno del anfitrión), fuentes TTF / OTF importadas, libros CJK verticales y de derecha a izquierda, libros de diseño fijo a página simple o doble, búsqueda de texto completo, marcadores, enlaces dentro del libro, notas e imágenes, y lectura en voz alta con el motor de texto a voz del sistema. El icono de la aplicación abre un lanzador con los libros recientes y el selector de documentos del sistema, otras aplicaciones entregan un EPUB mediante `ACTION_VIEW`, y la página de configuración cubre los valores predeterminados del lector, los datos guardados en el dispositivo y una comprobación manual de actualizaciones. La API de scripts `epub`, la sesión del lector del anfitrión y tres scripts de ejemplo se distribuyen con AutoJs6 6.8.0 (compilación 5282). Los resaltados, las notas y la exportación están previstos para 1.1.0 (ROADMAP.md, P9).

******

### Capturas de pantalla

******

Tomadas en un teléfono con los libros de muestra generados en `docs/fixtures` (no se muestra ningún libro de terceros); la interfaz sigue el idioma de AutoJs6, aquí inglés:

<table>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/reader.png?raw=true" alt="reader" width="180" /><br/>Lectura</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/table-of-contents.png?raw=true" alt="table-of-contents" width="180" /><br/>Índice</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/preferences.png?raw=true" alt="preferences" width="180" /><br/>Preferencias de lectura</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/search.png?raw=true" alt="search" width="180" /><br/>Búsqueda de texto completo</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/bookmarks.png?raw=true" alt="bookmarks" width="180" /><br/>Marcadores</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/read-aloud.png?raw=true" alt="read-aloud" width="180" /><br/>Lectura en voz alta</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/dark-theme.png?raw=true" alt="dark-theme" width="180" /><br/>Tema oscuro</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/sepia-theme.png?raw=true" alt="sepia-theme" width="180" /><br/>Tema sepia</td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/vertical-ja.png?raw=true" alt="vertical-ja" width="180" /><br/>Japonés vertical</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/fixed-layout.png?raw=true" alt="fixed-layout" width="180" /><br/>Diseño fijo</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/launcher.png?raw=true" alt="launcher" width="180" /><br/>Libros recientes</td>
    <td align="center"><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/images/screenshots/settings.png?raw=true" alt="settings" width="180" /><br/>Configuración</td>
  </tr>
</table>

******

### Funciones destacadas

******

- Motor Readium: los libros EPUB 2 (NCX) y EPUB 3 (NAV) se representan con el navegador de Readium y Readium CSS, incluidos enlaces internos, notas al pie e imágenes.
- Sin copias: el contenedor EPUB se lee en su lugar mediante un descriptor de solo lectura con lecturas posicionales, así que incluso los libros grandes se abren sin archivo de caché.
- Índice: salte a cualquier capítulo desde la barra de herramientas; las entradas anidadas conservan su nivel.
- Memoria de la posición de lectura: la última posición de cada libro se guarda en el almacenamiento privado del complemento bajo una huella de su contenido, por lo que el mismo libro se reanuda incluso después de moverlo o renombrarlo; `Empezar desde el principio` la borra.
- Interfaz del lector: título y capítulo en la barra de herramientas, barra de progreso con posición y porcentaje, modo inmersivo con un toque en el centro, zonas de toque y teclas de volumen para pasar páginas, y modo de desplazamiento o paginado.
- Preferencias de lectura: un panel inferior ajusta el tamaño del texto, la fuente, el interlineado, los márgenes, el espaciado de párrafos, la alineación, los guiones, los estilos del editor, el número de columnas y el diseño paginado o de desplazamiento; los cambios se aplican de inmediato y se recuerdan para cada libro. Temas claro, sepia y oscuro, o seguir el modo nocturno del host; la barra de herramientas y las barras del sistema adoptan los colores del tema.
- Importación de fuentes: elija archivos TTF u OTF con el selector de documentos del sistema; se validan, se guardan de forma privada en el complemento (hasta 10 fuentes de 20 MB cada una), se listan en el panel de preferencias junto a las fuentes integradas, se sirven a todos los libros y se eliminan desde el mismo panel.
- Libros CJK verticales y de derecha a izquierda: la progresión de lectura sigue a la publicación, así que las zonas de toque se invierten en los libros de derecha a izquierda; los libros japoneses y chinos con progresión de página de derecha a izquierda se muestran en vertical, y una preferencia `Dirección del texto` fuerza el texto horizontal o vertical. La interfaz sigue el idioma de AutoJs6 para su propia dirección de diseño, independientemente del libro.
- Libros de diseño fijo: las páginas se cuentan como `Página x de N`, el panel ofrece la opción `Doble página` (auto muestra dos páginas una junto a otra en horizontal) y oculta las preferencias de texto que no se aplican; el zoom de pellizco y el desplazamiento son de Readium.
- Búsqueda de texto completo: la entrada `Buscar` de la barra de herramientas encuentra cada coincidencia del libro, de 50 en 50 (hasta 500), agrupadas por capítulo con el texto circundante; tocar un resultado salta a él, lo resalta en la página y ofrece anterior / siguiente sobre la barra de progreso.
- Marcadores: el icono de la barra de herramientas marca la página actual (se rellena cuando la página tiene marcador) y la entrada `Marcadores` lista cada marcador con su capítulo, un extracto y la hora, del más reciente al más antiguo, para saltar a él, eliminarlo o borrarlos todos; se guardan por libro (hasta 500) junto a la posición de lectura.
- Gestos, teclas y enlaces: zonas de toque (desactivadas, izquierda / derecha o arriba / abajo), teclas de volumen, teclado físico y menú de selección de texto (copiar, compartir, búsqueda web y aplicaciones de procesamiento de texto); los enlaces internos guardan una pila de retorno, las notas se abren en un diálogo, los enlaces externos se abren tras confirmar o directamente, y una imagen tocada se abre a pantalla completa.
- Lectura en voz alta: `Lectura en voz alta` en el menú desplegable lee el libro desde la página actual con el motor de texto a voz del sistema, resalta la frase que se está leyendo y pasa las páginas; una barra bajo la página y una notificación multimedia ofrecen reproducir / pausar, frase anterior / siguiente y detener, los botones de los auriculares funcionan, la velocidad, el tono, el idioma y la voz son ajustables, la lectura continúa con la pantalla apagada y se detiene al cerrar el lector salvo que `Continuar en segundo plano` esté activado, y los ajustes de lectura en voz alta añaden un temporizador de apagado (15 / 30 / 60 minutos o el final del capítulo) y un interruptor para mantener la pantalla encendida.
- Enlaces externos: al tocar un enlace `http` o `https` se muestra la dirección completa y el navegador del sistema solo se abre tras confirmar.
- Lanzador independiente: el icono de la aplicación abre una cuadrícula de libros recientes con portada, título, autor, progreso y última lectura, más un botón `Abrir EPUB` que elige un libro con el selector de documentos del sistema; el lector es el mismo que abre el administrador de archivos.
- Se abre desde otras aplicaciones: un administrador de archivos, un navegador o una aplicación de correo puede entregar un EPUB `content://` mediante `ACTION_VIEW`; `Añadir a libros recientes` en el menú desbordante lo conserva en el lanzador cuando el remitente permite un acceso duradero.
- Página de ajustes con tema, paso de páginas, valores predeterminados de la lectura en voz alta, enlaces y gestión de datos, además del historial de versiones y una comprobación manual de actualizaciones que solo consulta GitHub al tocarla
- Servicio de scripts: un servicio Binder `org.autojs.plugin.EPUB` permite al anfitrión AutoJs6 leer un libro sin abrir el lector (metadatos, índice, orden de lectura, texto de capítulos como texto plano o Markdown ligero, recursos, búsqueda de texto completo y recuento de posiciones), con solicitudes acotadas, como máximo 8 libros abiertos a la vez y acceso limitado al anfitrión; AutoJs6 6.8.0 lo expone a los scripts como el módulo `epub` (vea Scripts más abajo).
- Sesión de lectura dirigida por el anfitrión: el anfitrión AutoJs6 puede abrir el lector sobre un libro a través del servicio `org.autojs.plugin.EPUB` y seguirlo (eventos de posición, marcador y cierre), saltar a un locator, href o progresión, pasar páginas o capítulos y ajustar las preferencias de lectura; el lector solo arranca mediante el lanzamiento explícito del anfitrión con un token de sesión de un solo uso, y cerrar la sesión deja el lector abierto para el usuario salvo que el anfitrión pida terminarlo.
- Integración con el anfitrión: los menús y diálogos siguen el idioma y el modo oscuro de AutoJs6; el sobre de Explorer Action se valida estrictamente antes de abrir cualquier contenido.
- Multilingüe: interfaz, instrucciones, README y changelog disponibles en 10 idiomas.

******

### Instalación

******

1. Desde el centro de complementos: abra `Complementos` en AutoJs6, elija `Readium EPUB Reader` en la lista oficial y toque instalar; el centro de complementos descarga el APK firmado, lo instala y le permite habilitar el complemento.
2. Desde GitHub: descargue el APK de la página [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) (el nombre del archivo lleva un CRC32 y `SHA256SUMS` lista la suma de comprobación), instálelo y luego habilite el complemento en el centro de complementos.
3. Requisitos: compilación interna de AutoJs6 5269 o posterior para la entrada del administrador de archivos, AutoJs6 6.8.0 (compilación 5282) o posterior para la API de scripts `epub`, Android 7.0 o posterior y un WebView del sistema.

******

### Cómo se usa

******

1. Descargue el APK más reciente del complemento desde la página [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) e instálelo en el dispositivo.
2. Abra el centro de complementos de AutoJs6 y active el complemento `Readium EPUB Reader`.
3. En el administrador de archivos de AutoJs6, toque un archivo `.epub`, o abra su menú (más acciones) y elija `Leer EPUB`.
4. Use el botón de índice de la barra de herramientas para saltar entre capítulos y el botón de preferencias para ajustar el texto y el tema; toque el tercio izquierdo o derecho de la página o pulse las teclas de volumen para pasar páginas, y toque el centro para ocultar o mostrar la barra de herramientas; pulse Atrás para cerrar el lector, la posición se recuerda.
5. Sin el administrador de archivos, toque el icono de la aplicación: el lanzador muestra sus libros recientes y `Abrir EPUB` elige un libro con el selector de documentos del sistema; los libros abiertos así permanecen en la lista con su portada y su progreso.
6. Desde otra aplicación (un administrador de archivos, las descargas de un navegador, un adjunto de correo), elija este lector para un archivo `.epub`; el libro se abre de la misma forma y `Añadir a libros recientes` en el menú desbordante lo conserva en la lista del lanzador cuando la aplicación remitente permite un acceso duradero.
7. Abra `Configuración` desde el menú del lanzador o el menú desbordante del lector para configurar el tema, el paso de páginas, los valores predeterminados de la lectura en voz alta y los enlaces, borrar los datos que guarda el complemento, leer el historial de versiones o buscar actualizaciones (la comprobación solo contacta con GitHub al tocarla).
8. Desde un script: `epub.open(path)` lee un libro (metadatos, índice, texto, búsqueda) y `epub.read(path)` abre este lector e informa de su posición; vea la sección Scripts más abajo y los ejemplos `Libros electrónicos` de AutoJs6.

> Si el complemento no aparece en el centro de complementos, actualice primero AutoJs6 a una versión reciente (compilación interna 5269 o posterior). Explorer Action v2 admite el botón principal y el menú contextual para un archivo, con permisos temporales de lectura del documento y su carpeta.

******

### Scripts

******

AutoJs6 6.8.0 añade el módulo global `epub` (alias `$epub`), al que sirve este complemento: lea un libro sin abrir el lector, o abra el lector y sígalo desde un script. AutoJs6 incluye tres scripts de ejemplo en `Ejemplos > Libros electrónicos`, y la referencia está en la [documentación de AutoJs6](https://docs.autojs6.com/#/epub):

Metadatos, índice y texto de los capítulos:

```javascript
let book = epub.open('./books/lighthouse.epub');
console.log(book.metadata.title, '-', (book.metadata.authors || []).join(', '));
book.toc.forEach(entry => console.log(entry.title, entry.href, (entry.children || []).length, 'children'));
console.log(book.readingOrder.length, 'resources,', book.positions, 'positions');
let first = book.readingOrder[0];
console.log(book.text(first.href, { format: 'markdown' }));
book.close();
```

Portada, búsqueda y funciones de conveniencia:

```javascript
let path = './books/lighthouse.epub';
let book = epub.open(path);
try {
    console.log('cover saved to', book.cover(files.cwd(), { overwrite: true }));
} catch (e) {
    if (!(e instanceof epub.EpubError) || e.code !== 'RESOURCE_NOT_FOUND') throw e;
    console.log('this book has no cover');
}
book.search('lighthouse', { limit: 20 }).forEach(hit => console.log(hit.title || hit.href, ':', hit.text));
files.write('./lighthouse.txt', book.textAll({ maxChars: 2 * 1024 * 1024 }));
book.close();
console.log(epub.metadata(path).language); // the convenience functions open and close the book themselves
epub.tocAsync(path).then(toc => console.log(toc.length, 'entries'));
```

Abrir el lector y seguir la posición:

```javascript
let session = epub.read('./books/lighthouse.epub', { progression: 0.25, preferences: { theme: 'sepia' } });
session.on('open', e => console.log('opened', e.title, 'at', e.href, '|', e.positions, 'positions'));
session.on('progress', e => console.log((e.totalProgression * 100).toFixed(1) + '%', e.chapterTitle || e.href));
session.on('bookmark', e => console.log('bookmark', e.action, e.locator.href, '| total', session.bookmarks().length));
session.on('close', e => console.log('closed:', e.reason)); // user, host, replaced, timeout, error or overflow
setTimeout(() => session.isOpen && session.nextChapter(), 30 * 1000);
setTimeout(() => session.isOpen && session.close(), 60 * 1000);
```

Las rutas son relativas al directorio de trabajo del script o absolutas (no se aceptan URI `content://`). Cada llamada lanza un `EpubError` con un `code` (`PLUGIN_UNAVAILABLE`, `NOT_EPUB`, `ENCRYPTED`, `PARSE_FAILED`, `TIMEOUT`, ...) cuando el complemento o el libro no se pueden usar, `epub.isAvailable()` indica si el complemento está instalado y habilitado, y cada método tiene un gemelo `*Async` que devuelve una Promise.

******

### Formatos compatibles

******

El complemento reconoce la siguiente extensión, además de archivos sin extensión marcados explícitamente como `application/epub+zip` por el anfitrión:

```text
epub
```

Solo se admite EPUB: libros reajustables y de diseño fijo en EPUB 2 o EPUB 3. Los archivos de cómic (CBZ), los audiolibros, los PDF y los libros protegidos con LCP quedan fuera del alcance; un libro marcado como cifrado con LCP se notifica como ilegible en lugar de mostrar contenido corrupto.

******

### Compatibilidad

******

Lo que necesita el complemento, dónde se verificó y lo que queda fuera del alcance:

- AutoJs6: compilación interna 5269 o posterior para la entrada del administrador de archivos (Explorer Action v2); la API de scripts `epub`, la sesión del lector del anfitrión y los scripts de ejemplo necesitan AutoJs6 6.8.0 (compilación 5282), la última compilación del anfitrión auditada para esta versión.
- Android 7.0 (API 24) hasta Android 16 (API 37, el objetivo); las páginas se renderizan en el WebView del dispositivo, por lo que se espera un Android System WebView o Chrome actualizado. El complemento no tiene bibliotecas nativas y funciona sin cambios en dispositivos con páginas de 16 KB.
- Verificado en AVD API 24 / 33 / 36 / 37, Sony Xperia XZ1 Compact (Android 9), Redmi 12C (Android 13, MIUI) y Xiaomi Pad 6 (Android 15, lado del servicio); la matriz dispositivo x escenario, sus desviaciones y las versiones de WebView están en `docs/dev/compatibility-matrix.md`.
- Libros: EPUB 2 y EPUB 3, reajustables y de diseño fijo, CJK vertical y de derecha a izquierda. Los libros protegidos con DRM (LCP, Adobe ADEPT) se informan como protegidos y nunca se renderizan; PDF, MOBI, AZW, CBZ y audiolibros quedan fuera del alcance.
- La lectura en voz alta necesita un motor de texto a voz con datos de voz para el idioma del libro (Google Speech Services, el motor del fabricante o cualquier otro motor instalado); un dispositivo sin motor utilizable lo informa tras unos 20 segundos en lugar de quedarse en silencio.
- Tamaño y rendimiento: el APK de release ocupa unos 3.3 MB; un libro de 200 MB se abre en 1 a 3 segundos en un teléfono de 2017, el cálculo de posiciones y de la huella nunca retrasa la primera página, y los libros con miles de capítulos tardan notablemente más en abrirse (`docs/dev/performance-baseline.md`).

******

### Preguntas frecuentes

******

#### ¿Cómo se recuerda mi posición de lectura?

La última posición de cada libro se guarda en el almacenamiento privado del complemento bajo una huella del contenido del archivo, nunca bajo su ruta, así que reabrir el mismo libro continúa donde lo dejó. Elija `Empezar desde el principio` en el menú para borrarla.

#### ¿Puedo cambiar la fuente, el tamaño del texto o el tema?

Sí. Abra el panel de preferencias desde la barra de herramientas para ajustar el tamaño del texto, la fuente (predeterminada del editor, con serifa, sin serifa, monoespaciada o las fuentes de accesibilidad incluidas con Readium), el interlineado, los márgenes, los espaciados, la alineación, las columnas y el tema (claro, sepia, oscuro o seguir al host). Toque `Importar fuente` en el panel para añadir sus propios archivos TTF u OTF; se guardan de forma privada en el complemento y se eliminan desde `Administrar fuentes`.

#### ¿Este complemento sube mis libros a algún sitio?

No. El complemento no tiene servidor propio. La red solo se usa cuando el propio libro hace referencia a recursos remotos y para la comprobación manual de actualizaciones de la página de ajustes, que solo consulta la API GitHub Releases por HTTPS al tocarla y nunca descarga nada.

#### ¿Por qué no se admiten archivos PDF, MOBI o AZW?

El lector está construido sobre el kit de herramientas Readium, que solo renderiza EPUB. PDF necesita otro renderizador, y MOBI / AZW son formatos de Amazon sin un motor de renderizado abierto; conviértalos primero a EPUB con una herramienta como Calibre. Los archivos de cómics (CBZ) y los audiolibros también quedan fuera del alcance.

#### La lectura en voz alta no suena

El complemento habla a través del motor de texto a voz elegido en la configuración del sistema (`Accesibilidad > Salida de texto a voz`). Compruebe que hay instalado un motor con datos de voz para el idioma del libro, que el volumen multimedia está subido y que ninguna otra aplicación retiene el foco de audio (una llamada o la música pausan la lectura). Un libro en un idioma que el motor no puede hablar usa la voz predeterminada del motor, y un dispositivo sin motor utilizable muestra un mensaje tras unos 20 segundos.

#### Una fuente importada no aparece en el libro

Los estilos del editor pueden fijar sus propias fuentes: desactive `Estilos del editor` en el panel de preferencias y vuelva a seleccionar la fuente importada. Solo se aceptan archivos TTF y OTF (las colecciones de fuentes, `.ttc`, se rechazan con su propio mensaje), una fuente se aplica al texto principal, y los títulos a los que el editor asignó una familia concreta la conservan.

#### ¿Cómo se manejan los libros verticales en japonés o chino?

Un libro cuyo spine declara una progresión de páginas de derecha a izquierda y un idioma japonés o chino se renderiza en vertical y pasa las páginas de derecha a izquierda; la preferencia `Dirección del texto` fuerza texto horizontal o vertical para cualquier libro. La interfaz conserva la dirección del idioma de AutoJs6, así que una interfaz en inglés sigue de izquierda a derecha mientras el libro se lee de derecha a izquierda.

******

### Permisos y seguridad

******

El complemento mantiene el comportamiento predeterminado de Readium para el contenido del libro: los scripts y recursos remotos del libro no se eliminan ni se bloquean, incluidos los recursos `http://` sin cifrar. Abra solo libros de confianza.

- Mínimo privilegio: el complemento solo recibe el permiso temporal de lectura del content URI concedido por el anfitrión, nunca ve rutas del sistema de archivos y nunca escribe el libro en el almacenamiento.
- Sobre estricto: la solicitud de Explorer Action debe llevar exactamente un objetivo EPUB, su carpeta, una versión de protocolo coincidente, una compilación del anfitrión compatible y ambos permisos de lectura; todo lo demás se rechaza antes de abrir el archivo.
- Entrada separada para otras aplicaciones: `ACTION_VIEW` lo atiende su propia actividad exportada, que solo acepta documentos `content://` con permiso de lectura (nunca `file://`, nunca una carpeta), mientras que la actividad de Explorer Action sigue protegida por el permiso de complemento de AutoJs6; el permiso de la aplicación remitente solo se conserva si elige `Añadir a libros recientes`.
- Servicio de scripts protegido: el servicio `org.autojs.plugin.EPUB` se exporta detrás del permiso del complemento, solo atiende al paquete anfitrión AutoJs6 con una firma coincidente, comprueba cada solicitud contra límites fijos (longitud del href, ventana de texto, páginas de búsqueda, tamaño de las opciones, 8 libros abiertos, 64 MB por recurso) y nunca inicia el lector desde segundo plano: una sesión de lectura solo entrega al anfitrión un token de un solo uso, el anfitrión inicia por sí mismo la Activity del lector, una sesión no reclamada se cierra a los 60 s y un token incorrecto no abre nada.
- Comprobación de actualizaciones solo a petición: la página de ajustes consulta la API GitHub Releases por HTTPS únicamente cuando toca `Buscar actualizaciones` (como máximo una vez al día, sin redirecciones, respuesta acotada), muestra el resultado y abre la página de la versión en el navegador; el complemento nunca descarga ni instala nada por sí mismo.
- Análisis acotado: un contenedor malformado (no es ZIP, falta `container.xml`, falta el documento de paquete, salto de ruta en el manifest) termina con un mensaje de error en lugar de un bloqueo.
- Los enlaces externos se muestran completos y se abren en el navegador del sistema solo tras confirmar; se rechazan los esquemas distintos de `http` y `https`.
- Los datos de lectura permanecen locales: las posiciones se indexan por una huella del contenido y no se escribe ninguna ruta ni nombre de archivo en el almacenamiento.
- La lectura en voz alta se ejecuta en un servicio de reproducción multimedia no exportado que solo existe mientras una voz lee y se detiene cuando la detiene, termina el libro o se cierra el lector (o, con `Continuar en segundo plano` activado, cuando la detiene desde la notificación); el texto se entrega al motor de texto a voz elegido en los ajustes del sistema, y el complemento no mantiene ningún wake lock.

El manifiesto solicita el permiso de red, el permiso de complemento de AutoJs6 y, para la lectura en voz alta, los permisos de servicio en primer plano (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) más `POST_NOTIFICATIONS` en Android 13+, que se pide una sola vez al iniciar la lectura y puede rechazarse (la lectura continúa entonces sin los controles de la notificación). AndroidX añade además un permiso de firma limitado al paquete que protege los receptores dinámicos no exportados; no concede acceso a los datos del dispositivo. No se solicitan permisos de almacenamiento, multimedia, cámara, ubicación, accesibilidad ni superposición.

******

### Interfaz del complemento

******

La siguiente información está dirigida a desarrolladores; el anfitrión descubre y ejecuta el complemento con estas identidades:

```text
application id: io.github.supermonster003.autojs6.plugin.readium.epub.reader
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: readium-epub-reader
engine: explorer-action
variant: default
protocol version: 2
minimum host build: 5269
audited host build: 5282
audited host protocol: 22
```

Explorer Action v2 admite el botón principal y el menú contextual para un archivo, con permisos temporales de lectura del documento y su carpeta. Se requiere AutoJs6 build 5269 o posterior.

- [Ver la matriz de compatibilidad de Explorer Action](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/explorer-action-compatibility.md)

******

### Hoja de ruta

******

ROADMAP.md registra cada hito como una lista marcable con criterios de aceptación y evidencias: P0 a P8 (el lector, preferencias y fuentes, búsqueda y marcadores, lectura en voz alta, la entrada independiente, el contrato del anfitrión, la API de scripts `epub`, robustez y la puerta de lanzamiento 1.0.0) están marcados; P9 (resaltados, notas y exportación, 1.1.0) está previsto. Los elementos sin marcar describen planes y no capacidades entregadas. Los comentarios a través de Issues son bienvenidos.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.0.0

_2026/09/19_

- `Aviso` Compilación de desarrollo: las fases P0 de la hoja de ruta (esqueleto, validación de Readium, muestras de prueba) están en curso; la primera versión pública llega con la fase P8
- `Función` Un botón principal `Leer EPUB` y una acción de menú para archivos `.epub` en el administrador de archivos de AutoJs6 (ID de complemento `readium-epub-reader`, Explorer Action v2); los archivos con extensión `.epub` que el host informa como `application/zip` también se aceptan
- `Función` Base del lector: los libros EPUB 2 y EPUB 3 se representan con el navegador de Readium, con índice y enlaces externos confirmados
- `Función` Memoria de la posición de lectura: la última posición de cada libro se guarda bajo la huella de su contenido (clave rápida al abrir, luego SHA-256 del archivo completo) y se restaura en la siguiente apertura; `Empezar desde el principio` la borra
- `Función` Interfaz del lector: título del libro y capítulo actual en la barra de herramientas, barra de progreso con posición sintética y porcentaje, modo inmersivo con un toque en el centro, zonas de toque y teclas de volumen para pasar páginas, y conmutador de modo de desplazamiento
- `Función` Panel de preferencias de lectura: el tamaño del texto, la fuente, el interlineado, los márgenes, el espaciado de párrafos, la alineación, los guiones, los estilos del editor, el número de columnas y el diseño paginado o de desplazamiento se aplican de inmediato y se recuerdan para todos los libros; temas claro, sepia y oscuro más `Seguir al host`, con la barra de herramientas y las barras del sistema recoloreadas a juego
- `Función` Importación de fuentes: los archivos TTF y OTF elegidos con el selector de documentos del sistema se validan (firma SFNT, tabla `name`, 20 MB por archivo, 10 fuentes), se guardan de forma privada en `files/fonts/<sha256>` y se sirven al navegador de Readium como declaraciones `@font-face`; las fuentes importadas aparecen en el panel de preferencias junto a las integradas y pueden eliminarse allí
- `Función` Libros CJK verticales y de derecha a izquierda: la progresión de lectura sigue a la publicación (las zonas de toque se invierten en los libros de derecha a izquierda), los libros japoneses / chinos con progresión de página de derecha a izquierda se muestran en vertical mediante Readium CSS, una preferencia `Dirección del texto` fuerza el texto horizontal o vertical, y la dirección del diseño de la interfaz es independiente del libro
- `Función` Libros de diseño fijo: `Página x de N` en la barra de progreso, una preferencia `Doble página` (auto = dos páginas en horizontal, una página, dos páginas) con las preferencias de texto ocultas, y el zoom de pellizco de Readium
- `Función` Búsqueda de texto completo: la entrada `Buscar` de la barra de herramientas abre un panel de resultados que carga 50 coincidencias a la vez (hasta 500), agrupadas por capítulo con su contexto; tocar una coincidencia salta a ella y la resalta en la página, con anterior / siguiente en una barra sobre el progreso
- `Función` Marcadores: un icono de la barra de herramientas añade o quita un marcador de la página actual (con el capítulo y un extracto del texto), y un panel `Marcadores` los lista del más reciente al más antiguo con saltar, eliminar y borrar todo; se guardan por libro (hasta 500) junto a la posición de lectura
- `Función` Controles de lectura: las zonas de toque pueden desactivarse o fijarse en izquierda / derecha o arriba / abajo, los teclados físicos pasan página con las flechas, las teclas de página y el espacio, y el texto seleccionado ofrece copiar, compartir, búsqueda web y las aplicaciones de procesamiento de texto del sistema
- `Función` Enlaces: los enlaces internos del libro se abren en el lector y la tecla atrás vuelve a donde estaba, las notas al pie y finales se abren en un diálogo, y los enlaces externos se abren tras confirmar o, si así lo elige, directamente en el navegador; los enlaces de otros esquemas se rechazan
- `Función` Imágenes: tocar una imagen la abre a pantalla completa con su pie
- `Función` Lectura en voz alta: el menú desplegable lee el libro desde la página actual con el motor de texto a voz del sistema, resalta la frase que se está leyendo y pasa las páginas; una barra bajo la página y una notificación multimedia ofrecen reproducir / pausar, frase anterior / siguiente y detener, los botones de los auriculares funcionan, la velocidad, el tono, el idioma y la voz son ajustables, la lectura continúa con la pantalla apagada y se detiene al cerrar el lector
- `Función` Temporizador de apagado para la lectura en voz alta (15 / 30 / 60 minutos o el final del capítulo), interruptor para mantener la pantalla encendida y `Continuar en segundo plano` (desactivado por defecto): con él activado, la voz continúa tras cerrar el lector hasta el final del libro o del temporizador, la notificación permite pausar, detener o volver a abrir el libro en la frase leída, y al volver a abrir el mismo libro la voz continúa donde va; la posición de lectura se guarda cuando una voz en segundo plano se detiene
- `Función` Los libros se leen en su lugar a través del descriptor de archivo concedido, con lecturas posicionales; nada se copia ni se extrae al almacenamiento
- `Función` Interfaz, instrucciones, README y changelog en 10 idiomas
- `Función` Lanzador independiente: el icono de la aplicación abre una cuadrícula de libros recientes (portada, título, autor, progreso y última lectura, hasta 100) y un botón `Abrir EPUB` que elige un libro con el selector de documentos del sistema; los libros elegidos conservan un permiso de lectura persistente y se reabren desde la cuadrícula, un libro cuyo archivo desapareció se marca como no disponible, y una pulsación larga quita un libro y libera su permiso
- `Función` Apertura desde otras aplicaciones: los administradores de archivos, navegadores y aplicaciones de correo pueden entregar un EPUB `content://` al lector mediante `ACTION_VIEW`; el libro se abre como cualquier otro pero no aparece en el lanzador, salvo que `Añadir a libros recientes` en el menú desbordante logre conservar el acceso concedido por el remitente (si no puede, lo rechaza); las rutas `file://`, las solicitudes sin permiso de lectura y las carpetas se rechazan
- `Función` Página de ajustes e historial de versiones: el menú del lanzador y el menú desbordante del lector abren una página de ajustes para el tema, las zonas de toque, las teclas de volumen, la velocidad, el tono y el temporizador de sueño predeterminado de la lectura en voz alta, los enlaces externos y los datos que guarda el complemento (posiciones de lectura, libros recientes, fuentes importadas, preferencias, cada uno borrado tras confirmar), con una sección Acerca de, el historial de versiones integrado y una comprobación manual de actualizaciones que solo consulta GitHub al tocar y abre la página de la versión en el navegador (sin descargas, `Ignorar esta versión` se recuerda)
- `Función` Servicio de capacidades EPUB para el anfitrión AutoJs6 (hoja de ruta P5.2): el servicio Binder `org.autojs.plugin.EPUB` abre un libro desde el descriptor de solo lectura del anfitrión y responde con metadatos, índice, orden de lectura, texto de capítulos (texto plano o Markdown ligero, paginado), recursos a través de una tubería, búsqueda de texto completo y recuento de posiciones; como máximo hay 8 libros abiertos a la vez, un libro inactivo se cierra a los 5 minutos, cada solicitud se comprueba contra sus límites y solo el anfitrión AutoJs6 puede llamar al servicio
- `Función` Sesión de lectura dirigida por el anfitrión sobre el contrato EPUB (hoja de ruta P5.3): `openReader` abre el libro, emite un token de sesión de un solo uso y deja el lanzamiento al anfitrión, que inicia explícitamente la Activity del lector con ese token; la sesión informa después los eventos `open`, `progress` (como máximo cada 500 ms), `bookmark`, `error` y `close` con una sola generación y una secuencia estrictamente creciente, acepta `goTo` (locator, href o progresión), `navigate` (página o capítulo), `setPreferences` (el subconjunto de preferencias del contrato; las claves desconocidas se informan, no se aplican), `getBookmarks` y `getState`, sustituye a una sesión anterior, se cierra a los 60 s si ningún lector la reclama, y un `close` del anfitrión deja el lector abierto salvo que pida terminarlo
- `Corrección` Advertencias de lectura de SDK XML v4 con AGP 9.1 y comprobaciones de alineación nativa de APK activadas por error al ensamblar pruebas unitarias JVM, mediante los plugins de compilación compartidos 1.8.3
- `Corrección` Un fallo al escribir el progreso (carpeta del libro eliminada, almacenamiento no escribible) ya no bloquea el lector; ese registro se pierde y la lectura continúa
- `Corrección` El lector ya no muere junto con el anfitrión cuando AutoJs6 se detiene o se actualiza mientras se lee su proveedor de ajustes; esa lectura simplemente falla y no se aplican el idioma / modo nocturno del anfitrión
- `Corrección` Una sesión del anfitrión cuyo intent de inicio llega a un lector que ya está en la cima de su tarea (entrega single-top, por ejemplo después de que un script dejara el lector abierto) se abre ahora en un lector nuevo en lugar de esperar sin reclamar hasta el tiempo límite de 60 s; el lector anterior termina como si fuera reemplazado (hoja de ruta P6.2)
- `Corrección` Un libro cuyo XML de NCX / OPF está truncado o malformado ahora falla de forma segura: el servicio responde `PARSE_FAILED` y el lector muestra su panel de apertura fallida, en lugar del código `INTERNAL` o de un cierre inesperado por el `AssertionError` que lanza el analizador XML de Readium (hoja de ruta P7.1)
- `Corrección` Una página de `search` se limita a 50 s en el lado del plugin y responde `TIMEOUT` cuando la consulta solo coincide al final de un libro enorme (50 000 recursos), de modo que el hilo Binder ya no sigue ocupado más allá del propio tiempo límite de llamada de 60 s del anfitrión (hoja de ruta P7.1)
- `Corrección` Cada WebView de página que Readium crea en el lector lleva ahora una frontera además de la configuración propia de Readium: sin acceso al sistema de archivos ni a proveedores de contenido y con los dos interruptores de origen cruzado para file URL apagados, mientras JavaScript sigue activado para Readium (hoja de ruta D6); la revisión de las fronteras del WebView, del contenedor y de los componentes queda registrada en `docs/dev/security-boundaries.md` (hoja de ruta P7.2)
- `Corrección` Si el proceso del lector muere por una excepción no capturada, la posición de lectura actual se escribe primero en disco, de forma sincrónica, antes de que actúe el manejo de fallos del propio sistema; el plugin no escribe registros ni planta ningún árbol de Timber, así que ningún título, ruta o texto de libro llega jamás a logcat (hoja de ruta P7.7)
- `Corrección` Elegir una colección TrueType u OpenType (`.ttc` / `.otc`) como fuente de lectura ahora informa de que las colecciones de fuentes no son compatibles, en lugar de decir que el archivo no es una fuente; encontrado al ejecutar la matriz de compatibilidad dispositivo x escenario registrada en `docs/dev/compatibility-matrix.md` (hoja de ruta P7.3)
- `Corrección` Accesibilidad: los cuatro deslizadores del panel de preferencias de lectura (tamaño del texto, márgenes de página, altura de línea, espaciado de párrafos) llevan ahora etiquetas que un lector de pantalla puede pronunciar, y la barra de herramientas del lector crece con los tamaños de fuente grandes del sistema en lugar de recortar el subtítulo del capítulo; lo respalda una auditoría de instrumentation sobre etiquetas, objetivos táctiles de 48 dp, escala de fuente 1.3x, modo nocturno, RTL forzado, paginación con teclado y orientación horizontal (hoja de ruta P7.6)
- `Corrección` La lectura en voz alta ya no espera indefinidamente a un motor de voz que nunca termina de inicializarse (el Google TTS sin datos de voz del emulador API 24 hace exactamente eso): tras 20 segundos el lector informa de que no hay ningún motor utilizable y vuelve al reposo, y una sesión que llegue más tarde se cierra (hoja de ruta P7.3)
- `Mejora` Tamaño del APK de release: el reproductor DiViNa que Readium incluye en los assets del navegador (427 KB, nunca usado por un lector EPUB) queda fuera de los assets combinados y la regla keep general del paquete del plugin desaparece, de modo que R8 también reduce las clases propias del plugin; el APK de release pasa de 3,922,786 B tras P5 a 3,328,220 B (hoja de ruta P7.5, detalles en `docs/dev/release-size.md`)
- `Dependencia` Se añade Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
- `Dependencia` Se añade `androidx.media3:media3-session` 1.11.0 (ya incluido por `readium-navigator-media-tts`; declarado directamente para el servicio en primer plano de lectura en voz alta)
- `Dependencia` Se añade `org.jsoup:jsoup` 1.23.2 (ya incluido por `readium-shared`; declarado directamente para la extracción del texto de capítulos del servicio EPUB)

##### Para consultar más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Compilación Release:

```powershell
.\gradlew.bat :app:assembleRelease
```

Los parámetros de compilación provienen de `version.properties`. El SDK mínimo actual es 24 y el SDK de destino es 37.

******

### Localización y generación de documentos

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml` localiza los metadatos del complemento y la interfaz del lector, mientras que `plugin_instruction.md` proporciona las instrucciones visibles en el anfitrión. Para el README y el changelog, edite siempre las fuentes JSON bajo `.readme/` y `.changelog/` y ejecute `py .python/generate_markdown.py` para regenerarlo todo; los archivos generados nunca se editan a mano. Ejecute `py .python/generate_markdown.py --check` para comprobar que fuentes y archivos generados están sincronizados.

******

### Licencia y avisos de terceros

******

El complemento se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/LICENSE). El Readium Kotlin Toolkit (BSD 3-Clause), AndroidX Media3 y Jsoup, las bibliotecas de contrato de AutoJs6 y los demás componentes incluidos en el APK se listan con sus versiones, sumas de comprobación y licencias en los [avisos de terceros](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md).

******

### Enlaces

******

- Documentación de AutoJs6: https://docs.autojs6.com
- Referencia de la API de scripts `epub`: https://docs.autojs6.com/#/epub
- Especificación EPUB 3.3: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit
- Avisos de terceros: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/THIRD_PARTY_NOTICES.md
- Alineación de páginas de 16 KB y verificación de la compilación: https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md
