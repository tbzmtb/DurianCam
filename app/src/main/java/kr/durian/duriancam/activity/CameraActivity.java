package kr.durian.duriancam.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

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

import java.util.ArrayList;

import kr.durian.duriancam.R;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.service.IDataServiceCallback;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

/**
 * Created by kimsunyung on 16. 7. 8..
 */
public class CameraActivity extends AppCompatActivity implements
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
        mDimViewLayout = (RelativeLayout) findViewById(R.id.dim_view_layout);
        mSelfViewDimLayout = (RelativeLayout) findViewById(R.id.self_view_dim_layout);
        setMicMute(false);
        setSpeakerSound(true);
        setSpeakerPhone(false);
        mHandler = new CheckPeerCameraHandler();
        mCheckHandler = new CheckHandler();
        setWindowFrags();
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void setMicMute(boolean mute) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMicrophoneMute(mute);

    }

    private void setSpeakerSound(boolean enable) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_VOICE_CALL, enable);
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, enable);
        audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, enable);
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
            mCheckHandler.postDelayed(mCheckRunnable, 60000*30);

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
        mDimViewLayout.setVisibility(View.VISIBLE);
        mSelfViewDimLayout.setVisibility(View.VISIBLE);
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
            json.put(Config.PARAM_MODE, "");
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
            json.put(Config.PARAM_MODE, String.valueOf(DataPreference.getMode()));
            DataPreference.setOfferData(json.toString());
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
                        mReceiveOfferData = new JSONObject(data);
                        mService.sendData(getOfferAckData().toString());
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
                    String mode = result.getString(Config.PARAM_MODE);
                    DataPreference.setPeerMode(Integer.parseInt(mode));
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

                    if (DataPreference.getPeerMode() == Config.MODE_CCTV) {
                        mSelfViewDimLayout.setVisibility(View.VISIBLE);
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

                    } else if (DataPreference.getPeerMode() == Config.MODE_BABY_TALK) {
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
            } else if (value == Config.HANDLER_MODE_ANSWER_ACK) {
                cancelProgress();
                getAnswerAck = true;
                checkFinish = false;
                for (int i = 0; i < mCandidates.size(); i++) {
                    mService.sendData(mCandidates.get(i).toString());
                }
                if (DataPreference.getMode() == Config.MODE_CCTV) {
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

                } else if (DataPreference.getMode() == Config.MODE_BABY_TALK) {
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
            } else if (value == Config.HANDLER_MODE_CANDIDATE) {
                try {
                    JSONObject json = new JSONObject(data);
                    JSONObject candidate = json.optJSONObject(Config.PARAM_CANDIDATE);
                    RtcCandidate rtcCandidate = RtcCandidates.fromJsep(candidate);
                    mRtcSession.addRemoteCandidate(rtcCandidate);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (value == Config.HANDLER_MODE_CONFIG_ACK) {
                try {
                    JSONObject json = new JSONObject(data);
                    String description = json.getString(Config.PARAM_DESCRIPTION);
                    if (description.equals(Config.PARAM_CHECK_CAMERA_PEER_EXIST)) {
                        if (DataPreference.getMode() == Config.MODE_VIEWER) {
                            unCallHandler();
                            startConnect(DataPreference.getMode());
                        } else {
                            startConnect(DataPreference.getMode());
                            sendPeerCameraExist(json);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (value == Config.HANDLER_MODE_HANGUP) {
                Logger.d(TAG, "이게 왜 호출되지 data = " + data);
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

    }
}
