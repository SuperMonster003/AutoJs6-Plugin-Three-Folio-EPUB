# -*- coding: utf-8 -*-
"""Generate the EPUB test fixtures under docs/fixtures (roadmap P0.3 / D22).

Every fixture is synthesized from text written for this repository, so no third-party license is
involved. The output is deterministic (fixed ZIP timestamps, sorted entries), and
docs/fixtures/SHA256SUMS.txt records the digests that docs/fixtures/README.md refers to.

Fixtures:
  minimal-epub2.epub                 EPUB 2 (NCX), 3 chapters
  minimal-epub3.epub                 EPUB 3 (NAV), 3 chapters, internal links, footnote, PNG image
  malformed-not-a-zip.epub           plain text, not a ZIP container
  malformed-missing-container.epub   valid ZIP without META-INF/container.xml
  malformed-missing-opf.epub         container.xml points to an OPF that does not exist
  malformed-bad-ncx.epub             EPUB 2 whose NCX is not well-formed XML
  malformed-path-traversal.epub      manifest href that escapes the container ("../")
  malformed-many-entries.epub        2000 tiny entries in the manifest and spine
  malformed-high-ratio.epub          one 64 MiB zero-filled resource (deflates to a few KiB)
  malformed-encrypted-lcp.epub       META-INF/encryption.xml declaring an LCP-protected resource
  malformed-empty-zip.epub           a valid ZIP archive with no entries at all (P7 hostile matrix)
  malformed-missing-mimetype.epub    EPUB 3 container without the mimetype entry
  malformed-bad-opf.epub             package document cut in the middle of a tag (not well-formed XML)
  malformed-xxe.epub                 OPF and chapter 1 declare external entities (file:// canary, loopback http://) that must never resolve
  malformed-traversal-encoded.epub   percent-encoded, absolute, file:// and backslash hrefs; ZIP entry names that escape the container
  malformed-long-names.epub          one spine resource whose name is 3000 characters long (above the contract's 2048-character href ceiling)
  malformed-duplicate-entries.epub   chapter 1 stored twice in the ZIP with different text; manifest item and spine itemref repeated
  malformed-lcp-license-only.epub    META-INF/license.lcpl without encryption.xml (LCP marker)
  malformed-encrypted-adept.epub     META-INF/encryption.xml with an Adobe ADEPT resource key (ADEPT marker)
  vertical-ja.epub                   EPUB 3, Japanese, page-progression-direction rtl, publisher vertical-rl CSS
  vertical-zh.epub                   EPUB 3, traditional Chinese, page-progression-direction rtl, no writing mode
  rtl-ar.epub                        EPUB 3, Arabic, dir="rtl", page-progression-direction rtl
  fixed-layout.epub                  EPUB 3 pre-paginated, 6 plates of 600x800 CSS px, spread properties

Usage: py .python/generate_fixtures.py          (the committed set above)
       py .python/generate_fixtures.py --perf   (roadmap P7 performance samples into build/perf-fixtures, not committed:
                                                perf-images-20mb.epub, perf-images-200mb.epub, perf-chapters-5000.epub)
"""

from __future__ import annotations

import hashlib
import io
import random
import struct
import sys
import warnings
import zipfile
import zlib
from pathlib import Path
from typing import NamedTuple

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "fixtures"
PERF_OUT = ROOT / "build" / "perf-fixtures"
FIXED_TIME = (2026, 9, 19, 0, 0, 0)

CONTAINER_XML = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="{opf}" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
"""

CHAPTER_TEXTS = {
    1: [
        "The lighthouse keeper counted the ships the way other people counted sheep.",
        "Each hull that slipped past the reef was a small victory against the fog.",
        "Tonight the fog had teeth, and the lamp room smelled of hot brass and salt.",
    ],
    2: [
        "By morning the wind had turned, dragging the clouds inland like wet wool.",
        "She climbed the spiral stair twice, once for the log and once for the view.",
        "Nothing on the horizon, which was the best kind of news a keeper could write down.",
    ],
    3: [
        "The relief boat came on the third day, late and low in the water.",
        "They traded bread for stories and left before the tide could change its mind.",
        "When the door shut, the lighthouse went back to its only job: being seen.",
    ],
}

STYLE_CSS = "body { font-family: serif; line-height: 1.5; margin: 1em; }\nh1 { font-size: 1.4em; }\n"

# Japanese vertical books usually carry the writing mode in their own CSS (EPUB prefix and the
# standard property); the Chinese sample deliberately has none, so only the reader's metadata path
# (CJK language plus a right-to-left page progression) turns it vertical.
VERTICAL_CSS = (
    "html { -epub-writing-mode: vertical-rl; writing-mode: vertical-rl; }\n"
    "body { font-family: serif; line-height: 1.8; margin: 1em; }\nh1 { font-size: 1.4em; }\n"
)

# The same lighthouse story, retold for this repository in Japanese, traditional Chinese and Arabic.
JAPANESE_TEXTS = {
    1: [
        "灯台守は、ほかの人が羊を数えるように船を数えた。",
        "岩礁をすり抜けていく船体のひとつひとつが、霧に対する小さな勝利だった。",
        "今夜の霧には牙があり、灯室は熱い真鍮と潮の匂いがした。",
    ],
    2: [
        "朝には風向きが変わり、雲を濡れた羊毛のように内陸へ引きずっていった。",
        "彼女は螺旋階段を二度のぼった。一度は日誌のため、もう一度は眺めのために。",
        "水平線には何もない。それは灯台守が書き留められる、いちばん良い知らせだった。",
    ],
    3: [
        "交代の船は三日目に来た。遅れて、喫水も深く。",
        "彼らはパンと物語を交換し、潮が気を変える前に去っていった。",
        "扉が閉まると、灯台はただひとつの仕事に戻った。「見られること」だ。",
    ],
}

CHINESE_TEXTS = {
    1: [
        "守塔人數著船，就像別人數著羊。",
        "每一艘掠過礁石的船身，都是對霧氣的一次小小勝利。",
        "今夜的霧長了牙，燈室裡滿是熱黃銅和鹽的氣味。",
    ],
    2: [
        "到了早晨風向轉了，把雲像濕羊毛一樣拖向內陸。",
        "她爬了兩次螺旋梯，一次為了日誌，一次為了風景。",
        "地平線上什麼都沒有，這是守塔人能寫下的最好消息。",
    ],
    3: [
        "接替的船第三天才來，來得晚，吃水也深。",
        "他們用麵包換故事，趕在潮水改變主意之前離開了。",
        "門一關上，燈塔就回到它唯一的工作：被看見。",
    ],
}

ARABIC_TEXTS = {
    1: [
        "كان حارس المنارة يعد السفن كما يعد الآخرون الخراف.",
        "كل هيكل ينزلق عبر الشعاب كان انتصارا صغيرا على الضباب.",
        "الليلة كان للضباب أنياب، وغرفة المصباح تفوح برائحة النحاس الساخن والملح.",
    ],
    2: [
        "مع الصباح تحولت الريح، تجر الغيوم إلى الداخل كصوف مبلل.",
        "صعدت الدرج الحلزوني مرتين، مرة للسجل ومرة للمنظر.",
        "لا شيء في الأفق، وهذا أفضل خبر يمكن لحارس أن يدونه.",
    ],
    3: [
        "جاء قارب الإغاثة في اليوم الثالث، متأخرا وثقيلا في الماء.",
        "تبادلوا الخبز بالحكايات ورحلوا قبل أن يغير المد رأيه.",
        "حين أغلق الباب، عادت المنارة إلى عملها الوحيد: أن ترى.",
    ],
}


class Locale(NamedTuple):
    """Language-dependent parts of a sample; the default is the English lighthouse story."""

    lang: str = "en"
    chapter: str = "Chapter {n}"
    contents: str = "Contents"
    start: str = "Start"
    texts: dict = CHAPTER_TEXTS
    css: str = STYLE_CSS
    dir: str | None = None          # html dir attribute
    progression: str | None = None  # spine page-progression-direction (EPUB 3)


ENGLISH = Locale()
JAPANESE = Locale("ja", "第{n}章", "目次", "本文", JAPANESE_TEXTS, VERTICAL_CSS, None, "rtl")
CHINESE_TRADITIONAL = Locale("zh-Hant", "第{n}章", "目錄", "正文", CHINESE_TEXTS, STYLE_CSS, None, "rtl")
ARABIC = Locale("ar", "الفصل {n}", "المحتويات", "البداية", ARABIC_TEXTS, STYLE_CSS, "rtl", "rtl")


def xhtml(title: str, body: str, epub3: bool, locale: Locale = ENGLISH) -> str:
    epub_ns = ' xmlns:epub="http://www.idpf.org/2007/ops"' if epub3 else ""
    direction = f' dir="{locale.dir}"' if locale.dir else ""
    doctype = "<!DOCTYPE html>" if epub3 else (
        '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">'
    )
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        f"{doctype}\n"
        f'<html xmlns="http://www.w3.org/1999/xhtml"{epub_ns} xml:lang="{locale.lang}"{direction}>\n'
        f"<head><title>{title}</title><link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\"/></head>\n"
        f"<body>\n{body}</body>\n</html>\n"
    )


def chapter_body(number: int, epub3: bool, locale: Locale = ENGLISH) -> str:
    paragraphs = "\n".join(f"<p>{text}</p>" for text in locale.texts[number])
    extra = ""
    english = epub3 and locale.lang == "en"
    if english and number == 1:
        extra = (
            '<p>A note about the reef<a href="#note-1" epub:type="noteref" id="ref-1">1</a> '
            'and a link to <a href="chapter3.xhtml#landing">the last chapter</a>.</p>\n'
            '<p><img src="images/beacon.png" alt="A small beacon glyph"/></p>\n'
            '<aside epub:type="footnote" id="note-1"><p>The reef is named after nobody in particular.</p></aside>\n'
        )
    if english and number == 3:
        extra = '<p id="landing">You have arrived at the landing anchor.</p>\n'
    return f'<h1 id="chapter-{number}">{locale.chapter.format(n=number)}</h1>\n{paragraphs}\n{extra}'



def opf(title: str, epub3: bool, manifest_extra: list[tuple[str, str, str, str]] = (), spine_extra: list[str] = (),
        locale: Locale = ENGLISH, identifier: str | None = None) -> str:
    version = "3.0" if epub3 else "2.0"
    items = [
        ("chapter1", "chapter1.xhtml", "application/xhtml+xml", ""),
        ("chapter2", "chapter2.xhtml", "application/xhtml+xml", ""),
        ("chapter3", "chapter3.xhtml", "application/xhtml+xml", ""),
        ("style", "style.css", "text/css", ""),
    ]
    if epub3:
        items.append(("nav", "nav.xhtml", "application/xhtml+xml", ' properties="nav"'))
        items.append(("beacon", "images/beacon.png", "image/png", ""))
    else:
        items.append(("ncx", "toc.ncx", "application/x-dtbncx+xml", ""))
    items.extend(manifest_extra)
    manifest = "\n".join(
        f'    <item id="{item_id}" href="{href}" media-type="{media}"{extra}/>'
        for item_id, href, media, extra in items
    )
    spine_ids = ["chapter1", "chapter2", "chapter3", *spine_extra]
    spine_attr = "" if epub3 else ' toc="ncx"'
    if locale.progression:
        spine_attr += f' page-progression-direction="{locale.progression}"'
    spine = "\n".join(f'    <itemref idref="{item_id}"/>' for item_id in spine_ids)
    modified = '\n    <meta property="dcterms:modified">2026-09-19T00:00:00Z</meta>' if epub3 else ""
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        f'<package xmlns="http://www.idpf.org/2007/opf" version="{version}" unique-identifier="uid">\n'
        '  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">\n'
        f"    <dc:identifier id=\"uid\">urn:uuid:autojs6-readium-fixture-{identifier or ('epub3' if epub3 else 'epub2')}</dc:identifier>\n"
        f"    <dc:title>{title}</dc:title>\n"
        f"    <dc:language>{locale.lang}</dc:language>\n"
        "    <dc:creator>AutoJs6 Readium EPUB Reader fixtures</dc:creator>"
        f"{modified}\n"
        "  </metadata>\n"
        f"  <manifest>\n{manifest}\n  </manifest>\n"
        f"  <spine{spine_attr}>\n{spine}\n  </spine>\n"
        "</package>\n"
    )


def ncx(title: str, well_formed: bool = True) -> str:
    points = "\n".join(
        f'    <navPoint id="np-{n}" playOrder="{n}"><navLabel><text>Chapter {n}</text></navLabel>'
        f'<content src="chapter{n}.xhtml"/></navPoint>'
        for n in (1, 2, 3)
    )
    closing = "</navMap>\n</ncx>\n" if well_formed else "</navMap>\n<ncx>\n"
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">\n'
        '  <head><meta name="dtb:uid" content="urn:uuid:autojs6-readium-fixture-epub2"/></head>\n'
        f"  <docTitle><text>{title}</text></docTitle>\n"
        f"  <navMap>\n{points}\n  {closing}"
    )


def nav_xhtml(locale: Locale = ENGLISH) -> str:
    note = '<ol><li><a href="chapter1.xhtml#ref-1">The reef note</a></li></ol>' if locale.lang == "en" else ""
    items = "\n".join(
        f'  <li><a href="chapter{n}.xhtml">{locale.chapter.format(n=n)}</a>{note if n == 1 else ""}</li>'
        for n in (1, 2, 3)
    )
    body = (
        f'<nav epub:type="toc" id="toc"><h1>{locale.contents}</h1><ol>\n{items}\n</ol></nav>\n'
        f'<nav epub:type="landmarks" hidden="hidden"><ol><li><a epub:type="bodymatter" href="chapter1.xhtml">{locale.start}</a></li></ol></nav>\n'
    )
    return xhtml(locale.contents, body, epub3=True, locale=locale)


def beacon_png() -> bytes:
    """A 16x16 RGBA PNG (teal square with a white dot), built without third-party libraries."""
    width = height = 16
    rows = []
    for y in range(height):
        row = bytearray([0])
        for x in range(width):
            center = abs(x - 7.5) < 2.5 and abs(y - 7.5) < 2.5
            row += bytes((255, 255, 255, 255)) if center else bytes((0x00, 0x69, 0x5C, 255))
        rows.append(bytes(row))
    raw = b"".join(rows)

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


# A six-plate picture book (roadmap P2.4): every page declares the same viewport, the spine carries
# the page-spread properties (cover centered, then left / right pairs) and the package asks for
# automatic spreads. Colours differ per plate so a screenshot shows which pages are on screen.
FXL_WIDTH, FXL_HEIGHT = 600, 800
FXL_PLATES = [
    ("Cover", "#00695c"),
    ("The reef", "#1565c0"),
    ("The lamp room", "#6a1b9a"),
    ("The stair", "#ad1457"),
    ("The relief boat", "#ef6c00"),
    ("Being seen", "#2e7d32"),
]
FXL_SPREADS = ["center", "left", "right", "left", "right", "left"]
FXL_TOC = [(1, "Cover"), (2, "The reef"), (4, "The stair"), (6, "Being seen")]
FXL_CSS = (
    f"html, body {{ margin: 0; padding: 0; width: {FXL_WIDTH}px; height: {FXL_HEIGHT}px; overflow: hidden; }}\n"
    "body { font-family: sans-serif; color: #ffffff; }\n"
    ".frame { position: absolute; left: 40px; top: 40px; width: 520px; height: 720px; box-sizing: border-box; border: 6px solid #ffffff; }\n"
    ".number { position: absolute; left: 0; right: 0; top: 140px; margin: 0; font-size: 240px; line-height: 1; font-weight: bold; text-align: center; }\n"
    ".caption { position: absolute; left: 0; right: 0; top: 440px; margin: 0; font-size: 36px; text-align: center; }\n"
    ".side { position: absolute; left: 0; right: 0; top: 520px; margin: 0; font-size: 22px; text-align: center; opacity: 0.8; }\n"
    ".beacon { position: absolute; left: 232px; top: 600px; width: 48px; height: 48px; }\n"
    + "".join(f"body.plate-{n} {{ background: {color}; }}\n" for n, (_, color) in enumerate(FXL_PLATES, 1))
)


def fxl_page(number: int) -> str:
    title = FXL_PLATES[number - 1][0]
    side = FXL_SPREADS[number - 1]
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE html>\n'
        '<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">\n'
        f'<head><title>Plate {number}</title><meta name="viewport" content="width={FXL_WIDTH}, height={FXL_HEIGHT}"/>'
        '<link rel="stylesheet" type="text/css" href="fxl.css"/></head>\n'
        f'<body class="plate-{number}"><div class="frame"><p class="number">{number}</p><p class="caption">{title}</p>'
        f'<p class="side">{side} page, {number} of {len(FXL_PLATES)}</p>'
        '<img class="beacon" src="images/beacon.png" alt=""/></div></body>\n</html>\n'
    )


def fxl_nav() -> str:
    items = "\n".join(f'  <li><a href="page{n}.xhtml">{title}</a></li>' for n, title in FXL_TOC)
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE html>\n'
        '<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">\n'
        '<head><title>Contents</title></head>\n<body>\n'
        f'<nav epub:type="toc" id="toc"><h1>Contents</h1><ol>\n{items}\n</ol></nav>\n'
        '<nav epub:type="landmarks" hidden="hidden"><ol>'
        '<li><a epub:type="cover" href="page1.xhtml">Cover</a></li>'
        '<li><a epub:type="bodymatter" href="page2.xhtml">Start</a></li></ol></nav>\n'
        "</body>\n</html>\n"
    )


def fxl_opf(title: str) -> str:
    items = "\n".join(
        f'    <item id="page{n}" href="page{n}.xhtml" media-type="application/xhtml+xml"/>'
        for n in range(1, len(FXL_PLATES) + 1)
    )
    spine = "\n".join(
        f'    <itemref idref="page{n}" properties="{"rendition:page-spread-center" if side == "center" else f"page-spread-{side}"}"/>'
        for n, side in enumerate(FXL_SPREADS, 1)
    )
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid" prefix="rendition: http://www.idpf.org/vocab/rendition/#">\n'
        '  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">\n'
        '    <dc:identifier id="uid">urn:uuid:autojs6-readium-fixture-fixed-layout</dc:identifier>\n'
        f"    <dc:title>{title}</dc:title>\n"
        "    <dc:language>en</dc:language>\n"
        "    <dc:creator>AutoJs6 Readium EPUB Reader fixtures</dc:creator>\n"
        '    <meta property="dcterms:modified">2026-09-19T00:00:00Z</meta>\n'
        '    <meta property="rendition:layout">pre-paginated</meta>\n'
        '    <meta property="rendition:orientation">auto</meta>\n'
        '    <meta property="rendition:spread">auto</meta>\n'
        "  </metadata>\n"
        f"  <manifest>\n{items}\n"
        '    <item id="css" href="fxl.css" media-type="text/css"/>\n'
        '    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>\n'
        '    <item id="beacon" href="images/beacon.png" media-type="image/png"/>\n'
        "  </manifest>\n"
        f"  <spine>\n{spine}\n  </spine>\n"
        "</package>\n"
    )


def fxl_entries(title: str) -> list[tuple[str, bytes]]:
    entries = [("META-INF/container.xml", CONTAINER_XML.format(opf="OEBPS/content.opf").encode())]
    entries.append(("OEBPS/content.opf", fxl_opf(title).encode()))
    for n in range(1, len(FXL_PLATES) + 1):
        entries.append((f"OEBPS/page{n}.xhtml", fxl_page(n).encode()))
    entries.append(("OEBPS/fxl.css", FXL_CSS.encode()))
    entries.append(("OEBPS/nav.xhtml", fxl_nav().encode()))
    entries.append(("OEBPS/images/beacon.png", beacon_png()))
    return entries


def build_zip(entries: list[tuple[str, bytes]], mimetype_first: bool = True) -> bytes:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        if mimetype_first:
            info = zipfile.ZipInfo("mimetype", FIXED_TIME)
            info.compress_type = zipfile.ZIP_STORED
            archive.writestr(info, b"application/epub+zip")
        for name, data in entries:
            info = zipfile.ZipInfo(name, FIXED_TIME)
            # Hostile names (backslashes, leading slashes, "..") stay verbatim on every OS, and a name
            # stored twice is a deliberate fixture, not a mistake to warn about.
            info.filename = name
            info.compress_type = zipfile.ZIP_DEFLATED
            with warnings.catch_warnings():
                warnings.simplefilter("ignore", UserWarning)
                archive.writestr(info, data)
    return buffer.getvalue()


def epub_entries(epub3: bool, title: str, *, ncx_well_formed: bool = True, opf_name: str = "OEBPS/content.opf",
                 manifest_extra=(), spine_extra=(), extra_files: list[tuple[str, bytes]] = (),
                 locale: Locale = ENGLISH, identifier: str | None = None) -> list[tuple[str, bytes]]:
    entries = [("META-INF/container.xml", CONTAINER_XML.format(opf=opf_name).encode())]
    entries.append((opf_name, opf(title, epub3, list(manifest_extra), list(spine_extra), locale, identifier).encode()))
    for n in (1, 2, 3):
        chapter = xhtml(locale.chapter.format(n=n), chapter_body(n, epub3, locale), epub3, locale)
        entries.append((f"OEBPS/chapter{n}.xhtml", chapter.encode()))
    entries.append(("OEBPS/style.css", locale.css.encode()))
    if epub3:
        entries.append(("OEBPS/nav.xhtml", nav_xhtml(locale).encode()))
        entries.append(("OEBPS/images/beacon.png", beacon_png()))
    else:
        entries.append(("OEBPS/toc.ncx", ncx(title, ncx_well_formed).encode()))
    entries.extend(extra_files)
    return entries


# The canary the XXE fixture points at: a file the instrumentation test writes under the plugin's own
# files directory; its content must never surface through the parsed book.
XXE_CANARY_PATH = "/data/data/io.github.supermonster003.autojs6.plugin.readium.epub.reader/files/xxe-canary.txt"


def xxe_doctype(root: str) -> str:
    return (
        f"<!DOCTYPE {root} [\n"
        f'  <!ENTITY canary SYSTEM "file://{XXE_CANARY_PATH}">\n'
        '  <!ENTITY loopback SYSTEM "http://127.0.0.1:9/xxe">\n'
        '  <!ENTITY internal "internal-entity-expanded">\n'
        "]>"
    )


def xxe_entries() -> list[tuple[str, bytes]]:
    """The package document and chapter 1 reference external entities; a resolver would leak the canary."""
    result = []
    for name, data in epub_entries(True, "XXE &canary;"):
        if name == "OEBPS/content.opf":
            text = data.decode("utf-8")
            text = text.replace("<package ", xxe_doctype("package") + "\n<package ", 1)
            text = text.replace("<dc:creator>", "<dc:creator>&internal; &loopback; ", 1)
            data = text.encode("utf-8")
        elif name == "OEBPS/chapter1.xhtml":
            text = data.decode("utf-8")
            text = text.replace("<!DOCTYPE html>", xxe_doctype("html"), 1)
            text = text.replace(
                "<body>\n",
                "<body>\n<p>canary: &canary;</p>\n<p>loopback: &loopback;</p>\n<p>internal: &internal;</p>\n",
                1,
            )
            data = text.encode("utf-8")
        result.append((name, data))
    return result


def duplicate_entries() -> list[tuple[str, bytes]]:
    """Chapter 1 is stored twice with different text; the manifest item and the spine itemref repeat too."""
    result = []
    for name, data in epub_entries(True, "Duplicate entries"):
        if name == "OEBPS/content.opf":
            text = data.decode("utf-8")
            item = '    <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>\n'
            text = text.replace(item, item * 2, 1)
            text = text.replace('    <itemref idref="chapter1"/>\n', '    <itemref idref="chapter1"/>\n' * 2, 1)
            data = text.encode("utf-8")
        result.append((name, data))
        if name == "OEBPS/chapter1.xhtml":
            second = xhtml("Duplicate chapter", "<h1>Duplicate chapter</h1>\n<p>DUPLICATE-SECOND-COPY</p>\n", True)
            result.append((name, second.encode("utf-8")))
    return result


def adept_encryption_xml() -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#" xmlns:ds="http://www.w3.org/2000/09/xmldsig#">\n'
        "  <enc:EncryptedData>\n"
        '    <enc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>\n'
        '    <ds:KeyInfo><resource xmlns="http://ns.adobe.com/adept">urn:uuid:autojs6-readium-fixture-adept</resource></ds:KeyInfo>\n'
        '    <enc:CipherData><enc:CipherReference URI="OEBPS/chapter1.xhtml"/></enc:CipherData>\n'
        "  </enc:EncryptedData>\n"
        "</encryption>\n"
    )


TRAVERSAL_HREFS = [
    ("esc-encoded", "%2e%2e/%2e%2e/escaped.txt"),
    ("esc-dotdot", "../../escaped.txt"),
    ("esc-absolute", "/etc/hosts"),
    ("esc-file-url", "file:///etc/hosts"),
    ("esc-backslash", "..\\..\\escaped.txt"),
]

LONG_NAME = "long/" + "x" * 3000 + ".xhtml"


def fixtures() -> dict[str, bytes]:
    result: dict[str, bytes] = {}
    result["minimal-epub2.epub"] = build_zip(epub_entries(False, "Minimal EPUB 2"))
    result["minimal-epub3.epub"] = build_zip(epub_entries(True, "Minimal EPUB 3"))
    result["vertical-ja.epub"] = build_zip(epub_entries(True, "灯台守 (縦書き)", locale=JAPANESE, identifier="vertical-ja"))
    result["vertical-zh.epub"] = build_zip(
        epub_entries(True, "守塔人 (直排)", locale=CHINESE_TRADITIONAL, identifier="vertical-zh")
    )
    result["rtl-ar.epub"] = build_zip(epub_entries(True, "حارس المنارة", locale=ARABIC, identifier="rtl-ar"))
    result["fixed-layout.epub"] = build_zip(fxl_entries("The Lighthouse Plates"))
    result["malformed-not-a-zip.epub"] = b"This file pretends to be an EPUB but is plain text.\n"
    result["malformed-missing-container.epub"] = build_zip(
        [entry for entry in epub_entries(True, "Missing container") if entry[0] != "META-INF/container.xml"]
    )
    result["malformed-missing-opf.epub"] = build_zip(
        [entry for entry in epub_entries(True, "Missing OPF") if not entry[0].endswith("content.opf")]
    )
    result["malformed-bad-ncx.epub"] = build_zip(epub_entries(False, "Bad NCX", ncx_well_formed=False))
    result["malformed-path-traversal.epub"] = build_zip(
        epub_entries(
            True,
            "Path traversal",
            manifest_extra=[("escape", "../../../etc/passwd", "text/plain", "")],
            extra_files=[("../escaped.txt", b"should never be extracted\n")],
        )
    )
    many = [(f"filler{i}", f"filler/{i}.xhtml", "application/xhtml+xml", "") for i in range(2000)]
    filler_files = [
        (f"OEBPS/filler/{i}.xhtml", xhtml(f"Filler {i}", f"<p>Filler {i}</p>\n", True).encode())
        for i in range(2000)
    ]
    result["malformed-many-entries.epub"] = build_zip(
        epub_entries(True, "Many entries", manifest_extra=many, spine_extra=[f"filler{i}" for i in range(2000)],
                     extra_files=filler_files)
    )
    result["malformed-high-ratio.epub"] = build_zip(
        epub_entries(
            True,
            "High ratio",
            manifest_extra=[("blob", "blob.bin", "application/octet-stream", "")],
            extra_files=[("OEBPS/blob.bin", bytes(64 * 1024 * 1024))],
        )
    )
    encryption_xml = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<encryption xmlns="urn:oasis:names:tc:opendocument:xmlns:container" xmlns:enc="http://www.w3.org/2001/04/xmlenc#" xmlns:ds="http://www.w3.org/2000/09/xmldsig#">\n'
        '  <enc:EncryptedData Id="ed1">\n'
        '    <enc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes256-cbc"/>\n'
        '    <ds:KeyInfo><ds:RetrievalMethod URI="license.lcpl#/encryption/content_key" Type="http://readium.org/2014/01/lcp#EncryptedContentKey"/></ds:KeyInfo>\n'
        '    <enc:CipherData><enc:CipherReference URI="OEBPS/chapter1.xhtml"/></enc:CipherData>\n'
        "  </enc:EncryptedData>\n"
        "</encryption>\n"
    )
    license_json = (
        '{"id":"fixture-license","issued":"2026-09-19T00:00:00Z","provider":"https://example.invalid",'
        '"encryption":{"profile":"http://readium.org/lcp/basic-profile","content_key":{"algorithm":"http://www.w3.org/2001/04/xmlenc#aes256-cbc","encrypted_value":"AAAA"},'
        '"user_key":{"algorithm":"http://www.w3.org/2001/04/xmlenc#sha256","text_hint":"fixture","key_check":"AAAA"}},'
        '"links":[{"rel":"hint","href":"https://example.invalid/hint"},{"rel":"publication","href":"https://example.invalid/book.epub","type":"application/epub+zip"}],'
        '"user":{"id":"fixture"},"rights":{},"signature":{"algorithm":"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256","certificate":"AAAA","value":"AAAA"}}'
    )
    result["malformed-encrypted-lcp.epub"] = build_zip(
        epub_entries(
            True,
            "LCP marker",
            extra_files=[("META-INF/encryption.xml", encryption_xml.encode()), ("META-INF/license.lcpl", license_json.encode())],
        )
    )
    # ---- Roadmap P7.1 hostile inputs (the P0.3 set above plus these) ----
    result["malformed-empty-zip.epub"] = build_zip([], mimetype_first=False)
    result["malformed-missing-mimetype.epub"] = build_zip(epub_entries(True, "Missing mimetype"), mimetype_first=False)
    truncated_opf = opf("Bad OPF", True).split("<manifest>")[0] + '<manifest>\n    <item id="chapter1" href="chapter1.xhtml"\n'
    result["malformed-bad-opf.epub"] = build_zip(
        [(name, truncated_opf.encode() if name == "OEBPS/content.opf" else data) for name, data in epub_entries(True, "Bad OPF")]
    )
    result["malformed-xxe.epub"] = build_zip(xxe_entries())
    result["malformed-traversal-encoded.epub"] = build_zip(
        epub_entries(
            True,
            "Encoded traversal",
            manifest_extra=[(item_id, href, "application/xhtml+xml", "") for item_id, href in TRAVERSAL_HREFS],
            spine_extra=[item_id for item_id, _ in TRAVERSAL_HREFS],
            extra_files=[
                ("/abs/escaped.txt", b"absolute zip entry name\n"),
                ("..\\escaped-win.txt", b"backslash traversal entry name\n"),
                ("OEBPS/../escaped-dot.txt", b"dot-dot inside an entry name\n"),
                ("escaped.txt", b"a file at the container root\n"),
            ],
        )
    )
    result["malformed-long-names.epub"] = build_zip(
        epub_entries(
            True,
            "Long names",
            manifest_extra=[("long", LONG_NAME, "application/xhtml+xml", "")],
            spine_extra=["long"],
            extra_files=[("OEBPS/" + LONG_NAME, xhtml("Long name", "<p>A resource with a 3000-character name.</p>\n", True).encode())],
        )
    )
    result["malformed-duplicate-entries.epub"] = build_zip(duplicate_entries())
    result["malformed-lcp-license-only.epub"] = build_zip(
        epub_entries(True, "LCP license only", extra_files=[("META-INF/license.lcpl", license_json.encode())])
    )
    result["malformed-encrypted-adept.epub"] = build_zip(
        epub_entries(True, "ADEPT marker", extra_files=[("META-INF/encryption.xml", adept_encryption_xml().encode())])
    )
    return result


# ---- Roadmap P7 performance samples (generated on demand, never committed) ----

PERF_IMAGE_SIDE = 256  # 256x256 RGB noise deflates to nothing, so every image costs its raw ~197 KB


def noise_png(seed: int, side: int = PERF_IMAGE_SIDE) -> bytes:
    """An incompressible RGB PNG: random pixels stored through zlib level 0 (still a valid image)."""
    rng = random.Random(seed)
    raw = b"".join(b"\x00" + rng.randbytes(side * 3) for _ in range(side))

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", side, side, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 0))
        + chunk(b"IEND", b"")
    )


def perf_paragraphs(chapter: int, count: int = 12) -> str:
    rng = random.Random(chapter)
    words = ["lighthouse", "keeper", "reef", "fog", "lamp", "brass", "salt", "tide", "stair", "log", "horizon", "boat",
             "bread", "stories", "door", "wind", "cloud", "wool", "victory", "hull", "ship", "morning", "relief", "seen"]
    paragraphs = []
    for p in range(count):
        sentence = " ".join(rng.choice(words) for _ in range(18)).capitalize() + "."
        paragraphs.append(f"<p>Chapter {chapter}, paragraph {p + 1}: {sentence} {sentence}</p>")
    return "\n".join(paragraphs)


def perf_book(title: str, identifier: str, chapters: int, image_bytes: int = 0) -> bytes:
    """`chapters` XHTML documents in the spine, each with a paragraph block and, when `image_bytes` is set, its own
    noise PNG (stored, not deflated, so the archive costs what the images weigh)."""
    items = []
    spine = []
    entries: list[tuple[str, bytes, bool]] = [("META-INF/container.xml", CONTAINER_XML.format(opf="OEBPS/content.opf").encode(), True)]
    nav_items = []
    for n in range(1, chapters + 1):
        items.append(f'    <item id="c{n}" href="c/{n}.xhtml" media-type="application/xhtml+xml"/>')
        spine.append(f'    <itemref idref="c{n}"/>')
        nav_items.append(f'<li><a href="c/{n}.xhtml">Chapter {n}</a></li>')
        image = f'<p><img src="../img/{n}.png" alt="noise {n}"/></p>\n' if image_bytes else ""
        body = f"<h1>Chapter {n}</h1>\n{image}{perf_paragraphs(n)}\n"
        entries.append((f"OEBPS/c/{n}.xhtml", xhtml(f"Chapter {n}", body, True).encode(), True))
        if image_bytes:
            items.append(f'    <item id="i{n}" href="img/{n}.png" media-type="image/png"/>')
            entries.append((f"OEBPS/img/{n}.png", noise_png(n), False))
    items.append('    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>')
    nav = (
        '<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE html>\n'
        '<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Contents</title></head>'
        '<body><nav epub:type="toc"><h1>Contents</h1><ol>' + "".join(nav_items) + "</ol></nav></body></html>\n"
    )
    entries.append(("OEBPS/nav.xhtml", nav.encode(), True))
    package = (
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">\n'
        '  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">\n'
        f'    <dc:identifier id="uid">urn:uuid:autojs6-readium-perf-{identifier}</dc:identifier>\n'
        f"    <dc:title>{title}</dc:title>\n    <dc:language>en</dc:language>\n"
        '    <dc:creator>AutoJs6 Readium EPUB Reader fixtures</dc:creator>\n'
        '    <meta property="dcterms:modified">2026-09-21T00:00:00Z</meta>\n  </metadata>\n'
        "  <manifest>\n" + "\n".join(items) + "\n  </manifest>\n  <spine>\n" + "\n".join(spine) + "\n  </spine>\n</package>\n"
    )
    entries.insert(1, ("OEBPS/content.opf", package.encode(), True))
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        info = zipfile.ZipInfo("mimetype", FIXED_TIME)
        info.compress_type = zipfile.ZIP_STORED
        archive.writestr(info, b"application/epub+zip")
        for name, data, deflate in entries:
            info = zipfile.ZipInfo(name, FIXED_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED if deflate else zipfile.ZIP_STORED
            archive.writestr(info, data)
    return buffer.getvalue()


PERF_FIXTURES = {
    "perf-images-20mb.epub": lambda: perf_book("Perf images 20 MB", "images-20mb", chapters=100, image_bytes=1),
    "perf-images-200mb.epub": lambda: perf_book("Perf images 200 MB", "images-200mb", chapters=1000, image_bytes=1),
    "perf-chapters-5000.epub": lambda: perf_book("Perf 5000 chapters", "chapters-5000", chapters=5000),
}


def main_perf() -> None:
    PERF_OUT.mkdir(parents=True, exist_ok=True)
    for name, build in PERF_FIXTURES.items():
        path = PERF_OUT / name
        if path.exists():
            print(f"Kept {path.relative_to(ROOT).as_posix()} ({path.stat().st_size} bytes)")
            continue
        data = build()
        path.write_bytes(data)
        print(f"Generated {path.relative_to(ROOT).as_posix()} ({len(data)} bytes) sha256={hashlib.sha256(data).hexdigest()}")


def main() -> None:
    if "--perf" in sys.argv[1:]:
        main_perf()
        return
    OUT.mkdir(parents=True, exist_ok=True)
    sums = []
    for name, data in fixtures().items():
        path = OUT / name
        path.write_bytes(data)
        digest = hashlib.sha256(data).hexdigest()
        sums.append(f"{digest}  {name}")
        print(f"Generated {path.relative_to(ROOT).as_posix()} ({len(data)} bytes) sha256={digest}")
    (OUT / "SHA256SUMS.txt").write_text("\n".join(sums) + "\n", encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
