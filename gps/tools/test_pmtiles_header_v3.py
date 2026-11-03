import struct, unittest

def check(h, size):
    assert len(h)>=127 and h[:7]==b"PMTiles" and h[7]==3
    def region(o,l,required=False):return o>=0 and l>=(1 if required else 0) and o<=size and l<=size-o
    for i,required in ((8,True),(24,False),(40,False),(56,False)):
        offset,length=struct.unpack_from('<QQ',h,i)
        assert region(offset,length,required)
    assert h[100]<=h[101]<=30
    assert 1<=h[99]<=5 and 1<=h[97]<=4 and 1<=h[98]<=4

class TestPMTilesHeader(unittest.TestCase):
    def sample(self):
        b=bytearray(127);b[:7]=b'PMTiles';b[7]=3
        struct.pack_into('<QQQQQQQQ',b,8,127,20,147,10,157,10,167,33)
        b[97]=2;b[98]=2;b[99]=1;b[100]=0;b[101]=14
        return b
    def test_valid(self):check(self.sample(),200)
    def test_bad_magic(self):
        b=self.sample();b[0]=0
        with self.assertRaises(AssertionError):check(b,200)
    def test_unsupported_version(self):
        b=self.sample();b[7]=4
        with self.assertRaises(AssertionError):check(b,200)
    def test_invalid_zoom(self):
        b=self.sample();b[100]=15
        with self.assertRaises(AssertionError):check(b,200)
    def test_out_of_bounds(self):
        b=self.sample();struct.pack_into('<Q',b,16,999)
        with self.assertRaises(AssertionError):check(b,200)
    def test_bad_leaf_range(self):
        b=self.sample();struct.pack_into('<Q',b,40,201)
        with self.assertRaises(AssertionError):check(b,200)
    def test_bad_tile_data_range(self):
        b=self.sample();struct.pack_into('<Q',b,64,999)
        with self.assertRaises(AssertionError):check(b,200)
    def test_bad_compression(self):
        b=self.sample();b[97]=0
        with self.assertRaises(AssertionError):check(b,200)
    def test_bad_tile_type(self):
        b=self.sample();b[99]=0
        with self.assertRaises(AssertionError):check(b,200)
if __name__=='__main__':unittest.main()
