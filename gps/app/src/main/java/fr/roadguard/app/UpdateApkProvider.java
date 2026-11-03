package fr.roadguard.app;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;import java.io.*;
public final class UpdateApkProvider extends ContentProvider {
 public boolean onCreate(){return true;}private File file(Uri u)throws FileNotFoundException{if(!"/apk".equals(u.getPath()))throw new FileNotFoundException();File f=new File(getContext().getCacheDir(),"roadguard-update.apk");if(!f.isFile())throw new FileNotFoundException();return f;}
 public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
 public String getType(Uri u){return "application/vnd.android.package-archive";}
 public Cursor query(Uri u,String[] projection,String selection,String[] args,String sort){try{File f=file(u);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(cols);Object[] values=new Object[cols.length];for(int i=0;i<cols.length;i++)values[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?f.getName():OpenableColumns.SIZE.equals(cols[i])?f.length():null;c.addRow(values);return c;}catch(Exception e){return null;}}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
