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

> Etapa actual (compilación de desarrollo 1.0.0): el lector abre el libro con la configuración predeterminada de Readium, ofrece un índice, recuerda la posición de lectura de cada libro, proporciona modo de desplazamiento, zonas de toque, teclas de volumen y modo inmersivo, y cuenta con un panel de preferencias para el tamaño del texto, la fuente, los espaciados, la alineación, las columnas y los temas, que puede seguir el modo nocturno del host, e importa sus propias fuentes TTF u OTF. Los marcadores, la búsqueda de texto completo, la lectura en voz alta, el diseño fijo, la entrada independiente y la API de scripts `epub` están planificados en ROADMAP.md y aún no están disponibles.

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
- Enlaces externos: al tocar un enlace `http` o `https` se muestra la dirección completa y el navegador del sistema solo se abre tras confirmar.
- Integración con el anfitrión: los menús y diálogos siguen el idioma y el modo oscuro de AutoJs6; el sobre de Explorer Action se valida estrictamente antes de abrir cualquier contenido.
- Multilingüe: interfaz, instrucciones, README y changelog disponibles en 10 idiomas.

******

### Cómo se usa

******

1. Descargue el APK más reciente del complemento desde la página [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Readium-EPUB-Reader/releases) e instálelo en el dispositivo.
2. Abra el centro de complementos de AutoJs6 y active el complemento `Readium EPUB Reader`.
3. En el administrador de archivos de AutoJs6, toque un archivo `.epub`, o abra su menú (más acciones) y elija `Leer EPUB`.
4. Use el botón de índice de la barra de herramientas para saltar entre capítulos y el botón de preferencias para ajustar el texto y el tema; toque el tercio izquierdo o derecho de la página o pulse las teclas de volumen para pasar páginas, y toque el centro para ocultar o mostrar la barra de herramientas; pulse Atrás para cerrar el lector, la posición se recuerda.

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

#### ¿Cómo se recuerda mi posición de lectura?

La última posición de cada libro se guarda en el almacenamiento privado del complemento bajo una huella del contenido del archivo, nunca bajo su ruta, así que reabrir el mismo libro continúa donde lo dejó. Elija `Empezar desde el principio` en el menú para borrarla.

#### ¿Puedo cambiar la fuente, el tamaño del texto o el tema?

Sí. Abra el panel de preferencias desde la barra de herramientas para ajustar el tamaño del texto, la fuente (predeterminada del editor, con serifa, sin serifa, monoespaciada o las fuentes de accesibilidad incluidas con Readium), el interlineado, los márgenes, los espaciados, la alineación, las columnas y el tema (claro, sepia, oscuro o seguir al host). Toque `Importar fuente` en el panel para añadir sus propios archivos TTF u OTF; se guardan de forma privada en el complemento y se eliminan desde `Administrar fuentes`.

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
- Los datos de lectura permanecen locales: las posiciones se indexan por una huella del contenido y no se escribe ninguna ruta ni nombre de archivo en el almacenamiento.

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
- `Función` Un botón principal `Leer EPUB` y una acción de menú para archivos `.epub` en el administrador de archivos de AutoJs6 (ID de complemento `readium-epub-reader`, Explorer Action v2); los archivos con extensión `.epub` que el host informa como `application/zip` también se aceptan
- `Función` Base del lector: los libros EPUB 2 y EPUB 3 se representan con el navegador de Readium, con índice y enlaces externos confirmados
- `Función` Memoria de la posición de lectura: la última posición de cada libro se guarda bajo la huella de su contenido (clave rápida al abrir, luego SHA-256 del archivo completo) y se restaura en la siguiente apertura; `Empezar desde el principio` la borra
- `Función` Interfaz del lector: título del libro y capítulo actual en la barra de herramientas, barra de progreso con posición sintética y porcentaje, modo inmersivo con un toque en el centro, zonas de toque y teclas de volumen para pasar páginas, y conmutador de modo de desplazamiento
- `Función` Panel de preferencias de lectura: el tamaño del texto, la fuente, el interlineado, los márgenes, el espaciado de párrafos, la alineación, los guiones, los estilos del editor, el número de columnas y el diseño paginado o de desplazamiento se aplican de inmediato y se recuerdan para todos los libros; temas claro, sepia y oscuro más `Seguir al host`, con la barra de herramientas y las barras del sistema recoloreadas a juego
- `Función` Importación de fuentes: los archivos TTF y OTF elegidos con el selector de documentos del sistema se validan (firma SFNT, tabla `name`, 20 MB por archivo, 10 fuentes), se guardan de forma privada en `files/fonts/<sha256>` y se sirven al navegador de Readium como declaraciones `@font-face`; las fuentes importadas aparecen en el panel de preferencias junto a las integradas y pueden eliminarse allí
- `Función` Los libros se leen en su lugar a través del descriptor de archivo concedido, con lecturas posicionales; nada se copia ni se extrae al almacenamiento
- `Función` Interfaz, instrucciones, README y changelog en 10 idiomas
- `Corrección` Advertencias de lectura de SDK XML v4 con AGP 9.1 y comprobaciones de alineación nativa de APK activadas por error al ensamblar pruebas unitarias JVM, mediante los plugins de compilación compartidos 1.8.3
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
