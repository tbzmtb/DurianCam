package kr.durian.duriancam.service;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

/**
 * Created by tbzm on 16. 4. 18.
 */
public class DataService extends Service {

    private static final String TAG = "DataService";
    private WebSocketClient mWebSocketClient;

    final RemoteCallbackList<IDataServiceCallback> callbacks = new RemoteCallbackList();

    private final IDataService.Stub mBinder = new IDataService.Stub() {
        @Override
        public void connectWebSocket() throws RemoteException {
            connectWebSocketInService();
        }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public boolean unregisterCallback(IDataServiceCallback callback)
                throws RemoteException {
            boolean flag = false;

            if (callback != null) {
                flag = unregisterCallback(callback);
            }

            return flag;
        }

        @Override
        public boolean registerCallback(IDataServiceCallback callback)
                throws RemoteException {


            boolean flag = false;

            if (callback != null) {
                flag = callbacks.register(callback);
            }

            return flag;
        }

        @Override
        public void sendOffer() throws RemoteException {
            sendOfferInService();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;

    }


    @Override
    public void onCreate() {
        Logger.d(TAG, "Service is onCreate..");
        super.onCreate();
    }

    private void connectWebSocketInService() {
        Logger.d(TAG, "connectWebSocketInServer");
        URI uri;
        try {
            uri = new URI(Config.WEB_SOCKET_URL);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
//        closeWebsocket();
        HashMap<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Sec-WebSocket-Protocol", "ns-rtc");
        Logger.d(TAG, "mWebSocketClient == start ");

        mWebSocketClient = new WebSocketClient(uri, new Draft_17(), httpHeaders, 0) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Logger.e(TAG, "mWebSocketClient onOpen = " + handshakedata.toString());
                sendMessageLogin();
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                Logger.e(TAG, "mWebSocketClient onMessage bytes =" + bytes.toString());
                super.onMessage(bytes);
            }

            @Override
            public void onMessage(String message) {
                Logger.e(TAG, "mWebSocketClient onMessage = " + message);
                try {
                    JSONObject jsono = new JSONObject(message);
                    switch (jsono.get(Config.PARAM_TYPE).toString()) {
                        case Config.SOCKET_MESSAGE_LOGIN_ACK: {
                            Message msg = Message.obtain();
                            msg.what = Config.MODE_START;
                            handler.sendMessage(msg);
                            break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Logger.e(TAG, "mWebSocketClient onClose = " + code);
                Logger.e(TAG, "mWebSocketClient onClose = " + reason);
                Logger.e(TAG, "mWebSocketClient onClose = " + remote);
            }

            @Override
            public void onError(Exception ex) {
                Logger.e(TAG, "mWebSocketClient onError = " + ex);
            }
        };

        mWebSocketClient.connect();

    }

    public void sendMessageLogin() {
        String loginId = DataPreference.getRtcid();
        try {
            JSONObject jsono = new JSONObject();
            jsono.put(Config.SERVER_PARAM_DEVICE_TYPE, Config.DEVICE_TYPE_ANDROID_VALUE);
            jsono.put(Config.PARAM_UUID, Build.SERIAL);
            jsono.put(Config.SERVER_PARAM_VERSION, Config.getAppVersionName(this));
            jsono.put(Config.PARAM_RTCID, loginId);
            jsono.put(Config.PARAM_TYPE, Config.SERVER_PARAM_LOGIN);
            jsono.put(Config.PARAM_PASSWORD, "");
            sendMessageWithWebSocket(jsono.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendOfferInService(){

    }

    private void sendMessageWithWebSocket(String json) {
        Logger.w("!!!", "sendMessageWithWebSocket json = " + json);
        try {
            mWebSocketClient.send(json);
        } catch (WebsocketNotConnectedException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "Service is onDestory");
        closeWebsocket();
        super.onDestroy();
    }

    private void closeWebsocket(){
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
            mWebSocketClient = null;
        }
    }

    private Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case Config.MODE_START: {
                    int N = callbacks.beginBroadcast();
                    for (int i = 0; i < N; i++) {
                        try {
                            IDataServiceCallback cb = callbacks.getBroadcastItem(i);
                            cb.valueChanged(Config.MODE_START);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    callbacks.finishBroadcast();
                    break;
                }
                default:
                    break;
            }
            return false;
        }
    });

}
