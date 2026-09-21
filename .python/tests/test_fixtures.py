"""Guards docs/fixtures: the committed samples must match the generator byte for byte."""

from pathlib import Path
import hashlib
import io
import importlib.util
import re
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "docs/fixtures"
MAX_TOTAL_BYTES = 15 * 1024 * 1024


def load_generator():
    spec = importlib.util.spec_from_file_location("generate_fixtures", ROOT / ".python/generate_fixtures.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FixtureGeneratorTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.generated = load_generator().fixtures()

    def test_generator_is_deterministic(self):
        again = load_generator().fixtures()
        self.assertEqual(self.generated, again)

    def test_committed_fixtures_match_the_generator_and_the_checksum_list(self):
        sums = {}
        for line in (FIXTURES / "SHA256SUMS.txt").read_text(encoding="utf-8").splitlines():
            digest, name = line.split("  ", 1)
            sums[name] = digest
        self.assertEqual(set(self.generated), set(sums))
        for name, data in self.generated.items():
            with self.subTest(fixture=name):
                self.assertEqual(hashlib.sha256(data).hexdigest(), sums[name])
                self.assertEqual(data, (FIXTURES / name).read_bytes())
        committed = {p.name for p in FIXTURES.glob("*.epub")}
        self.assertEqual(set(self.generated), committed)

    def test_every_fixture_is_documented_and_the_set_stays_small(self):
        readme = (FIXTURES / "README.md").read_text(encoding="utf-8")
        documented = set(re.findall(r"^\| `([^`]+\.epub)` \|", readme, flags=re.MULTILINE))
        self.assertEqual(set(self.generated), documented)
        self.assertLess(sum(len(data) for data in self.generated.values()), MAX_TOTAL_BYTES)

    def test_direction_samples_declare_their_language_and_page_progression(self):
        expected = {"vertical-ja.epub": "ja", "vertical-zh.epub": "zh-Hant", "rtl-ar.epub": "ar"}
        for name, lang in expected.items():
            with self.subTest(fixture=name), zipfile.ZipFile(io.BytesIO(self.generated[name])) as archive:
                opf = archive.read("OEBPS/content.opf").decode("utf-8")
                self.assertIn(f"<dc:language>{lang}</dc:language>", opf)
                self.assertIn('<spine page-progression-direction="rtl">', opf)
                chapter = archive.read("OEBPS/chapter1.xhtml").decode("utf-8")
                self.assertIn(f'xml:lang="{lang}"', chapter)
                self.assertEqual(name == "rtl-ar.epub", 'dir="rtl"' in chapter)
                css = archive.read("OEBPS/style.css").decode("utf-8")
                self.assertEqual(name == "vertical-ja.epub", "writing-mode: vertical-rl" in css)

    def test_the_fixed_layout_sample_declares_its_layout_viewport_and_spreads(self):
        with zipfile.ZipFile(io.BytesIO(self.generated["fixed-layout.epub"])) as archive:
            opf = archive.read("OEBPS/content.opf").decode("utf-8")
            self.assertIn('<meta property="rendition:layout">pre-paginated</meta>', opf)
            self.assertIn('<meta property="rendition:spread">auto</meta>', opf)
            self.assertEqual(1, opf.count('properties="rendition:page-spread-center"'))
            self.assertEqual(3, opf.count('properties="page-spread-left"'))
            self.assertEqual(2, opf.count('properties="page-spread-right"'))
            for number in range(1, 7):
                page = archive.read(f"OEBPS/page{number}.xhtml").decode("utf-8")
                self.assertIn('<meta name="viewport" content="width=600, height=800"/>', page)
                self.assertIn(f'<p class="number">{number}</p>', page)

    def test_hostile_fixtures_carry_their_markers(self):
        generated = self.generated
        self.assertEqual(22, len(generated["malformed-empty-zip.epub"]))
        with zipfile.ZipFile(io.BytesIO(generated["malformed-missing-mimetype.epub"])) as archive:
            self.assertNotIn("mimetype", archive.namelist())
            self.assertIn("META-INF/container.xml", archive.namelist())
        with zipfile.ZipFile(io.BytesIO(generated["malformed-bad-opf.epub"])) as archive:
            self.assertFalse(archive.read("OEBPS/content.opf").decode("utf-8").rstrip().endswith("</package>"))
        with zipfile.ZipFile(io.BytesIO(generated["malformed-xxe.epub"])) as archive:
            opf = archive.read("OEBPS/content.opf").decode("utf-8")
            self.assertIn('<!ENTITY canary SYSTEM "file:///data/data/', opf)
            self.assertIn("<dc:title>XXE &canary;</dc:title>", opf)
            chapter = archive.read("OEBPS/chapter1.xhtml").decode("utf-8")
            self.assertEqual(1, chapter.count("<!DOCTYPE"))
            self.assertIn("<p>canary: &canary;</p>", chapter)
        traversal = generated["malformed-traversal-encoded.epub"]
        # zipfile rewrites os.sep to "/" when it reads names back on Windows, so the entry names are checked raw.
        for name in ("/abs/escaped.txt", "..\\escaped-win.txt", "OEBPS/../escaped-dot.txt", "escaped.txt"):
            self.assertIn(name.encode("ascii"), traversal)
        with zipfile.ZipFile(io.BytesIO(traversal)) as archive:
            opf = archive.read("OEBPS/content.opf").decode("utf-8")
            for href in ("%2e%2e/%2e%2e/escaped.txt", "../../escaped.txt", "/etc/hosts", "file:///etc/hosts", "..\\..\\escaped.txt"):
                self.assertIn(f'href="{href}"', opf)
        with zipfile.ZipFile(io.BytesIO(generated["malformed-long-names.epub"])) as archive:
            self.assertEqual(1, len([name for name in archive.namelist() if len(name) > 2048]))
        with zipfile.ZipFile(io.BytesIO(generated["malformed-duplicate-entries.epub"])) as archive:
            self.assertEqual(2, archive.namelist().count("OEBPS/chapter1.xhtml"))
            self.assertEqual(2, archive.read("OEBPS/content.opf").decode("utf-8").count('<itemref idref="chapter1"/>'))
        with zipfile.ZipFile(io.BytesIO(generated["malformed-lcp-license-only.epub"])) as archive:
            self.assertIn("META-INF/license.lcpl", archive.namelist())
            self.assertNotIn("META-INF/encryption.xml", archive.namelist())
        with zipfile.ZipFile(io.BytesIO(generated["malformed-encrypted-adept.epub"])) as archive:
            self.assertIn("http://ns.adobe.com/adept", archive.read("META-INF/encryption.xml").decode("utf-8"))

    def test_epub_containers_start_with_the_stored_mimetype_entry(self):
        for name in ("minimal-epub2.epub", "minimal-epub3.epub"):
            data = self.generated[name]
            self.assertEqual(data[:4], b"PK\x03\x04", name)
            self.assertEqual(data[30:38], b"mimetype", name)
            self.assertEqual(data[38:58], b"application/epub+zip", name)


if __name__ == "__main__":
    unittest.main()
