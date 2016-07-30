package kr.durian.duriancam.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import kr.durian.duriancam.R;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.service.IDataServiceCallback;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

/**
 * Created by sunyungkim on 16. 7. 29..
 */
public class ShowDetectedImageActivity extends AppCompatActivity {
    private String mPushImageTime;
    private IDataService mService;
    private final String TAG = getClass().getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "ShowDetectedImageActivity oncreate call");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.show_detected_image_activity);
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void finish() {
        unregisterServiceCallback();
        unbindService(mConnection);
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

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                mPushImageTime = getIntent().getStringExtra(Config.PUSH_IMAGE_TIME_INTENT_KEY);
                registerServiceCallback();
                sendSecureImageRequestData();

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private void sendSecureImageRequestData() {
        try {
            if (mService != null && mPushImageTime != null) {
                JSONObject data = new JSONObject();
                JSONObject kobj = new JSONObject();
                kobj.put(Config.PARAM_IMAGE_TIME_KEY, mPushImageTime);
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
            Logger.d(TAG, "unregisterCallback1 call = ", b);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    final IDataServiceCallback mCallbcak = new IDataServiceCallback.Stub() {

        @Override
        public void valueChanged(int value, String data) throws RemoteException {
            if (value == Config.HANDLER_MODE_CONFIG_ACK) {
                try {
                    JSONObject json = new JSONObject(data);
                    String description = json.getString(Config.PARAM_DESCRIPTION);
                    if (description.equals(Config.PARAM_SECURE_IMAGE_REQUEST)) {
                        Logger.d(TAG, "base 64 디코딩 해야함 ");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
    };
}
