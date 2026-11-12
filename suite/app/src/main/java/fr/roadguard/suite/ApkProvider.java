/*
 * Adapté d'UnknownAuto (https://github.com/GitMag/UnknownAuto) — MIT License, Copyright (c) 2026 GitMag.
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */
package fr.roadguard.suite;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Donne l'APK de Pilote (un seul fichier, cache/install.apk) à l'installateur d'Android, en lecture seule :
 * content://fr.roadguard.suite.apk/install.apk
 */
public final class ApkProvider extends ContentProvider {

    static final String AUTHORITY = "fr.roadguard.suite.apk";
    static final String NAME = "install.apk";

    static Uri uri() {
        return new Uri.Builder().scheme("content").authority(AUTHORITY).path(NAME).build();
    }

    private File fileFor(Uri uri) {
        String name = uri.getLastPathSegment();
        if (!NAME.equals(name)) {
            throw new SecurityException("not served: " + name);
        }
        return new File(getContext().getCacheDir(), NAME);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // L'installateur demande le nom et la taille avant de lire.
        File f = fileFor(uri);
        String[] cols = projection != null ? projection : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor c = new MatrixCursor(cols);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) {
                row[i] = f.getName();
            } else if (OpenableColumns.SIZE.equals(cols[i])) {
                row[i] = f.length();
            } else {
                row[i] = null;
            }
        }
        c.addRow(row);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
