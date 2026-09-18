"""Guards docs/fixtures: the committed samples must match the generator byte for byte."""

from pathlib import Path
import hashlib
import importlib.util
import re
import unittest

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

    def test_epub_containers_start_with_the_stored_mimetype_entry(self):
        for name in ("minimal-epub2.epub", "minimal-epub3.epub"):
            data = self.generated[name]
            self.assertEqual(data[:4], b"PK\x03\x04", name)
            self.assertEqual(data[30:38], b"mimetype", name)
            self.assertEqual(data[38:58], b"application/epub+zip", name)


if __name__ == "__main__":
    unittest.main()
