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
* `Función` Panel de preferencias de lectura: el tamaño del texto, la fuente, el interlineado, los márgenes, el espaciado de párrafos, la alineación, los guiones, los estilos del editor, el número de columnas y el diseño paginado o de desplazamiento se aplican de inmediato y se recuerdan para todos los libros; temas claro, sepia y oscuro más `Seguir al host`, con la barra de herramientas y las barras del sistema recoloreadas a juego
* `Función` Importación de fuentes: los archivos TTF y OTF elegidos con el selector de documentos del sistema se validan (firma SFNT, tabla `name`, 20 MB por archivo, 10 fuentes), se guardan de forma privada en `files/fonts/<sha256>` y se sirven al navegador de Readium como declaraciones `@font-face`; las fuentes importadas aparecen en el panel de preferencias junto a las integradas y pueden eliminarse allí
* `Función` Libros CJK verticales y de derecha a izquierda: la progresión de lectura sigue a la publicación (las zonas de toque se invierten en los libros de derecha a izquierda), los libros japoneses / chinos con progresión de página de derecha a izquierda se muestran en vertical mediante Readium CSS, una preferencia `Dirección del texto` fuerza el texto horizontal o vertical, y la dirección del diseño de la interfaz es independiente del libro
* `Función` Libros de diseño fijo: `Página x de N` en la barra de progreso, una preferencia `Doble página` (auto = dos páginas en horizontal, una página, dos páginas) con las preferencias de texto ocultas, y el zoom de pellizco de Readium
* `Función` Búsqueda de texto completo: la entrada `Buscar` de la barra de herramientas abre un panel de resultados que carga 50 coincidencias a la vez (hasta 500), agrupadas por capítulo con su contexto; tocar una coincidencia salta a ella y la resalta en la página, con anterior / siguiente en una barra sobre el progreso
* `Función` Marcadores: un icono de la barra de herramientas añade o quita un marcador de la página actual (con el capítulo y un extracto del texto), y un panel `Marcadores` los lista del más reciente al más antiguo con saltar, eliminar y borrar todo; se guardan por libro (hasta 500) junto a la posición de lectura
* `Función` Controles de lectura: las zonas de toque pueden desactivarse o fijarse en izquierda / derecha o arriba / abajo, los teclados físicos pasan página con las flechas, las teclas de página y el espacio, y el texto seleccionado ofrece copiar, compartir, búsqueda web y las aplicaciones de procesamiento de texto del sistema
* `Función` Los libros se leen en su lugar a través del descriptor de archivo concedido, con lecturas posicionales; nada se copia ni se extrae al almacenamiento
* `Función` Interfaz, instrucciones, README y changelog en 10 idiomas
* `Corrección` Advertencias de lectura de SDK XML v4 con AGP 9.1 y comprobaciones de alineación nativa de APK activadas por error al ensamblar pruebas unitarias JVM, mediante los plugins de compilación compartidos 1.8.3
* `Corrección` Un fallo al escribir el progreso (carpeta del libro eliminada, almacenamiento no escribible) ya no bloquea el lector; ese registro se pierde y la lectura continúa
* `Corrección` El lector ya no muere junto con el anfitrión cuando AutoJs6 se detiene o se actualiza mientras se lee su proveedor de ajustes; esa lectura simplemente falla y no se aplican el idioma / modo nocturno del anfitrión
* `Dependencia` Se añade Readium Kotlin Toolkit 3.4.0 (`readium-shared`, `readium-streamer`, `readium-navigator`, `readium-navigator-media-tts`)
