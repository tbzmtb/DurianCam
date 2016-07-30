package kr.durian.duriancam.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.menu.ExpandedMenuView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.ads.*;
import com.ericsson.research.owr.sdk.CameraSource;
import com.ericsson.research.owr.sdk.InvalidDescriptionException;
import com.ericsson.research.owr.sdk.RtcCandidate;
import com.ericsson.research.owr.sdk.RtcCandidates;
import com.ericsson.research.owr.sdk.RtcConfig;
import com.ericsson.research.owr.sdk.RtcConfigs;
import com.ericsson.research.owr.sdk.RtcSession;
import com.ericsson.research.owr.sdk.RtcSessions;
import com.ericsson.research.owr.sdk.SessionDescription;
import com.ericsson.research.owr.sdk.SessionDescriptions;
import com.ericsson.research.owr.sdk.SimpleStreamSet;
import com.ericsson.research.owr.sdk.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kr.durian.duriancam.R;
import kr.durian.duriancam.provider.CamProvider;
import kr.durian.duriancam.provider.CamSQLiteHelper;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.service.IDataServiceCallback;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;
import kr.durian.duriancam.util.MediaScanner;

/**
 * Created by kimsunyung on 16. 7. 8..
 */
public class CameraActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener,
        RtcSession.OnLocalCandidateListener,
        RtcSession.OnLocalDescriptionListener,
        View.OnClickListener {
    private IDataService mService;
    private final String TAG = getClass().getName();
    private RtcConfig mRtcConfig;
    private RtcSession mRtcSession;
    private SimpleStreamSet mStreamSet;
    private VideoView mSelfView;
    private VideoView mRemoteView;
    private JSONObject mReceiveOfferData;
    private final String SDPMID = "sdpMid";
    private final String VIDEO = "video";
    private ArrayList<JSONObject> mCandidates = new ArrayList<>();
    private boolean getAnswerAck = false;
    private boolean getAnswer = false;
    private CheckPeerCameraHandler mHandler;
    private ProgressDialog mProgressDialog;
    boolean sendHangupAck = false;
    private RelativeLayout mDimViewLayout;
    private RelativeLayout mSelfViewDimLayout;
    private CheckHandler mCheckHandler;
    boolean checkFinish = true;
    private RelativeLayout mRemoteViewParent;
    private boolean startConnecting = false;
    private long remoteViewTimestemp = 0;
    private int checkBuffferDataCount = 0;
    private AudioManager mAudioManager;
    private RelativeLayout mBtnPhoto;
    private CheckBufferHandler mCheckBufferHandler;
    private RelativeLayout mSecureSettingView;
    private Switch mMotionDetectModeOnOffSwitch;
    private Switch mDisplayOnOffSwitch;
    private SeekBar mDetectSensitivity;
    private Switch mVideoRecordingSwitch;
    private long backKeyPressedTime;
    private ImageButton mSecureSettingBack;
    private Toast toast;
//    private final int CHECK_DELAY_COUNT = 60000 * 30;
private final int CHECK_DELAY_COUNT = 5000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "CameraActivity oncreate call");
        getSupportActionBar().hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (DataPreference.getMode() == Config.MODE_VIEWER) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            if (DataPreference.getKeepSceenOn()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
        setContentView(R.layout.activity_camera);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mBtnPhoto = (RelativeLayout) findViewById(R.id.btn_photo_save);
        mBtnPhoto.setOnClickListener(this);
        mDimViewLayout = (RelativeLayout) findViewById(R.id.dim_view_layout);
        mSelfViewDimLayout = (RelativeLayout) findViewById(R.id.self_view_dim_layout);
        mRemoteViewParent = (RelativeLayout) findViewById(R.id.remote_view_parent);
        mSecureSettingView = (RelativeLayout) findViewById(R.id.secure_setting_layout);
        mDisplayOnOffSwitch = (Switch) findViewById(R.id.display_on_off_switch);
        mDisplayOnOffSwitch.setOnCheckedChangeListener(mSwitchChangeListener);
        mDetectSensitivity = (SeekBar) findViewById(R.id.secure_sensitive_seek_bar);
        mDetectSensitivity.setOnSeekBarChangeListener(mDetectSensitivityChangeListener);
        mVideoRecordingSwitch = (Switch) findViewById(R.id.video_recording_switch);
        mVideoRecordingSwitch.setOnCheckedChangeListener(mSwitchChangeListener);
        mMotionDetectModeOnOffSwitch = (Switch) findViewById(R.id.motion_detect_mode_on_off_switch);
        mMotionDetectModeOnOffSwitch.setOnCheckedChangeListener(mSwitchChangeListener);

        mSecureSettingBack = (ImageButton) findViewById(R.id.btn_back);
        mSecureSettingBack.setOnClickListener(this);
        setMicMute(false);
        setSpeakerSound(true);
        setSpeakerPhone(false);
        mCheckBufferHandler = new CheckBufferHandler();
        mHandler = new CheckPeerCameraHandler();
        mCheckHandler = new CheckHandler();
        setWindowFrags();
        DataPreference.setSecuringMode(false);
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private SeekBar.OnSeekBarChangeListener mDetectSensitivityChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            DataPreference.setDetectSensitivity(String.valueOf(seekBar.getProgress()));
            sendSecureChangeOption();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }
    };

    private CompoundButton.OnCheckedChangeListener mSwitchChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()) {
                case R.id.display_on_off_switch:
                    if (b) {
                        DataPreference.setSecureDisplayEnable(Config.DISPLAY_HIDE_ON);
                    } else {
                        DataPreference.setSecureDisplayEnable(Config.DISPLAY_HIDE_OFF);
                    }
                    sendSecureChangeOption();
                    break;
                case R.id.video_recording_switch:
                    if (b) {
                        DataPreference.setVideoRecordingEnable(Config.VIDEO_RECORDING_ON);
                    } else {
                        DataPreference.setVideoRecordingEnable(Config.VIDEO_RECORDING_OFF);
                    }
                    sendSecureChangeOption();

                    break;
                case R.id.motion_detect_mode_on_off_switch:
                    if (!b) {
                        sendSecureFinishData();
                        finish();
                    }
                    break;
            }

        }
    };

    private void setDetectSensitivitySeekBar(String value) {
        if (value == null) {
            return;
        }
        try {
            if (mDetectSensitivity != null) {
                mDetectSensitivity.setProgress(Integer.parseInt(value));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMotionDetectModeEnable(String value) {
        if (value == null) {
            return;
        }
        if (value.equals(Config.MOTION_DETECT_MODE_ON)) {
            mMotionDetectModeOnOffSwitch.setChecked(true);
        } else {
            mMotionDetectModeOnOffSwitch.setChecked(false);
        }
    }

    private void setSecureDisplayEnalbe(String value) {
        if (value == null) {
            return;
        }
        if (value.equals(Config.DISPLAY_HIDE_ON)) {
            mDisplayOnOffSwitch.setChecked(true);
        } else {
            mDisplayOnOffSwitch.setChecked(false);
        }
    }

    private void setVideoRecordingEnable(String value) {
        if (value == null) {
            return;
        }
        if (value.equals(Config.VIDEO_RECORDING_ON)) {
            mVideoRecordingSwitch.setChecked(true);
        } else {
            mVideoRecordingSwitch.setChecked(false);
        }
    }

    private void setMicMute(boolean mute) {
        Logger.d(TAG, "setMicMute call");
        if (mAudioManager == null) {
            return;
        }
        mAudioManager.setMicrophoneMute(mute);

    }

    private void setSpeakerSound(boolean enable) {
        Logger.d(TAG, "setSpeakerSound call");
        if (mAudioManager == null) {
            return;
        }
        try {
            mAudioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, enable);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, enable);
            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, enable);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }

    private void setSpeakerPhone(boolean enable) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(enable);
    }

    private void setWindowFrags() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
    }

    private void startConnect(final int mode) {
        Logger.d(TAG, "sun startConnecting call");
        startConnecting = true;
        if (mStreamSet != null) {
            Logger.d(TAG, "error mStreamSet == not null");
            return;
        }
        mRtcConfig = RtcConfigs.defaultConfig(Config.STUN_SERVER, Config.TURN_SERVER);
        mStreamSet = SimpleStreamSet.defaultConfig(true, true);
        mSelfView = CameraSource.getInstance().createVideoView();
        mRemoteView = mStreamSet.createRemoteView();

        TextureView remoteView = (TextureView) findViewById(R.id.remote_view);
        mRemoteView.setView(remoteView);

        TextureView selfView = (TextureView) findViewById(R.id.self_view);
        selfView.setSurfaceTextureListener(this);
        mSelfView.setView(selfView);
        mSelfView.setMirrored(true);

        if (mode == Config.MODE_VIEWER) {
            mRtcSession = RtcSessions.create(mRtcConfig);
            mRtcSession.setOnLocalCandidateListener(CameraActivity.this);
            mRtcSession.setOnLocalDescriptionListener(CameraActivity.this);
            mRtcSession.start(mStreamSet);
            Logger.d(TAG, "mRtcSession init complete");
        }

    }

    private void showProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mProgressDialog == null) {
                        mProgressDialog = new ProgressDialog(CameraActivity.this);
                        mProgressDialog.setCanceledOnTouchOutside(false);
                        if (DataPreference.getMode() != Config.MODE_VIEWER) {
                            mProgressDialog.setMessage(getString(R.string.wait_for_view));
                        } else {
                            mProgressDialog.setMessage(getString(R.string.wait_for_connection));
                        }
                        mProgressDialog.setOnDismissListener(mProgressDismissListener);
                        mProgressDialog.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    DialogInterface.OnDismissListener mProgressDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            finish();
        }
    };

    private void cancelProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.setOnDismissListener(null);
                    mProgressDialog.cancel();
                    mProgressDialog = null;
                }
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "CameraActivity onPause Call");

    }


    public void unCallHandler() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(0);
        }
    };

    public void callHandler() {
        if (mHandler != null) {
            mHandler.postDelayed(mRunnable, 3000);

        }
    }

    public void unRegisterCheckHandler() {
        if (mCheckHandler != null) {
            mCheckHandler.removeCallbacks(mCheckRunnable);
        }
    }

    private Runnable mCheckRunnable = new Runnable() {
        @Override
        public void run() {
            mCheckHandler.sendEmptyMessage(0);
        }
    };

    public void callCheckHandler() {
        if (mCheckHandler != null) {
            mCheckHandler.postDelayed(mCheckRunnable, CHECK_DELAY_COUNT);

        }
    }
    private class CheckHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            JSONObject json = getCheckData();
            if (json != null)
                try {
                    mService.sendData(getCheckData().toString());
                    callCheckHandler();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
        }
    }

    private class CheckPeerCameraHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (DataPreference.getViewerWillConnectMode() == Config.MODE_SECURE) {
                checkPeerCameraExistForSecure();
            } else {
                checkPeerCameraExist();
            }
            callHandler();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, "CameraActivity onResume Call");

    }

    public void onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            toast = Toast.makeText(CameraActivity.this, getResources().getString(R.string.backbutton_click_is_finish), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            finish();
            toast.cancel();
        }
    }

    @Override
    public void finish() {
        Logger.d(TAG, "finish call");
        if (startConnecting) {
            return;
        }
        mDimViewLayout.setVisibility(View.VISIBLE);
        mSelfViewDimLayout.setVisibility(View.VISIBLE);
        mCheckBufferHandler.removeCallbacks(mCheckBufferRunnable);

        if (DataPreference.getMode() != Config.MODE_VIEWER && !checkFinish) {
            cancelProgress();
            sendHangupData();
            disconnectSession();
            showProgress();
            setMicMute(false);
            setSpeakerSound(true);
            setSpeakerPhone(false);
            checkFinish = true;
            getAnswerAck = false;
            return;
        } else {
            cancelProgress();
            if (DataPreference.getMode() == Config.MODE_VIEWER) {
                if (getAnswer) {
                    sendHangupData();
                    disconnectSession();
                }
            } else {
                if (getAnswerAck) {
                    sendHangupData();
                    disconnectSession();
                }
            }
            closeWebSocket();
            unregisterServiceCallback();
            unRegisterCheckHandler();
            unbindService(mConnection);
            unCallHandler();
            super.finish();
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

    private void closeWebSocket() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mService != null) {
                        mService.closeWebSocket();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 2000);

    }

    private void checkPeerCameraExist() {
        try {
            if (mService != null) {
                JSONObject json = getCheckPeerCameraExist();
                if (json == null) {
                    Logger.d(TAG, "json = " + json);
                    return;
                }
                mService.sendData(json.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void checkPeerCameraExistForSecure() {
        try {
            if (mService != null) {
                JSONObject json = getCheckPeerCameraExistForSecure();
                if (json == null) {
                    Logger.d(TAG, "json = " + json);
                    return;
                }
                mService.sendData(json.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendHangupData() {
        if (sendHangupAck) {
            return;
        }
        try {
            if (mService != null) {
                JSONObject json = getHangUpData();
                if (json == null) {
                    Logger.d(TAG, "json = " + json);
                    return;
                }
                mService.sendData(json.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendPeerCameraExist(JSONObject json) {
        try {
            if (mService != null) {
                JSONObject data = new JSONObject();
                data.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
                data.put(Config.PARAM_FROM, json.getString(Config.PARAM_TO));
                data.put(Config.PARAM_TO, json.getString(Config.PARAM_FROM));
                data.put(Config.PARAM_SESSION_ID, json.getString(Config.PARAM_SESSION_ID));
                data.put(Config.PARAM_DESCRIPTION, json.getString(Config.PARAM_DESCRIPTION));
                mService.sendData(data.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void secureSettingViewVisible(boolean visible, JSONObject json) {
        if (visible) {
            mSecureSettingView.setVisibility(View.VISIBLE);
        } else {
            mSecureSettingView.setVisibility(View.GONE);
        }
        if (json != null) {
            try {
                Logger.d(TAG, "받은 데이타 =" + json);
                JSONObject config = json.getJSONObject(Config.PARAM_CONFIG);
                DataPreference.setVideoRecordingEnable(config.getString(Config.PARAM_VIDEO_ON_OFF_VALUE));
                mVideoRecordingSwitch.setOnCheckedChangeListener(null);
                setVideoRecordingEnable(config.getString(Config.PARAM_VIDEO_ON_OFF_VALUE));
                mVideoRecordingSwitch.setOnCheckedChangeListener(mSwitchChangeListener);

                DataPreference.setDetectSensitivity(config.getString(Config.PARAM_DETECT_SENSITIVITY_VALUE));
                setDetectSensitivitySeekBar(config.getString(Config.PARAM_DETECT_SENSITIVITY_VALUE));

                DataPreference.setSecureDisplayEnable(config.getString(Config.PARAM_DETECT_DISPLAY_ON_OFF_VALUE));
                setSecureDisplayEnalbe(config.getString(Config.PARAM_DETECT_DISPLAY_ON_OFF_VALUE));

                setMotionDetectModeEnable(config.getString(Config.PARAM_DETECT_MODE_ON_OFF_VALUE));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendSecureChangeOption() {
        try {
            if (mService != null) {
                JSONObject data = new JSONObject();
                JSONObject kobj = new JSONObject();
                kobj.put(Config.PARAM_VIDEO_ON_OFF_VALUE, DataPreference.getVideoRecordingEnable());
                kobj.put(Config.PARAM_DETECT_SENSITIVITY_VALUE, DataPreference.getDetectSensitivity());
                kobj.put(Config.PARAM_DETECT_DISPLAY_ON_OFF_VALUE, DataPreference.getSecureDisplayEnable());
                data.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
                data.put(Config.PARAM_FROM, DataPreference.getPeerRtcid());
                data.put(Config.PARAM_TO, DataPreference.getRtcid());
                data.put(Config.PARAM_SESSION_ID, System.currentTimeMillis());
                data.put(Config.PARAM_DESCRIPTION, Config.PARAM_SECURE_CHANGE_OPTION);
                data.put(Config.PARAM_CONFIG, kobj);
                mService.sendData(data.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendPeerCameraExistForSecure(JSONObject json) {
        try {
            if (mService != null) {
                JSONObject data = new JSONObject();
                JSONObject kobj = new JSONObject();
                kobj.put(Config.PARAM_VIDEO_ON_OFF_VALUE, DataPreference.getVideoRecordingEnable());
                kobj.put(Config.PARAM_DETECT_SENSITIVITY_VALUE, DataPreference.getDetectSensitivity());
                kobj.put(Config.PARAM_DETECT_DISPLAY_ON_OFF_VALUE, DataPreference.getSecureDisplayEnable());
                kobj.put(Config.PARAM_DETECT_MODE_ON_OFF_VALUE, Config.MOTION_DETECT_MODE_ON);
                data.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
                data.put(Config.PARAM_FROM, json.getString(Config.PARAM_TO));
                data.put(Config.PARAM_TO, json.getString(Config.PARAM_FROM));
                data.put(Config.PARAM_SESSION_ID, json.getString(Config.PARAM_SESSION_ID));
                data.put(Config.PARAM_DESCRIPTION, json.getString(Config.PARAM_DESCRIPTION));
                data.put(Config.PARAM_CONFIG, kobj);
                mService.sendData(data.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startAd() {
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
//        AdRequest adRequest = new AdRequest.Builder().addTestDevice("57DCA262185892DD6E21498192076F49").build();
        adView.loadAd(adRequest);
    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                showProgress();
                registerServiceCallback();
                callCheckHandler();
                if (DataPreference.getMode() == Config.MODE_VIEWER) {
                    unCallHandler();
                    callHandler();
                    startAd();
                }

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private JSONObject getOfferData(JSONObject json) {
        Logger.d(TAG, "getOfferData call");
        try {
            json = json.getJSONObject(Config.PARAM_SDP);
            json.put(Config.PARAM_TYPE, Config.PARAM_OFFER);
            json.put(Config.PARAM_SESSION_ID, Long.toString(System.currentTimeMillis()));
            json.put(Config.PARAM_FROM, DataPreference.getRtcid());
            json.put(Config.PARAM_TO, DataPreference.getPeerRtcid());
            json.put(Config.PARAM_MODE, DataPreference.getViewerWillConnectMode());
            DataPreference.setOfferData(json.toString());
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getAnswerData(JSONObject json) {
        Logger.d(TAG, "getAnswerData call");
        try {
            json = json.getJSONObject(Config.PARAM_SDP);
            json.put(Config.PARAM_TYPE, Config.PARAM_ANSWER);
            json.put(Config.PARAM_SUB_TYPE, Config.PARAM_ACCEPT);
            json.put(Config.PARAM_SESSION_ID, mReceiveOfferData.optString(Config.PARAM_SESSION_ID));
            json.put(Config.PARAM_FROM, mReceiveOfferData.optString(Config.PARAM_FROM));
            json.put(Config.PARAM_TO, mReceiveOfferData.optString(Config.PARAM_TO));
            json.put(Config.PARAM_MODE, "");
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getCandidateData(JSONObject json) {
        Logger.d(TAG, "getCandidateData call");
        try {
            json.put(Config.PARAM_TYPE, Config.PARAM_CANDIDATE);
            if (isOfferUser()) {
                Logger.d(TAG, "isOfferUser true");
                JSONObject offerData = new JSONObject(DataPreference.getOfferSendData());
                json.put(Config.PARAM_SUB_TYPE, Config.PARAM_OFFER);
                json.put(Config.PARAM_SESSION_ID, offerData.getString(Config.PARAM_SESSION_ID));
                json.put(Config.PARAM_TO, offerData.getString(Config.PARAM_TO));
                json.put(Config.PARAM_FROM, offerData.getString(Config.PARAM_FROM));
            } else {
                Logger.d(TAG, "isOfferUser false");
                json.put(Config.PARAM_SUB_TYPE, Config.PARAM_ANSWER);
                json.put(Config.PARAM_SESSION_ID, mReceiveOfferData.getString(Config.PARAM_SESSION_ID));
                json.put(Config.PARAM_TO, mReceiveOfferData.getString(Config.PARAM_TO));
                json.put(Config.PARAM_FROM, mReceiveOfferData.getString(Config.PARAM_FROM));
            }

            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isOfferUser() {
        if (mReceiveOfferData == null) {
            return true;
        } else {
            return false;
        }
    }

    private JSONObject getCheckPeerCameraExist() {
        Logger.d(TAG, "getCheckPeerCameraExist call");
        try {
            JSONObject json = new JSONObject();
            json.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
            json.put(Config.PARAM_SESSION_ID, System.currentTimeMillis());
            json.put(Config.PARAM_FROM, DataPreference.getPeerRtcid());
            json.put(Config.PARAM_TO, DataPreference.getRtcid());
            json.put(Config.PARAM_DESCRIPTION, Config.PARAM_CHECK_CAMERA_PEER_EXIST);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getCheckPeerCameraExistForSecure() {
        Logger.d(TAG, "getCheckPeerCameraExistForSecure call");
        try {
            JSONObject json = new JSONObject();
            json.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
            json.put(Config.PARAM_SESSION_ID, System.currentTimeMillis());
            json.put(Config.PARAM_FROM, DataPreference.getPeerRtcid());
            json.put(Config.PARAM_TO, DataPreference.getRtcid());
            json.put(Config.PARAM_DESCRIPTION, Config.PARAM_CHECK_CAMERA_PEER_EXIST_FOR_SECURE);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getHangUpData() {
        Logger.d(TAG, "getHangUpData call");
        try {
            JSONObject json = new JSONObject();
            json.put(Config.PARAM_TYPE, Config.PARAM_HANGUP);

            if (DataPreference.getMode() == Config.MODE_VIEWER) {
                JSONObject offerData = new JSONObject(DataPreference.getOfferSendData());
                json.put(Config.PARAM_SESSION_ID, offerData.getString(Config.PARAM_SESSION_ID));
                json.put(Config.PARAM_FROM, offerData.getString(Config.PARAM_FROM));
                json.put(Config.PARAM_TO, offerData.getString(Config.PARAM_TO));
                json.put(Config.PARAM_SUB_TYPE, Config.PARAM_OFFER);
            } else {
                json.put(Config.PARAM_SESSION_ID, mReceiveOfferData.getString(Config.PARAM_SESSION_ID));
                json.put(Config.PARAM_FROM, mReceiveOfferData.getString(Config.PARAM_FROM));
                json.put(Config.PARAM_TO, mReceiveOfferData.getString(Config.PARAM_TO));
                json.put(Config.PARAM_SUB_TYPE, Config.PARAM_ANSWER);
            }

            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendBusyConfigAckData(JSONObject json, String description) {
        try {
            if (mService != null) {
                JSONObject data = new JSONObject();
                data.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
                data.put(Config.PARAM_FROM, json.getString(Config.PARAM_TO));
                data.put(Config.PARAM_TO, json.getString(Config.PARAM_FROM));
                data.put(Config.PARAM_SESSION_ID, json.getString(Config.PARAM_SESSION_ID));
                data.put(Config.PARAM_DESCRIPTION, description);
                mService.sendData(data.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getOfferAckData() {
        Logger.d(TAG, "getOfferAckData call");
        try {
            JSONObject json = new JSONObject();
            json.put(Config.PARAM_TYPE, Config.PARAM_OFFER_ACK);
            json.put(Config.PARAM_SESSION_ID, mReceiveOfferData.optString(Config.PARAM_SESSION_ID));
            json.put(Config.PARAM_FROM, mReceiveOfferData.optString(Config.PARAM_FROM));
            json.put(Config.PARAM_TO, mReceiveOfferData.optString(Config.PARAM_TO));
            json.put(Config.PARAM_CODE, Config.PARAM_SUCCESS_CODE);
            json.put(Config.PARAM_MODE, mReceiveOfferData.optString(Config.PARAM_MODE));
            json.put(Config.PARAM_DESCRIPTION, Config.PARAM_SUCCESS_DESCRIPTION);
            DataPreference.setOfferAckData(json.toString());
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getOfferBusyAckData() {
        Logger.d(TAG, "getOfferBusyAckData call");
        try {
            JSONObject json = new JSONObject();
            json.put(Config.PARAM_TYPE, Config.PARAM_OFFER_ACK);
            json.put(Config.PARAM_SESSION_ID, mReceiveOfferData.optString(Config.PARAM_SESSION_ID));
            json.put(Config.PARAM_FROM, mReceiveOfferData.optString(Config.PARAM_FROM));
            json.put(Config.PARAM_TO, mReceiveOfferData.optString(Config.PARAM_TO));
            json.put(Config.PARAM_CODE, Config.PARAM_PEER_IS_CALLING_CODE);
            json.put(Config.PARAM_MODE, mReceiveOfferData.optString(Config.PARAM_MODE));
            json.put(Config.PARAM_DESCRIPTION, Config.PARAM_PEER_IS_CALLING);
            DataPreference.setOfferAckData(json.toString());
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getAnswerAckData(String data) {
        Logger.d(TAG, "getAnswerAckData call");
        try {
            JSONObject receiveJson = new JSONObject(data);
            JSONObject json = new JSONObject();
            json.put(Config.PARAM_TYPE, Config.PARAM_ANSWER_ACK);
            json.put(Config.PARAM_SESSION_ID, receiveJson.getString(Config.PARAM_SESSION_ID));
            json.put(Config.PARAM_FROM, receiveJson.getString(Config.PARAM_FROM));
            json.put(Config.PARAM_TO, receiveJson.getString(Config.PARAM_TO));
            json.put(Config.PARAM_CODE, Config.PARAM_SUCCESS_CODE);
            json.put(Config.PARAM_MODE, receiveJson.getString(Config.PARAM_MODE));
            json.put(Config.PARAM_DESCRIPTION, Config.PARAM_SUCCESS_DESCRIPTION);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendSecureFinishData() {
        Logger.d(TAG, "sendSecureFinishData call");
        try {
            if (mService != null) {
                JSONObject data = new JSONObject();
                data.put(Config.PARAM_TYPE, Config.PARAM_GET_CONFIG_ACK);
                data.put(Config.PARAM_SESSION_ID, System.currentTimeMillis());
                data.put(Config.PARAM_FROM, DataPreference.getPeerRtcid());
                data.put(Config.PARAM_TO, DataPreference.getRtcid());
                data.put(Config.PARAM_DESCRIPTION, Config.PARAM_FINISH_SECURE);
                mService.sendData(data.toString());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    final IDataServiceCallback mCallbcak = new IDataServiceCallback.Stub() {

        @Override
        public void valueChanged(int value, String data) throws RemoteException {
            if (value == Config.HANDLER_MODE_OFFER) {
                try {
                    if (mReceiveOfferData != null) {
                        Logger.d(TAG, "mReceiveOfferData not null");
                        mService.sendData(getOfferBusyAckData().toString());
                        return;
                    } else {
                        startConnecting = true;
                        mReceiveOfferData = new JSONObject(data);
                        mService.sendData(getOfferAckData().toString());
                        DataPreference.setViewerWillConnectMode(Integer.parseInt(mReceiveOfferData.getString(Config.PARAM_MODE)));
                        JSONObject result = new JSONObject();

                        result.put(Config.PARAM_TYPE, mReceiveOfferData.optString(Config.PARAM_TYPE));
                        result.put(Config.PARAM_SDP, mReceiveOfferData.optString(Config.PARAM_SDP));

                        SessionDescription sessionDescription = SessionDescriptions.fromJsep(result);
                        mRtcSession = RtcSessions.create(mRtcConfig);
                        mRtcSession.setOnLocalCandidateListener(CameraActivity.this);
                        mRtcSession.setOnLocalDescriptionListener(CameraActivity.this);

                        mRtcSession.setRemoteDescription(sessionDescription);
                        mRtcSession.start(mStreamSet);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InvalidDescriptionException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            } else if (value == Config.HANDLER_MODE_OFFER_ACK) {
                try {
                    JSONObject json = new JSONObject(data);
                    if (!json.get(Config.PARAM_CODE).equals(Config.PARAM_SUCCESS_CODE)) {
                        if (json.get(Config.PARAM_DESCRIPTION).equals(Config.PARAM_PEER_IS_CALLING)) {
                            Logger.d(TAG, "상대방이 통화중입니다");
                        } else {
                            Logger.d(TAG, "상대방이 로그인 중이 아닙니다");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (value == Config.HANDLER_MODE_ANSWER) {
                cancelProgress();
                try {
                    JSONObject result = new JSONObject(data);
                    JSONObject json = new JSONObject();
                    json.put(Config.PARAM_TYPE, Config.PARAM_ANSWER);
                    json.put(Config.PARAM_SDP, result.optString(Config.PARAM_SDP));
                    SessionDescription sessionDescription = SessionDescriptions.fromJsep(json);
                    Logger.d(TAG, "mRtcSession == " + mRtcSession);
                    mRtcSession.setRemoteDescription(sessionDescription);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mService.sendData(getAnswerAckData(data).toString());
                getAnswer = true;
                for (int i = 0; i < mCandidates.size(); i++) {
                    mService.sendData(mCandidates.get(i).toString());
                }
                if (DataPreference.getMode() == Config.MODE_VIEWER) {
                    mDimViewLayout.setVisibility(View.GONE);

                    if (DataPreference.getViewerWillConnectMode() == Config.MODE_CCTV) {
                        mSelfViewDimLayout.setVisibility(View.VISIBLE);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 10f);
                        mRemoteViewParent = (RelativeLayout) findViewById(R.id.remote_view_parent);
                        mRemoteViewParent.setLayoutParams(params);
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Logger.d(TAG, "상대방이 cctv 입니다. ");
                                setMicMute(true);
                                setSpeakerSound(false);
                                setSpeakerPhone(false);
                            }
                        }, 2000);

                    } else if (DataPreference.getViewerWillConnectMode() == Config.MODE_BABY_TALK) {
                        mSelfViewDimLayout.setVisibility(View.GONE);
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Logger.d(TAG, "상대방이 baby 입니다. ");
                                setMicMute(false);
                                setSpeakerSound(true);
                                setSpeakerPhone(true);
                            }
                        }, 2000);

                    }
                }
                checkBufferDataIfErrorThenFinish();
            } else if (value == Config.HANDLER_MODE_ANSWER_ACK) {
                cancelProgress();
                getAnswerAck = true;
                checkFinish = false;
                for (int i = 0; i < mCandidates.size(); i++) {
                    mService.sendData(mCandidates.get(i).toString());
                }
                if (DataPreference.getViewerWillConnectMode() == Config.MODE_CCTV) {
                    mDimViewLayout.setVisibility(View.VISIBLE);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Logger.d(TAG, "내가 cctv 일때 mute  ");
                            setMicMute(true);
                            setSpeakerSound(false);
                        }
                    }, 2000);

                } else if (DataPreference.getViewerWillConnectMode() == Config.MODE_BABY_TALK) {
                    mDimViewLayout.setVisibility(View.GONE);
                    mSelfViewDimLayout.setVisibility(View.GONE);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Logger.d(TAG, "내가 baby 일때 mute  ");
                            setMicMute(false);
                            setSpeakerSound(true);
                            setSpeakerPhone(true);
                        }
                    }, 2000);
                }
                checkBufferDataIfErrorThenFinish();
            } else if (value == Config.HANDLER_MODE_CANDIDATE) {
                try {
                    JSONObject json = new JSONObject(data);
                    JSONObject candidate = json.optJSONObject(Config.PARAM_CANDIDATE);
                    RtcCandidate rtcCandidate = RtcCandidates.fromJsep(candidate);
                    mRtcSession.addRemoteCandidate(rtcCandidate);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                startConnecting = false;

            } else if (value == Config.HANDLER_MODE_CONFIG_ACK) {
                try {
                    JSONObject json = new JSONObject(data);
                    String description = json.getString(Config.PARAM_DESCRIPTION);
                    if (description.equals(Config.PARAM_CHECK_CAMERA_PEER_EXIST)) {
                        if (DataPreference.getMode() == Config.MODE_VIEWER) {
                            unCallHandler();
                            startConnect(DataPreference.getMode());
                        } else {
                            if (mReceiveOfferData != null) { //통화중일때
                                Logger.d(TAG, "mReceiveOfferData not null calling");
                                sendBusyConfigAckData(json, Config.PARAM_PEER_IS_CALLING);
                            } else if (DataPreference.getSecuringMode()) {
                                Logger.d(TAG, "securing mode ");
                                sendBusyConfigAckData(json, Config.PARAM_PEER_IS_SECURING);
                            } else {
                                startConnect(DataPreference.getMode());
                                sendPeerCameraExist(json);
                            }
                        }
                    } else if (description.equals(Config.PARAM_CHECK_CAMERA_PEER_EXIST_FOR_SECURE)) {
                        if (DataPreference.getMode() == Config.MODE_VIEWER) {
                            unCallHandler();
                            cancelProgress();
                            secureSettingViewVisible(true, json);
                        } else {
                            if (mReceiveOfferData != null) { //통화중일때
                                Logger.d(TAG, "mReceiveOfferData not null");
                                sendBusyConfigAckData(json, Config.PARAM_PEER_IS_CALLING);
                            } else {
                                if (DataPreference.getSecuringMode()) {
                                    sendPeerCameraExistForSecure(json);
                                } else {
                                    sendPeerCameraExistForSecure(json);
                                    Intent intent = new Intent(CameraActivity.this, SecureActivity.class);
                                    startActivity(intent);
                                }

                            }
                        }
                    } else if (description.equals(Config.PARAM_FINISH_SECURE)) {
                        if (DataPreference.getMode() == Config.MODE_VIEWER) {
                            finish();
                        } else {
                            sendBroadcast(new Intent(Config.BROADCAST_FINISH_SECURE));
                        }
                    } else if (description.equals(Config.PARAM_SECURE_CHANGE_OPTION)) {
                        try {
                            JSONObject config = json.getJSONObject(Config.PARAM_CONFIG);
                            DataPreference.setDetectSensitivity(config.getString(Config.PARAM_DETECT_SENSITIVITY_VALUE));
                            DataPreference.setVideoRecordingEnable(config.getString(Config.PARAM_VIDEO_ON_OFF_VALUE));
                            DataPreference.setSecureDisplayEnable(config.getString(Config.PARAM_DETECT_DISPLAY_ON_OFF_VALUE));
                            sendBroadcast(new Intent(Config.BROADCAST_CHANGE_SECURE_OPTION));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (description.equals(Config.PARAM_SECURE_IMAGE_REQUEST)) {
                        if (DataPreference.getMode() != Config.MODE_VIEWER) {
                            JSONObject config = json.getJSONObject(Config.PARAM_CONFIG);
                            String time = config.getString(Config.PARAM_IMAGE_TIME_KEY);
                            String path = getDetectImageFileFromDatabase(config.getString(Config.PARAM_IMAGE_TIME_KEY));
                            Logger.d(TAG, "path " + path);
                            Logger.d(TAG, "time " + time);
                            if (path == null) {
                                return;
                            }
                            String sendData = makeJsonDataWithDetectImageFileAndInfo(path, time);
                            if (sendData != null) {
                                mService.sendData(sendData);
                            }
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (value == Config.HANDLER_MODE_HANGUP) {
                try {
                    JSONObject json = new JSONObject(data);
                    JSONObject result = new JSONObject();
                    result.put(Config.PARAM_TYPE, Config.PARAM_HANGUP_ACK);
                    result.put(Config.PARAM_SUB_TYPE, json.get(Config.PARAM_SUB_TYPE));
                    result.put(Config.PARAM_SESSION_ID, json.get(Config.PARAM_SESSION_ID));
                    result.put(Config.PARAM_FROM, json.get(Config.PARAM_FROM));
                    result.put(Config.PARAM_TO, json.get(Config.PARAM_TO));
                    result.put(Config.PARAM_CODE, Config.PARAM_SUCCESS_CODE);
                    mService.sendData(result.toString());
                    sendHangupAck = true;
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (value == Config.HANDLER_MODE_HANGUP_ACK) {

            }
        }
    };

    private String makeJsonDataWithDetectImageFileAndInfo(String fileFullPath, String time) {
        JSONObject json = new JSONObject();
        try {
            json.put(Config.PARAM_TYPE, Config.PARAM_EVENT);
            json.put(Config.PARAM_SESSION_ID, System.currentTimeMillis());
            json.put(Config.PARAM_FROM, DataPreference.getRtcid());
            json.put(Config.PARAM_TO, DataPreference.getPeerRtcid());
            json.put(Config.PARAM_FILE_PATH, fileFullPath);
            json.put(Config.PARAM_DESCRIPTION, Config.PARAM_SECURE_IMAGE_REQUEST);
            json.put(Config.PARAM_TIME, time);
            File file = new File(fileFullPath);
            JSONObject json2 = new JSONObject();
            if (file.exists()) {
                String data = Config.getByteStringForSecureImage(new File(fileFullPath));
                json2.put(Config.PARAM_IMAGE_CUT, data);
            }
            json.put(Config.PARAM_EVENT, json2);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getDetectImageFileFromDatabase(String time) {
        String path = null;
        ContentResolver resolver = getContentResolver();
        Cursor c = resolver.query(CamProvider.MOTION_IMAGE_TABLE_URI, new String[]{CamSQLiteHelper.COL_FILE_PATH},
                CamSQLiteHelper.COL_DATE + " = ? ", new String[]{time}, CamSQLiteHelper.COL_DATE + " desc");
        if (c != null && c.moveToFirst()) {
            try {
                do {
                    path = c.getString(c.getColumnIndex(CamSQLiteHelper.COL_FILE_PATH));

                } while (c.moveToNext());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }
        return path;
    }

    @Override
    public void onLocalDescription(SessionDescription localDescription) {
        Logger.d(TAG, "sun onLocalDescription call ");
        try {
            JSONObject json = new JSONObject();
            json.putOpt(Config.PARAM_SDP, SessionDescriptions.toJsep(localDescription));
            if (DataPreference.getMode() == Config.MODE_VIEWER) {
                JSONObject result = getOfferData(json);
                if (result != null) {
                    mService.sendData(result.toString());
                }
            } else {
                JSONObject result = getAnswerData(json);
                if (result != null) {
                    mService.sendData(result.toString());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocalCandidate(RtcCandidate candidate) {
        Logger.d(TAG, "sun onLocalCandidate call ");
        try {
            JSONObject json = new JSONObject();
            json.putOpt(Config.PARAM_CANDIDATE, RtcCandidates.toJsep(candidate));
            json.getJSONObject(Config.PARAM_CANDIDATE).put(SDPMID, VIDEO);
            JSONObject result = getCandidateData(json);
            if (result != null) {
                if (!isOfferUser()) {
                    if (!getAnswer) {
                        mCandidates.add(result);
                    } else {
                        mService.sendData(result.toString());
                    }
                } else {
                    if (!getAnswerAck) {
                        mCandidates.add(result);
                    } else {
                        mService.sendData(result.toString());
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public class CheckBufferHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            checkBufferDataIfErrorThenFinish();

        }
    }

    private void checkBufferDataIfErrorThenFinish() {
        Logger.d(TAG, "checkBufferDataIfErrorThenFinish call ");
        if (mRemoteView != null) {
            if (remoteViewTimestemp == mRemoteView.checkVideoView()) {
                checkBuffferDataCount++;
            } else {
                checkBuffferDataCount = 0;
            }
            remoteViewTimestemp = mRemoteView.checkVideoView();
        }
        if (checkBuffferDataCount > 3) {
            finish();
            return;
        }
        mCheckBufferHandler.postDelayed(mCheckBufferRunnable, 5000);
    }

    private Runnable mCheckBufferRunnable = new Runnable() {
        @Override
        public void run() {
            mCheckBufferHandler.sendEmptyMessage(0);
        }
    };

    private JSONObject getCheckData() {
        JSONObject jsono = new JSONObject();
        try {
            jsono.put(Config.PARAM_TYPE, Config.PARAM_CHECK);
            jsono.put(Config.PARAM_SESSION_ID, String.valueOf(System.currentTimeMillis()));
            jsono.put(Config.PARAM_RTCID, DataPreference.getRtcid());
            jsono.put(Config.PARAM_UUID, Build.SERIAL);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsono;
    }

    public void disconnectSession() {
        Logger.d(TAG, "disconnectSession call ");
        mStreamSet = null;
        if (mRtcSession != null) {
            mRtcSession.stop();
            mRtcSession = null;
        }
        if (DataPreference.getMode() != Config.MODE_VIEWER) {
            mReceiveOfferData = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_photo_save:
                saveBitmapToFileCache(CameraActivity.this, mRemoteView.getView().getBitmap(), Config.getSaveImageFileExternalDirectory(), getPictureFileName());
                break;

            case R.id.btn_back:
                finish();
                break;
        }
    }

    public String getPictureFileName() {
        Date now = new Date(System.currentTimeMillis());
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String formattedDate = df.format(now);
        String fileExtention = ".jpg";
        return "durian_" + formattedDate + fileExtention;
    }

    public void saveBitmapToFileCache(Context context, Bitmap bitmap, String strFilePath, String strFileName) {
        File filepath = new File(strFilePath);

        if (!filepath.exists()) {
            filepath.mkdirs();
        }
        File fileCacheItem = new File(strFilePath + strFileName);
        OutputStream out = null;
        try {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(context, context.getResources().getText(R.string.save_image_into_gallery), Toast.LENGTH_SHORT).show();

        new MediaScanner(context, new File(strFilePath + strFileName));

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int i, int i1) {
        Logger.d(TAG, "onSurfaceTextureAvailable call ");
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        Logger.d(TAG, "onSurfaceTextureSizeChanged call ");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Logger.d(TAG, "onSurfaceTextureDestroyed call ");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        Logger.d(TAG, "onSurfaceTextureUpdated call ");
    }
}
