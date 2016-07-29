package kr.durian.duriancam.activity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import kr.durian.duriancam.R;
import kr.durian.duriancam.util.DataPreference;

/**
 * Created by tbzm on 16. 5. 11.
 */
public class SettingActivity extends Activity implements View.OnClickListener {

    private final String TAG = getClass().getName();
    private ImageButton mBtnBack;
    private Switch mLoginSwitch;
    private Switch mScreenOnOffSwitch;
    private TextView mVersionText;
    private Switch mMotionDetectPushOnOffSwitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_setting);
        DataPreference.PREF = PreferenceManager.getDefaultSharedPreferences(this);
        mBtnBack = (ImageButton) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);
        mLoginSwitch = (Switch) findViewById(R.id.push_switch);
        mLoginSwitch.setOnCheckedChangeListener(onCheckChange);
        mScreenOnOffSwitch = (Switch)findViewById(R.id.screen_on_off_switch);
        mScreenOnOffSwitch.setOnCheckedChangeListener(onScreenSwitchCheckChange);
        mMotionDetectPushOnOffSwitch = (Switch)findViewById(R.id.push_on_off_switch);
        mMotionDetectPushOnOffSwitch.setOnCheckedChangeListener(onPushSwitchCheckChange);
        mVersionText = (TextView)findViewById(R.id.app_version);
        mVersionText.setText(getVersionName());
    }

    public String getVersionName()
    {
        try {
            PackageInfo pi= getPackageManager().getPackageInfo(getPackageName(), 0);
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


    @Override
    protected void onResume() {
        super.onResume();
        boolean check = DataPreference.getEasyLogin();
        if (mLoginSwitch != null) {
            mLoginSwitch.setChecked(check);
        }

        boolean screenValue = DataPreference.getKeepSceenOn();
        if(mScreenOnOffSwitch != null){
            mScreenOnOffSwitch.setChecked(screenValue);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();
                break;

        }
    }
}
