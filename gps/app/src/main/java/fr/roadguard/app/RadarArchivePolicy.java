package fr.roadguard.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;

/** Structural 7z checks before extraction. The source SHA-256 also authenticates the payload.
 * This validator does not decompress entries and must never imply radar activation. */
final class RadarArchivePolicy {
  static final long MAX_ARCHIVE_BYTES=100_000_000L;
  static final long MAX_NEXT_HEADER_BYTES=2_000_000L;
  private RadarArchivePolicy(){}
  private static long u32(byte[] b,int start){
    long n=0;for(int i=0;i<4;i++)n|=((long)b[start+i]&255)<<(8*i);return n;
  }
  private static long u64(byte[] b,int start)throws IOException{
    if(b[start+7]<0)throw new IOException("Entier 7z hors limites");
    long n=0;for(int i=0;i<8;i++)n|=((long)b[start+i]&255)<<(8*i);return n;
  }
  static void verify(File file)throws IOException{
    if(!file.isFile()||file.length()<33||file.length()>MAX_ARCHIVE_BYTES)
      throw new IOException("Taille d'archive 7z invalide");
    try(RandomAccessFile in=new RandomAccessFile(file,"r")){
      byte[] header=new byte[32];in.readFully(header);
      byte[] signature={0x37,0x7a,(byte)0xbc,(byte)0xaf,0x27,0x1c};
      for(int i=0;i<signature.length;i++)if(header[i]!=signature[i])throw new IOException("Signature 7z invalide");
      if(header[6]!=0||(header[7]&255)>4)throw new IOException("Version 7z non prise en charge");
      CRC32 crc=new CRC32();crc.update(header,12,20);
      if(crc.getValue()!=u32(header,8))throw new IOException("CRC de l'en-tête 7z incorrect");
      long offset=u64(header,12),size=u64(header,20),length=in.length();
      if(size<1||size>MAX_NEXT_HEADER_BYTES||offset>length-32)
        throw new IOException("Métadonnées 7z hors limites");
      long start=32+offset;
      if(size>length-start)throw new IOException("Archive 7z tronquée");
      in.seek(start);crc.reset();byte[] buffer=new byte[8192];long left=size;
      while(left>0){int count=in.read(buffer,0,(int)Math.min(left,buffer.length));
        if(count<0)throw new IOException("Métadonnées 7z tronquées");crc.update(buffer,0,count);left-=count;}
      if(crc.getValue()!=u32(header,28))throw new IOException("CRC des métadonnées 7z incorrect");
    }
  }
}
