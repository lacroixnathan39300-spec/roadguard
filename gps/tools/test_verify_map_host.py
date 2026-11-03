import io
import unittest
from verify_map_host import check

class Response:
    def __init__(self, status=206, range_header='bytes 0-126/300'):
        self.status = status
        self.headers = {'Content-Range': range_header}
    def __enter__(self): return self
    def __exit__(self, *args): pass
    def read(self, n): return b'PMTiles' + bytes([3]) + bytes(119)

class Tests(unittest.TestCase):
    def test_valid(self):
        self.assertTrue(check('https://example.org/france.pmtiles', lambda *a, **kw: Response())['range_supported'])
    def test_no_range(self):
        with self.assertRaises(ValueError): check('https://example.org/france.pmtiles', lambda *a, **kw: Response(status=200))
    def test_wrong_content_range(self):
        with self.assertRaises(ValueError): check('https://example.org/france.pmtiles', lambda *a, **kw: Response(range_header='bytes 10-136/300'))
    def test_bad_url(self):
        with self.assertRaises(ValueError): check('http://example.org/france.pmtiles', lambda *a, **kw: Response())

if __name__ == '__main__': unittest.main()
