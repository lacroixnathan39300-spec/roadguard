import io, json, unittest
from unittest.mock import patch
from verify_france_hosted import check

class Response:
 def __init__(self, data, status, headers, url):
  self.buf=io.BytesIO(data);self.status=status;self.headers=headers;self.url=url
 def __enter__(self):return self
 def __exit__(self,*a):pass
 def read(self,n=-1):return self.buf.read(n)

class TestHosted(unittest.TestCase):
 def setUp(self):
  self.url='https://maps.example.org/catalog.json';self.pack='https://maps.example.org/france.pmtiles'
  self.entry={'country':'france','scope':'country','format':'pmtiles','url':self.pack,'size':200,'sha256':'a'*64}
  self.header=bytearray(127);self.header[:7]=b'PMTiles';self.header[7]=3;self.header[99]=1
 def runcheck(self,status=206,content_range='bytes 0-126/200',header=None):
  catalog=Response(json.dumps({'packs':[self.entry]}).encode(),200,{},self.url)
  archive=Response(bytes(self.header) if header is None else header,status,{'Content-Range':content_range},self.pack)
  with patch('urllib.request.urlopen',side_effect=[catalog,archive]):return check(self.url)
 def test_valid_range(self):self.assertEqual(self.runcheck()['range'],'206 OK')
 def test_rejects_server_without_range(self):
  with self.assertRaisesRegex(ValueError,'Range unsupported'):self.runcheck(status=200)
 def test_rejects_wrong_length(self):
  with self.assertRaisesRegex(ValueError,'Content-Range'):self.runcheck(content_range='bytes 0-126/300')
 def test_rejects_nonvector(self):
  h=bytearray(self.header);h[99]=2
  with self.assertRaisesRegex(ValueError,'not PMTiles'):self.runcheck(header=bytes(h))
 def test_rejects_http_pack(self):
  self.entry['url']='http://maps.example.org/france.pmtiles'
  with self.assertRaisesRegex(ValueError,'HTTPS'):self.runcheck()

if __name__=='__main__':unittest.main()
