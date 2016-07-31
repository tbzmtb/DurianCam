package kr.durian.duriancam.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import kr.durian.duriancam.R;
import kr.durian.duriancam.adapter.SecureDetectedListDeleteAdapter;
import kr.durian.duriancam.data.SecureDetectedData;
import kr.durian.duriancam.provider.CamProvider;
import kr.durian.duriancam.provider.CamSQLiteHelper;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.service.IDataServiceCallback;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

/**
 * Created by sunyungkim on 16. 7. 31..
 */
public class DeleteDetectedImageActivity extends Activity implements View.OnClickListener {
    private String mPushImageTime;
    private IDataService mService;
    private final String TAG = getClass().getName();
    private ListView mListView;
    private SecureDetectedListDeleteAdapter mDetectedDeleteAdapter;
    private ArrayList<SecureDetectedData> mArrayDetectedDeleteData = new ArrayList<>();
    private final int MAX_DATA_NUM = 100;
    private ImageButton mBtnBack;
    private TextView mBtnDelete;
    private TextView mBtnDeleteAll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "DeleteDetectedImageActivity oncreate call");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_delete_detected_image);
        mListView = (ListView) findViewById(R.id.image_list_view);
        mDetectedDeleteAdapter = new SecureDetectedListDeleteAdapter(this, mArrayDetectedDeleteData);
        mListView.setAdapter(mDetectedDeleteAdapter);
        mListView.setOnItemClickListener(mOnListItemClicked);
        mBtnBack = (ImageButton) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);
        mBtnDelete = (TextView) findViewById(R.id.btn_delete);
        mBtnDelete.setOnClickListener(this);
        mBtnDeleteAll = (TextView) findViewById(R.id.btn_all_delete);
        mBtnDeleteAll.setOnClickListener(this);

    }

    AdapterView.OnItemClickListener mOnListItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            SecureDetectedData data = ((SecureDetectedListAdapter) parent.getAdapter()).getData().get(position);
            SecureDetectedData data = ((SecureDetectedListDeleteAdapter) parent.getAdapter()).getData().get(position);
            if (data.getSelected() == Config.NONE_SELECT) {
                data.setSelected(Config.SELECT);
            } else {
                data.setSelected(Config.NONE_SELECT);
            }
            ((SecureDetectedListDeleteAdapter) parent.getAdapter()).notifyDataSetChanged();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterServiceCallback();
        unbindService(mConnection);

    }

    @Override
    public void finish() {
        super.finish();
    }

    private void getDataFromDataBase() {
        Logger.d(TAG, "getDataFromDataBase call");
        mArrayDetectedDeleteData.clear();
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(CamProvider.MOTION_IMAGE_TABLE_URI, CamSQLiteHelper.TABLE_SECURE_ALL_COLUMNS,
                CamSQLiteHelper.COL_RTCID + " = ? AND " + CamSQLiteHelper.COL_VIEWR_OR_CAMERA_MODE + " = ? AND " + CamSQLiteHelper.COL_DELETE_VALUE + " = ? ",
                new String[]{DataPreference.getPeerRtcid(), String.valueOf(DataPreference.getMode()), String.valueOf(Config.NONE_DELETE)}, CamSQLiteHelper.COL_DATE + " desc");
        if (c != null && c.moveToFirst()) {
            try {
                do {
                    String rtcid = c.getString(c.getColumnIndex(CamSQLiteHelper.COL_RTCID));
                    String path = c.getString(c.getColumnIndex(CamSQLiteHelper.COL_FILE_PATH));
                    long time = c.getLong(c.getColumnIndex(CamSQLiteHelper.COL_DATE));
                    SecureDetectedData data = new SecureDetectedData(rtcid, path, time, Config.NONE_SELECT);
                    mArrayDetectedDeleteData.add(data);
                    if (mArrayDetectedDeleteData.size() == MAX_DATA_NUM) {
                        break;
                    }
                } while (c.moveToNext());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }
        Logger.d(TAG, "mArrayDetectedData size = " + mArrayDetectedDeleteData.size());
        mDetectedDeleteAdapter.notifyDataSetChanged();
    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                registerServiceCallback();
                getDataFromDataBase();

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private void sendSecureImageRequestData(String time) {
        try {
            if (mService != null && time != null) {
                JSONObject data = new JSONObject();
                JSONObject kobj = new JSONObject();
                kobj.put(Config.PARAM_IMAGE_TIME_KEY, time);
                data.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
                data.put(Config.PARAM_FROM, DataPreference.getPeerRtcid());
                data.put(Config.PARAM_TO, DataPreference.getRtcid());
                data.put(Config.PARAM_SESSION_ID, System.currentTimeMillis());
                data.put(Config.PARAM_DESCRIPTION, Config.PARAM_SECURE_IMAGE_REQUEST);
                data.put(Config.PARAM_CONFIG, kobj);
                mService.sendData(data.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void registerServiceCallback() {
        try {
            mService.registerCallback(mCallbcak);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void unregisterServiceCallback() {
        try {
            boolean b = mService.unregisterCallback(mCallbcak);
            Logger.d(TAG, "unregisterCallback call = ", b);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private synchronized void makeImageFromJsonData(JSONObject json) {
        Logger.d(TAG, "makeImageFromJsonData 시작 ");
        String mPeerRtcid = null;
        String detectedFileName = null;
        String base64ImageData = null;
        String time = null;
        String mFilePath = null;
        try {
            mPeerRtcid = json.get(Config.PARAM_FROM).toString();
            detectedFileName = json.get(Config.PARAM_FILE_NAME).toString();
            JSONObject event = json.getJSONObject(Config.PARAM_EVENT);
            base64ImageData = event.get(Config.PARAM_IMAGE_CUT).toString();
            time = json.getString(Config.PARAM_TIME);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Logger.d(TAG, mPeerRtcid);
        Logger.d(TAG, detectedFileName);
        Logger.d(TAG, base64ImageData);
        Logger.d(TAG, time);
        FileOutputStream fos;
        try {
            if (base64ImageData != null) {
                mFilePath = Config.getSaveImageFileExternalDirectory() + detectedFileName;
                fos = new FileOutputStream(mFilePath);
                byte[] decodedString = android.util.Base64.decode(base64ImageData, android.util.Base64.DEFAULT);
                fos.write(decodedString);
                fos.flush();
                fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        insertData2Database(mPeerRtcid, mFilePath, time);
    }

    private synchronized void insertData2Database(String rtcid, String path, String date) {
        Logger.d(TAG, "insertData2Database call" + date);
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(CamProvider.MOTION_IMAGE_TABLE_URI, CamSQLiteHelper.TABLE_SECURE_ALL_COLUMNS,
                CamSQLiteHelper.COL_RTCID + " = ? AND " + CamSQLiteHelper.COL_DATE + " = ? ", new String[]{DataPreference.getPeerRtcid(), date}, CamSQLiteHelper.COL_DATE + " desc");
        boolean isData = false;
        if (c != null && c.moveToFirst()) {

            try {
                do {
                    isData = true;
                } while (c.moveToNext());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }
        if (isData) {
            Logger.d(TAG, "이미 데이타가 있습니다" + path);
        } else {
            try {
                ContentValues values = new ContentValues();
                values.put(CamSQLiteHelper.COL_RTCID, rtcid);
                values.put(CamSQLiteHelper.COL_FILE_PATH, path);
                values.put(CamSQLiteHelper.COL_DATE, date);
                values.put(CamSQLiteHelper.COL_VIEWR_OR_CAMERA_MODE, String.valueOf(DataPreference.getMode()));
                resolver.insert(CamProvider.MOTION_IMAGE_TABLE_URI, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            SecureDetectedData data = new SecureDetectedData(rtcid, path, Long.parseLong(date), Config.NONE_SELECT);
            mArrayDetectedDeleteData.add(data);
        }
    }

    private boolean checkExistDataInLocalDataBase(String date) {
        Logger.d(TAG, "checkExistDataInLocalDataBase call" + date);
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(CamProvider.MOTION_IMAGE_TABLE_URI, CamSQLiteHelper.TABLE_SECURE_ALL_COLUMNS,
                CamSQLiteHelper.COL_RTCID + " = ? AND " + CamSQLiteHelper.COL_DATE + " = ? ", new String[]{DataPreference.getRtcid(), date}, CamSQLiteHelper.COL_DATE + " desc");
        boolean isData = false;
        if (c != null && c.moveToFirst()) {

            try {
                do {
                    isData = true;
                } while (c.moveToNext());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }
        return isData;
    }

    private IDataServiceCallback mCallbcak = new IDataServiceCallback.Stub() {

        @Override
        public void valueChanged(int value, String data) throws RemoteException {

        }
    };

    private void deleteData(ArrayList<SecureDetectedData> data) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        ContentProviderOperation operation;
        for (SecureDetectedData item : data) {

//            operation = ContentProviderOperation
//                    .newDelete(CamProvider.MOTION_IMAGE_TABLE_URI)
//                    .withSelection(CamSQLiteHelper.COL_FILE_PATH + " = ?", new String[]{item.getImagePath()})
//                    .build();
            operation = ContentProviderOperation
                    .newUpdate(CamProvider.MOTION_IMAGE_TABLE_URI)
                    .withSelection(CamSQLiteHelper.COL_FILE_PATH + " = ?", new String[]{item.getImagePath()})
                    .withValue(CamSQLiteHelper.COL_DELETE_VALUE, Config.DELETE)
                    .build();

            operations.add(operation);
            File file = new File(item.getImagePath());
            boolean b = file.delete();
            if (b) {
                Logger.d(TAG, "파일이 삭제되었습니다. ");
            } else {
                Logger.d(TAG, "file delete fail ");
            }
        }
        try {
            getContentResolver().applyBatch(CamProvider.AUTHORITY, operations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    private void deleteAllData(ArrayList<SecureDetectedData> data) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        ContentProviderOperation operation;
        for (SecureDetectedData item : data) {

            operation = ContentProviderOperation
                    .newUpdate(CamProvider.MOTION_IMAGE_TABLE_URI)
                    .withValue(CamSQLiteHelper.COL_DELETE_VALUE, Config.DELETE)
                    .build();

            operations.add(operation);
            File file = new File(item.getImagePath());
            boolean b = file.delete();
            if (b) {
                Logger.d(TAG, "파일이 삭제되었습니다. ");
            } else {
                Logger.d(TAG, "file delete fail ");
            }
        }
        try {
            getContentResolver().applyBatch(CamProvider.AUTHORITY, operations);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    private void showConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(null)
                .setMessage(getString(R.string.really_want_to_delete))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        deleteAllData(mArrayDetectedDeleteData);
                        finish();

                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private ArrayList<SecureDetectedData> getCheckedItem() {
        ArrayList<SecureDetectedData> data = new ArrayList<>();
        for (int i = 0; i < mArrayDetectedDeleteData.size(); i++) {
            if (mArrayDetectedDeleteData.get(i).getSelected() == Config.SELECT) {
                data.add(mArrayDetectedDeleteData.get(i));
            }
        }
        return data;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.btn_delete:
                deleteData(getCheckedItem());
                finish();
                break;
            case R.id.btn_all_delete:
                showConfirmDialog();
                break;

        }
    }
}
