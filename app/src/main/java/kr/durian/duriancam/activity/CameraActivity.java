package kr.durian.duriancam.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;

import com.ericsson.research.owr.sdk.RtcCandidate;
import com.ericsson.research.owr.sdk.RtcSession;
import com.ericsson.research.owr.sdk.SessionDescription;

import kr.durian.duriancam.R;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.service.IDataServiceCallback;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.Logger;

/**
 * Created by kimsunyung on 16. 7. 8..
 */
public class CameraActivity extends AppCompatActivity implements
        RtcSession.OnLocalCandidateListener,
        RtcSession.OnLocalDescriptionListener{
    private IDataService mService;
    private final String TAG = getClass().getName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_camera);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "CameraActivity onPause Call");
        unbindService(mConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d(TAG, "CameraActivity onResume Call");
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                try {
                    mService.registerCallback(mCallbcak);
                    mService.sendOffer();
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

    final IDataServiceCallback mCallbcak = new IDataServiceCallback.Stub() {

        @Override
        public void valueChanged(long value) throws RemoteException {

        }
    };

    @Override
    public void onLocalDescription(SessionDescription localDescription) {

    }

    @Override
    public void onLocalCandidate(RtcCandidate candidate) {

    }
}
