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
  vertical-ja.epub                   EPUB 3, Japanese, page-progression-direction rtl, publisher vertical-rl CSS
  vertical-zh.epub                   EPUB 3, traditional Chinese, page-progression-direction rtl, no writing mode
  rtl-ar.epub                        EPUB 3, Arabic, dir="rtl", page-progression-direction rtl

Usage: py .python/generate_fixtures.py
"""

from __future__ import annotations

import hashlib
import io
import struct
import zipfile
import zlib
from pathlib import Path
from typing import NamedTuple

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "fixtures"
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


def build_zip(entries: list[tuple[str, bytes]], mimetype_first: bool = True) -> bytes:
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as archive:
        if mimetype_first:
            info = zipfile.ZipInfo("mimetype", FIXED_TIME)
            info.compress_type = zipfile.ZIP_STORED
            archive.writestr(info, b"application/epub+zip")
        for name, data in entries:
            info = zipfile.ZipInfo(name, FIXED_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED
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


def fixtures() -> dict[str, bytes]:
    result: dict[str, bytes] = {}
    result["minimal-epub2.epub"] = build_zip(epub_entries(False, "Minimal EPUB 2"))
    result["minimal-epub3.epub"] = build_zip(epub_entries(True, "Minimal EPUB 3"))
    result["vertical-ja.epub"] = build_zip(epub_entries(True, "灯台守 (縦書き)", locale=JAPANESE, identifier="vertical-ja"))
    result["vertical-zh.epub"] = build_zip(
        epub_entries(True, "守塔人 (直排)", locale=CHINESE_TRADITIONAL, identifier="vertical-zh")
    )
    result["rtl-ar.epub"] = build_zip(epub_entries(True, "حارس المنارة", locale=ARABIC, identifier="rtl-ar"))
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
    return result


def main() -> None:
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
