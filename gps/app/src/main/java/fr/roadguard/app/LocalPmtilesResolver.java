package fr.roadguard.app;

import java.io.File;
import java.io.IOException;

/** Prefer checksum-verified downloaded sectors, preserving legacy manual imports. */
public final class LocalPmtilesResolver {
  private LocalPmtilesResolver() {}
  /** Availability only, for renderer selection on the UI thread; no full-file hashing. */
  public static File completed(String id,OfflineCellStore store,File legacyDir)throws IOException {
    if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}")||store==null||legacyDir==null)return null;
    File file=store.completedFile(id);if(file!=null)return file;
    file=new File(legacyDir,id+".pmtiles");return file.isFile()?file:null;
  }
  public static File resolve(String id, OfflineCellStore verified, File legacyDir) throws IOException {
    if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}") || verified == null || legacyDir == null)
      return null;
    File trusted = verified.verifiedFile(id);
    if (trusted != null) return trusted;
    // Legacy user-imported packs remain readable; this is NOT proof of a SHA-256 check.
    File old = new File(legacyDir, id + ".pmtiles");
    return old.isFile() ? old : null;
  }
}
