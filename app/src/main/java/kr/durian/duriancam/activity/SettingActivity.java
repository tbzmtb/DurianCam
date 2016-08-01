package kr.durian.duriancam.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

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
 * Created by tbzm on 16. 5. 11.
 */
public class SettingActivity extends Activity implements View.OnClickListener {
    private IDataService mService;
    private final String TAG = getClass().getName();
    private ImageButton mBtnBack;
    private Switch mLoginSwitch;
    private Switch mScreenOnOffSwitch;
    private TextView mVersionText;
    private Switch mMotionDetectPushOnOffSwitch;
    private RelativeLayout mDetectedDataLayout;
    private String mPushImageTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_setting);
        DataPreference.PREF = PreferenceManager.getDefaultSharedPreferences(this);
        mBtnBack = (ImageButton) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);
        mLoginSwitch = (Switch) findViewById(R.id.push_switch);
        mLoginSwitch.setOnCheckedChangeListener(onCheckChange);
        mScreenOnOffSwitch = (Switch) findViewById(R.id.screen_on_off_switch);
        mScreenOnOffSwitch.setOnCheckedChangeListener(onScreenSwitchCheckChange);
        mMotionDetectPushOnOffSwitch = (Switch) findViewById(R.id.push_on_off_switch);
        mMotionDetectPushOnOffSwitch.setOnCheckedChangeListener(onPushSwitchCheckChange);
        mVersionText = (TextView) findViewById(R.id.app_version);
        mVersionText.setText(getVersionName());
        mDetectedDataLayout = (RelativeLayout) findViewById(R.id.detected_data_layout);
        mDetectedDataLayout.setOnClickListener(this);
        mPushImageTime = getIntent().getStringExtra(Config.PUSH_IMAGE_TIME_INTENT_KEY);

    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean check = DataPreference.getEasyLogin();
        if (mLoginSwitch != null) {
            mLoginSwitch.setChecked(check);
        }

        boolean screenValue = DataPreference.getKeepSceenOn();
        if (mScreenOnOffSwitch != null) {
            mScreenOnOffSwitch.setChecked(screenValue);
        }
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mService.unregisterCallback(mCallbcak);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(mConnection);
    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                try {
                    mService.registerCallback(mCallbcak);
                    Logger.d(TAG,"pushImageTime = "+mPushImageTime);
                    if (mPushImageTime != null) {
                        mDetectedDataLayout.callOnClick();
                        mPushImageTime = null;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    final IDataServiceCallback mCallbcak = new IDataServiceCallback.Stub() {

        @Override
        public void valueChanged(int value, String data) throws RemoteException {
            if (value == Config.HANDLER_MODE_START) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (!json.isNull(Config.PARAM_DESCRIPTION)) {
                        String desciption = json.getString(Config.PARAM_DESCRIPTION);
                        if (desciption.equals(Config.PARAM_SUCCESS_DESCRIPTION)) {
                            startShowDetectedImageActivity();
                        } else {
                            //nothing to do
                        }
                    } else {
                        startShowDetectedImageActivity();

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void startShowDetectedImageActivity() {
        Intent intent = new Intent(SettingActivity.this, ShowDetectedImageListActivity.class);
        startActivity(intent);
    }

    public String getVersionName() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    CompoundButton.OnCheckedChangeListener onCheckChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            DataPreference.setEasyLogin(isChecked);

        }
    };

    CompoundButton.OnCheckedChangeListener onPushSwitchCheckChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            DataPreference.setPushEnable(isChecked);

        }
    };

    CompoundButton.OnCheckedChangeListener onScreenSwitchCheckChange = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            DataPreference.setScreenOnOff(isChecked);

        }
    };


    private void connectWebSocket() {
        if (mService != null) {
            try {
                mService.connectWebSocket();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();
                break;
            case R.id.detected_data_layout:
                if (DataPreference.getRtcid() == null) {
                    return;
                }
                connectWebSocket();
                break;

        }
    }
}
