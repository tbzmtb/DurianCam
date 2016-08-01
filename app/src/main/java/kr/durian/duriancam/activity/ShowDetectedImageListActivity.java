package kr.durian.duriancam.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.io.FileOutputStream;
import java.util.ArrayList;

import kr.durian.duriancam.R;
import kr.durian.duriancam.adapter.SecureDetectedListAdapter;
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
 * Created by sunyungkim on 16. 7. 29..
 */
public class ShowDetectedImageListActivity extends Activity implements View.OnClickListener {
    private String mPushImageTime;
    private IDataService mService;
    private final String TAG = getClass().getName();
    private ListView mListView;
    private SecureDetectedListAdapter mDetectedAdapter;
    private ArrayList<SecureDetectedData> mArrayDetectedData = new ArrayList<>();
    private final int MAX_DATA_NUM = 100;
    private ImageButton mBtnBack;
    private TextView mBtnDelete;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "ShowDetectedImageListActivity oncreate call");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_show_detected_image);
        mListView = (ListView) findViewById(R.id.image_list_view);
        mDetectedAdapter = new SecureDetectedListAdapter(this, mArrayDetectedData);
        mListView.setAdapter(mDetectedAdapter);
        mListView.setOnItemClickListener(mOnListItemClicked);
        mBtnBack = (ImageButton) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);
        mBtnDelete = (TextView) findViewById(R.id.btn_delete);
        mBtnDelete.setOnClickListener(this);

    }

    AdapterView.OnItemClickListener mOnListItemClicked = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SecureDetectedData data = ((SecureDetectedListAdapter) parent.getAdapter()).getData().get(position);
            Intent intent = new Intent(ShowDetectedImageListActivity.this, DetectedItemDetailActivity.class);
            intent.putExtra("selected_data", data);
            startActivity(intent);

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
        IntentFilter intentFilter = new IntentFilter(Config.BROADCAST_SECURE_DETECTED);

        registerReceiver(mSecurePushReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterServiceCallback();
        unregisterReceiver(mSecurePushReceiver);
        unbindService(mConnection);

    }

    @Override
    public void finish() {
        closeWebSocket();
        super.finish();
    }

    private void closeWebSocket() {
        try {
            if (mService != null) {
                mService.closeWebSocket();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private void getDataFromDataBase() {
        Logger.d(TAG, "getDataFromDataBase call");
        mArrayDetectedData.clear();
        String rtcid = "";
        if(DataPreference.getMode() == Config.MODE_VIEWER){
            rtcid = DataPreference.getPeerRtcid();
        }else {
            rtcid = DataPreference.getRtcid();
        }
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(CamProvider.MOTION_IMAGE_TABLE_URI, CamSQLiteHelper.TABLE_SECURE_ALL_COLUMNS,
                CamSQLiteHelper.COL_RTCID + " = ? AND " + CamSQLiteHelper.COL_VIEWR_OR_CAMERA_MODE + " = ? AND " + CamSQLiteHelper.COL_DELETE_VALUE + " = ? ",
                new String[]{rtcid, String.valueOf(DataPreference.getMode()), String.valueOf(Config.NONE_DELETE)}, CamSQLiteHelper.COL_DATE + " desc");
        if (c != null && c.moveToFirst()) {
            try {
                do {
                    String colRtcid = c.getString(c.getColumnIndex(CamSQLiteHelper.COL_RTCID));
                    String path = c.getString(c.getColumnIndex(CamSQLiteHelper.COL_FILE_PATH));
                    long time = c.getLong(c.getColumnIndex(CamSQLiteHelper.COL_DATE));
                    SecureDetectedData data = new SecureDetectedData(colRtcid, path, time, Config.NONE_SELECT);
                    mArrayDetectedData.add(data);
                    if (mArrayDetectedData.size() == MAX_DATA_NUM) {
                        break;
                    }
                } while (c.moveToNext());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }
        Logger.d(TAG, "mArrayDetectedData size = " + mArrayDetectedData.size());
        mDetectedAdapter.notifyDataSetChanged();
    }

    private BroadcastReceiver mSecurePushReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Config.BROADCAST_SECURE_DETECTED)) {
                // 액티비티 실행중 감지가 오는경우
                if (mService != null) {
                    getDataFromPeerServer();
                }
            }
        }
    };

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                registerServiceCallback();
                getDataFromDataBase();
                if (DataPreference.getMode() == Config.MODE_VIEWER) {
                    getDataFromPeerServer();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private void getDataFromPeerServer() {
        try {
            if (mService != null) {
                JSONObject data = new JSONObject();
                data.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
                data.put(Config.PARAM_FROM, DataPreference.getPeerRtcid());
                data.put(Config.PARAM_TO, DataPreference.getRtcid());
                data.put(Config.PARAM_SESSION_ID, System.currentTimeMillis());
                data.put(Config.PARAM_DESCRIPTION, Config.PARAM_GET_TOTAL_SECURE_IMAGE_DATA);
                mService.sendData(data.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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
            Logger.d(TAG, "이미 데이타가 있습니다 ");
        } else {
            Logger.d(TAG, "insert 데이타 ");
            try {
                ContentValues values = new ContentValues();
                values.put(CamSQLiteHelper.COL_RTCID, rtcid);
                values.put(CamSQLiteHelper.COL_FILE_PATH, path);
                values.put(CamSQLiteHelper.COL_DATE, date);
                values.put(CamSQLiteHelper.COL_VIEWR_OR_CAMERA_MODE, String.valueOf(DataPreference.getMode()));
                values.put(CamSQLiteHelper.COL_DELETE_VALUE, Config.NONE_DELETE);
                resolver.insert(CamProvider.MOTION_IMAGE_TABLE_URI, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            SecureDetectedData data = new SecureDetectedData(rtcid, path, Long.parseLong(date), Config.NONE_SELECT);
            mArrayDetectedData.add(data);
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
            try {
                JSONObject json = new JSONObject(data);

                if (value == Config.HANDLER_MODE_CONFIG_ACK) {
                    String description = json.getString(Config.PARAM_DESCRIPTION);
                    if (description.equals(Config.PARAM_GET_TOTAL_SECURE_IMAGE_DATA)) {
                        Logger.d(TAG, "total data taken ");
                        JSONObject config = json.getJSONObject(Config.PARAM_CONFIG);
                        int size = Integer.parseInt(config.getString(Config.PARAM_SIZE));
                        for (int i = 0; i < size; i++) {
                            String time = config.getString(String.valueOf(i));
                            if (!checkExistDataInLocalDataBase(time)) {
                                sendSecureImageRequestData(time);
                            }
                        }
                    }
//                    else if (description.equals(Config.PARAM_SECURE_IMAGE_REQUEST)) {
//                        Logger.d(TAG, "makeImageFromJsonData 시작 ");
//                        makeImageFromJsonData(json);
//                        if (mDetectedAdapter != null) {
//                            mDetectedAdapter.notifyDataSetChanged();
//                        }
//                    }


                } else if (value == Config.HANDLER_MODE_EVENT) {
                    makeImageFromJsonData(json);
                    if (mDetectedAdapter != null) {
                        mDetectedAdapter.notifyDataSetChanged();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.btn_delete:
                Intent intent = new Intent(ShowDetectedImageListActivity.this, DeleteDetectedImageActivity.class);
                startActivity(intent);
                break;

        }
    }
}
