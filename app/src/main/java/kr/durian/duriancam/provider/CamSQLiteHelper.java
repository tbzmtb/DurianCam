package kr.durian.duriancam.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by tbzm on 16. 4. 20.
 */
public class CamSQLiteHelper extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "motionimage.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_MOTION_IMAGE_LIST = "table_motion_image_list";

    public static final String COL_INEDEX = "_id";
    public static final String COL_RTCID = "rtcid";
    public static final String COL_DATE = "date";
    public static final String COL_VIEWR_OR_CAMERA_MODE = "mode";
    public static final String COL_FILE_PATH = "path";
    public static final String COL_DELETE_VALUE = "delete_value";
    public static final String[] TABLE_SECURE_ALL_COLUMNS = {COL_INEDEX, COL_RTCID, COL_DATE, COL_VIEWR_OR_CAMERA_MODE, COL_FILE_PATH, COL_DELETE_VALUE};

    private static final String DATABASE_CREATE_PARKING_LIST = "create table "
            + TABLE_MOTION_IMAGE_LIST + "(" + COL_INEDEX
            + " integer primary key autoincrement, "
            + COL_RTCID + " text not null , "
            + COL_DATE + " text not null , "
            + COL_VIEWR_OR_CAMERA_MODE + " text not null , "
            + COL_DELETE_VALUE + " text not null , "
            + COL_FILE_PATH + " text not null );";


    public CamSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_PARKING_LIST);
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_CREATE_PARKING_LIST);
        onCreate(db);
    }
}
