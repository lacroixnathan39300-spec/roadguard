package fr.roadguard.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Read the actual maximum zoom from a PMTiles v3 vector archive, including GPS-library files. */
public final class OfflineVectorZoom {
  private OfflineVectorZoom() {}
  public static int read(File archive) throws IOException {
    if (archive==null || !archive.isFile()) throw new IOException("Missing PMTiles archive");
    try (RandomAccessFile in=new RandomAccessFile(archive,"r")) {
      if(in.length()<127)throw new IOException("Truncated PMTiles header");
      byte[] h=new byte[127];in.readFully(h);
      byte[] magic={'P','M','T','i','l','e','s',3};
      for(int i=0;i<magic.length;i++)if(h[i]!=magic[i])throw new IOException("Not PMTiles v3");
      int min=h[100]&255,max=h[101]&255;
      if(min>max||max>22)throw new IOException("Invalid PMTiles zoom range");
      return max;
    }
  }
}
