package kr.durian.duriancam.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by tbzm on 16. 4. 20.
 */
public class CamProvider extends ContentProvider {
    public static final String AUTHORITY = "kr.durian.duriancam.provider.CamProvider";
    public static final Uri MOTION_IMAGE_TABLE_URI = Uri.parse("content://"+AUTHORITY+"/motion_image_table");
    private static final int MOTION_IMAGE_TABLE = 0;
    private CamSQLiteHelper mMotionSqliteHelper;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "motion_image_table", MOTION_IMAGE_TABLE);

    }

    @Override
    public boolean onCreate() {
        mMotionSqliteHelper = new CamSQLiteHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor result = null;
        switch (uriMatcher.match(uri)) {
            case MOTION_IMAGE_TABLE: {
                SQLiteDatabase database = mMotionSqliteHelper.getReadableDatabase();
                result = database.query(CamSQLiteHelper.TABLE_MOTION_IMAGE_LIST, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
        }
        return result;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return String.valueOf(uriMatcher.match(uri));
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (uriMatcher.match(uri)) {
            case MOTION_IMAGE_TABLE: {
                SQLiteDatabase database = mMotionSqliteHelper.getWritableDatabase();
                long newId = database.insert(CamSQLiteHelper.TABLE_MOTION_IMAGE_LIST, null, values);

                return uri.parse(MOTION_IMAGE_TABLE_URI.toString() + "/?newid=" + newId);
            }
            default:
                return null;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case MOTION_IMAGE_TABLE: {
                SQLiteDatabase database = mMotionSqliteHelper.getWritableDatabase();
                int deleted = database.delete(CamSQLiteHelper.TABLE_MOTION_IMAGE_LIST, selection, selectionArgs);

                return deleted;

            }

        }
        return -1;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = -1;
        switch (uriMatcher.match(uri)) {
            case MOTION_IMAGE_TABLE: {
                SQLiteDatabase database = mMotionSqliteHelper.getWritableDatabase();
                count = database.update(CamSQLiteHelper.TABLE_MOTION_IMAGE_LIST, values, selection, selectionArgs);

                break;
            }

        }
        return count;
    }
}
