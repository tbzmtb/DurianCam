package kr.durian.duriancam.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.WindowManager;

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
import java.util.List;
import java.util.concurrent.Exchanger;

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
        RtcSession.OnLocalDescriptionListener {
    private IDataService mService;
    private final String TAG = getClass().getName();
    private AudioManager mAudioManager;
    private RtcConfig mRtcConfig;
    private RtcSession mRtcSession;
    private SimpleStreamSet mStreamSet;
    private VideoView mSelfView;
    private VideoView mRemoteView;
    private JSONObject mReceiveOfferData;
    private final String SDPMID = "sdpMid";
    private final String VIDEO = "video";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "CameraActivity oncreate call");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setWindowFrags();
//        setMute();
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void setMute() {
        if (mAudioManager == null) {
            Logger.d(TAG, "mAudioManager = " + mAudioManager);
            return;
        }
        mAudioManager.setMicrophoneMute(true);

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


    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "CameraActivity onPause Call");


    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, "CameraActivity onResume Call");

    }

    protected void onDestroy() {
        super.onDestroy();
        disconnect();
        closeWebSocket();
        finish();
        unbindService(mConnection);
//        android.os.Process.killProcess((android.os.Process.myPid()));
    }

//    public void registerRestartAlarm() {
//        Logger.d(TAG, "registerRestartAlarm call");
//        Intent intent = new Intent(this, BootReceiver.class);
//        intent.setAction("ACTION.RESTART.PersistentService");
//        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
//        long firstTime = SystemClock.elapsedRealtime();
//        firstTime += 500;
//        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
//        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, sender);
//    }

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
                try {
                    mService.registerCallback(mCallbcak);
                    startConnect(DataPreference.getMode());


                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            if (mService != null) {
                try {
                    mService.unregisterCallback(mCallbcak);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
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
            json.put(Config.PARAM_MODE, "PET");
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
            json.put(Config.PARAM_MODE, mReceiveOfferData.optString(Config.PARAM_MODE));
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
                mService.sendData(getAnswerAckData(data).toString());
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

            } else if (value == Config.HANDLER_MODE_CANDIDATE) {
                try {
                    JSONObject json = new JSONObject(data);
                    JSONObject candidate = json.optJSONObject(Config.PARAM_CANDIDATE);
                    RtcCandidate rtcCandidate = RtcCandidates.fromJsep(candidate);
                    mRtcSession.addRemoteCandidate(rtcCandidate);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
                mService.sendData(result.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        Logger.d(TAG, "disconnect call ");
        mStreamSet = null;
        if (mRtcSession != null) {
            mRtcSession.stop();
            mRtcSession = null;
        }
        if (DataPreference.getMode() == Config.MODE_CAMERA) {
            mReceiveOfferData = null;
        }
    }
}
