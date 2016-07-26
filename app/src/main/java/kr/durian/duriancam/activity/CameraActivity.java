package kr.durian.duriancam.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ericsson.research.owr.HelperServerType;
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
import java.util.Collection;
import java.util.Date;

import kr.durian.duriancam.R;
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
    private boolean startConnect = false;
    private long remoteViewTimestemp = 0;
    private int checkBuffferDataCount = 0;
    private AudioManager mAudioManager;
    private ImageView mBtnPhoto;
    private CheckBufferHandler mCheckBufferHandler;

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
        mBtnPhoto = (ImageView) findViewById(R.id.btn_photo_save);
        mBtnPhoto.setOnClickListener(this);
        mDimViewLayout = (RelativeLayout) findViewById(R.id.dim_view_layout);
        mSelfViewDimLayout = (RelativeLayout) findViewById(R.id.self_view_dim_layout);
        mRemoteViewParent = (RelativeLayout) findViewById(R.id.remote_view_parent);

        setMicMute(false);
        setSpeakerSound(true);
        setSpeakerPhone(false);
        mCheckBufferHandler = new CheckBufferHandler();
        mHandler = new CheckPeerCameraHandler();
        mCheckHandler = new CheckHandler();
        setWindowFrags();
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
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
        mAudioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, enable);
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, enable);
        mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, enable);

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
        Logger.d(TAG, "sun startConnect call");
        startConnect = true;
        if (mStreamSet != null) {
            Logger.d(TAG, "error mStreamSet == not null");
            return;
        }
        mRtcConfig = RtcConfigs.defaultConfig(Config.STUN_SERVER, Config.TURN_SERVER);
//        mRtcConfig = RtcConfigs.defaultConfig(getHelperSever(Config.STUN_SERVER, Config.TURN_SERVER));
        mStreamSet = SimpleStreamSet.defaultConfig(true, true);
        mSelfView = CameraSource.getInstance().createVideoView();
        mRemoteView = mStreamSet.createRemoteView();

        TextureView remoteView = (TextureView) findViewById(R.id.remote_view);
        mRemoteView.setView(remoteView);

        TextureView selfView = (TextureView) findViewById(R.id.self_view);
        selfView.setSurfaceTextureListener(this);
        mSelfView.setView(selfView);
//        mSelfView.setMirrored(true);

        if (mode == Config.MODE_VIEWER) {
            mRtcSession = RtcSessions.create(mRtcConfig);
            mRtcSession.setOnLocalCandidateListener(CameraActivity.this);
            mRtcSession.setOnLocalDescriptionListener(CameraActivity.this);
            mRtcSession.start(mStreamSet);
            Logger.d(TAG, "mRtcSession init complete");
        }

    }

    private Collection<RtcConfig.HelperServer> getHelperSever(String stunServerUrl, String tunnServerUrl) {
        ArrayList<RtcConfig.HelperServer> mHelperServers = new ArrayList<>();

        String[] split = stunServerUrl.split(":");
        if (split.length < 1 || split.length > 2) {
            throw new IllegalArgumentException("invalid stun server url: " + stunServerUrl);
        }
        final int stun_port;
        if (split.length == 2) {
            stun_port = Integer.parseInt(split[1]);
        } else {
            stun_port = 3478;
        }
        mHelperServers.add(new RtcConfig.HelperServer(HelperServerType.STUN, split[0], stun_port, "gorst", "hero"));

        split = tunnServerUrl.split(":");
        if (split.length < 1 || split.length > 5) {
            throw new IllegalArgumentException("invalid stun server url: " + tunnServerUrl);
        }
        final int turn_port;
        if (split.length >= 2) {
            turn_port = Integer.parseInt(split[1]);
        } else {
            turn_port = 3478;
        }
        final String username;
        if (split.length >= 3) {
            username = split[2];
        } else {
            username = "";
        }
        final String password;
        if (split.length >= 4) {
            password = split[3];
        } else {
            password = "";
        }

        mHelperServers.add(new RtcConfig.HelperServer(HelperServerType.TURN_TCP, split[0], turn_port, username, password));
        return mHelperServers;
    }

    private void showProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
            mCheckHandler.postDelayed(mCheckRunnable, 60000 * 30);

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
            checkPeerCameraExist();
            callHandler();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, "CameraActivity onResume Call");

    }


    @Override
    public void finish() {
        Logger.d(TAG, "finish call");
        if (startConnect) {
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
            return;
        } else {
            cancelProgress();
            sendHangupData();
            disconnectSession();
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
                        startConnect = true;
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
                startConnect = false;

            } else if (value == Config.HANDLER_MODE_CONFIG_ACK) {
                try {
                    JSONObject json = new JSONObject(data);
                    String description = json.getString(Config.PARAM_DESCRIPTION);
                    if (description.equals(Config.PARAM_CHECK_CAMERA_PEER_EXIST)) {
                        if (DataPreference.getMode() == Config.MODE_VIEWER) {
                            unCallHandler();
                            startConnect(DataPreference.getMode());
                        } else {
                            if (mReceiveOfferData != null) {
                                Logger.d(TAG, "mReceiveOfferData not null");
                                mService.sendData(getOfferBusyAckData().toString());
                            } else {
                                startConnect(DataPreference.getMode());
                                sendPeerCameraExist(json);
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
        if (checkBuffferDataCount > 2) {
            finish();
            return;
        }
        mCheckBufferHandler.postDelayed(mCheckBufferRunnable, 4000);
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
                saveBitmapToFileCache(CameraActivity.this, mRemoteView.getView().getBitmap(), getSaveImageFileExternalDirectory(), getPictureFileName());
                break;
        }
    }

    public static String getSaveImageFileExternalDirectory() {
        File fileRoot = new File(Environment.getExternalStorageDirectory() + File.separator + "Durian");
        if (!fileRoot.exists()) {
            fileRoot.mkdir();
        }
        return fileRoot.getPath() + File.separator;
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
//        assert texture != null;
//        Surface previewSurface = new Surface(texture);
//
//        MediaRecorder mMediaRecorder = new MediaRecorder();
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mMediaRecorder.setOutputFile(getVideoFilePath());
//        mMediaRecorder.setVideoEncodingBitRate(10000000);
//        mMediaRecorder.setVideoFrameRate(30);
//        mMediaRecorder.setVideoSize(640, 360);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
////        mMediaRecorder.setOrientationHint();
//        try {
//            mMediaRecorder.prepare();
//        }catch (IOException e){
//            e.printStackTrace();
//        }
//        List<Surface> surfaces = new ArrayList<>();
//        Surface mRecorderSurface = mMediaRecorder.getSurface();
//        surfaces.add(mRecorderSurface);
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
