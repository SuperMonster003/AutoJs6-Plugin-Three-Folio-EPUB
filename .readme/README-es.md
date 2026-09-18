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

> Etapa actual (compilación de desarrollo 1.0.0): el lector abre el libro con la configuración predeterminada de Readium y ofrece un índice. La posición de lectura, los marcadores, las preferencias, la búsqueda de texto completo, la lectura en voz alta, el diseño fijo, la importación de fuentes, la entrada independiente desde el lanzador y la API de scripts `epub` están planificados en ROADMAP.md y todavía no están disponibles.

******

### Funciones destacadas

******

- Motor Readium: los libros EPUB 2 (NCX) y EPUB 3 (NAV) se representan con el navegador de Readium y Readium CSS, incluidos enlaces internos, notas al pie e imágenes.
- Sin copias: el contenedor EPUB se lee en su lugar mediante un descriptor de solo lectura con lecturas posicionales, así que incluso los libros grandes se abren sin archivo de caché.
- Índice: salte a cualquier capítulo desde la barra de herramientas; las entradas anidadas conservan su nivel.
- Enlaces externos: al tocar un enlace `http` o `https` se muestra la dirección completa y el navegador del sistema solo se abre tras confirmar.
- Integración con el anfitrión: los menús y diálogos siguen el idioma y el modo oscuro de AutoJs6; el sobre de Explorer Action se valida estrictamente antes de abrir cualquier contenido.
- Multilingüe: interfaz, instrucciones, README y changelog disponibles en 10 idiomas.

******

### Cómo se usa

******

1. Descargue el APK más reciente del complemento desde la página [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) e instálelo en el dispositivo.
2. Abra el centro de complementos de AutoJs6 y active el complemento `Readium EPUB Reader`.
3. En el administrador de archivos de AutoJs6, toque un archivo `.epub`, o abra su menú (más acciones) y elija `Leer EPUB`.
4. Use el botón de índice de la barra de herramientas para cambiar de capítulo; pulse Atrás para cerrar el lector.

> Si el complemento no aparece en el centro de complementos, actualice primero AutoJs6 a una versión reciente (compilación interna 5269 o posterior). Explorer Action v2 admite el botón principal y el menú contextual para un archivo, con permisos temporales de lectura del documento y su carpeta.

******

### Formatos compatibles

******

El complemento reconoce la siguiente extensión, además de archivos sin extensión marcados explícitamente como `application/epub+zip` por el anfitrión:

```text
epub
```

Solo se admite EPUB: libros reajustables y de diseño fijo en EPUB 2 o EPUB 3. Los archivos de cómic (CBZ), los audiolibros, los PDF y los libros protegidos con LCP quedan fuera del alcance; un libro marcado como cifrado con LCP se notifica como ilegible en lugar de mostrar contenido corrupto.

******

### Preguntas frecuentes

******

#### ¿Por qué todavía no se recuerda mi posición de lectura?

La memoria de posición y los marcadores pertenecen al siguiente hito de ROADMAP.md. La compilación actual siempre abre el libro por el principio.

#### ¿Puedo cambiar la fuente, el tamaño del texto o el tema?

Todavía no. Las preferencias de lectura (fuente, tamaño, interlineado, márgenes, temas, modo paginado o de desplazamiento) llegan con el hito de preferencias; la compilación actual usa los valores predeterminados de Readium.

#### ¿Este complemento sube mis libros a algún sitio?

No. El complemento no tiene servidor propio. La red solo se usa cuando el propio libro hace referencia a recursos remotos y para la comprobación manual de actualizaciones prevista en la página de ajustes independiente.

******

### Permisos y seguridad

******

El complemento mantiene el comportamiento predeterminado de Readium para el contenido del libro: los scripts y recursos remotos del libro no se eliminan ni se bloquean, incluidos los recursos `http://` sin cifrar. Abra solo libros de confianza.

- Mínimo privilegio: el complemento solo recibe el permiso temporal de lectura del content URI concedido por el anfitrión, nunca ve rutas del sistema de archivos y nunca escribe el libro en el almacenamiento.
- Sobre estricto: la solicitud de Explorer Action debe llevar exactamente un objetivo EPUB, su carpeta, una versión de protocolo coincidente, una compilación del anfitrión compatible y ambos permisos de lectura; todo lo demás se rechaza antes de abrir el archivo.
- Análisis acotado: un contenedor malformado (no es ZIP, falta `container.xml`, falta el documento de paquete, salto de ruta en el manifest) termina con un mensaje de error en lugar de un bloqueo.
- Los enlaces externos se muestran completos y se abren en el navegador del sistema solo tras confirmar; se rechazan los esquemas distintos de `http` y `https`.

El manifiesto solo solicita el permiso de red y el permiso de complemento de AutoJs6. AndroidX añade además un permiso de firma limitado al paquete que protege los receptores dinámicos no exportados; no concede acceso a los datos del dispositivo. No se solicitan permisos de almacenamiento, multimedia, cámara, ubicación, accesibilidad ni superposición.

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

Las capacidades previstas y su estado se siguen en ROADMAP.md como una lista marcable, organizada por hitos con criterios de aceptación: memoria de posición y marcadores, preferencias e importación de fuentes, búsqueda de texto completo, lectura en voz alta, diseño fijo, entrada de aplicación independiente, contrato del anfitrión y API de scripts `epub`. Los elementos sin marcar describen planes, no capacidades entregadas. Los comentarios a través de Issues son bienvenidos.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/ROADMAP.md)

******

### Historial de versiones

******

#### v1.0.0

_2026/09/19_

- `Aviso` Compilación de desarrollo: las fases P0 de la hoja de ruta (esqueleto, validación de Readium, muestras de prueba) están en curso; la primera versión pública llega con la fase P8
- `Función` Un botón principal `Leer EPUB` y una acción de menú para archivos `.epub` en el administrador de archivos de AutoJs6 (ID de complemento `readium-epub-reader`, Explorer Action v2)
- `Función` Base del lector: los libros EPUB 2 y EPUB 3 se representan con el navegador de Readium, con índice y enlaces externos confirmados
- `Función` Los libros se leen en su lugar a través del descriptor de archivo concedido, con lecturas posicionales; nada se copia ni se extrae al almacenamiento
- `Función` Interfaz, instrucciones, README y changelog en 10 idiomas
- `Dependencia` Se añade Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)

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

### Enlaces

******

- Documentación de AutoJs6: https://docs.autojs6.com
- Especificación EPUB 3.3: https://www.w3.org/TR/epub-33/
- Readium Kotlin Toolkit: https://github.com/readium/kotlin-toolkit


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/blob/master/docs/16kb.md)
