package kr.durian.duriancam.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

import kr.durian.duriancam.R;
import kr.durian.duriancam.service.DataService;
import kr.durian.duriancam.service.IDataService;
import kr.durian.duriancam.service.IDataServiceCallback;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

/**
 * Created by sunyungkim on 16. 7. 26..
 */
public class SecureActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private IDataService mService;
    private final String TAG = getClass().getName();
    private JavaCameraView mOpenCVCameraView;
    private BackgroundSubtractorMOG2 mBgSubtractor;
    private Mat mRgb;
    private Mat mFGMask;
    private Mat mGray;
    private List<MatOfPoint> mContours;
    private ArrayList<Integer> mSaveContoursData;
    private double mLearningRate = 0.5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "SecureActivity oncreate call");
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
        setContentView(R.layout.activity_secure);
        bindService(new Intent(this,
                DataService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mContours.clear();
        mGray = inputFrame.gray();
        Imgproc.cvtColor(mGray, mRgb, Imgproc.COLOR_GRAY2RGB);
        mBgSubtractor.apply(mRgb, mFGMask, mLearningRate);
        Imgproc.erode(mFGMask, mFGMask, new Mat());
        Imgproc.dilate(mFGMask, mFGMask, new Mat());
        Imgproc.findContours(mFGMask, mContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        Logger.d(TAG, "mContours.size = " + mContours.size());
        return mRgb;
    }

    @Override
    public void onCameraViewStopped() {

    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        Logger.d(TAG, "onCameraViewStarted");
        mBgSubtractor = Video.createBackgroundSubtractorMOG2();
        mRgb = new Mat();
        mFGMask = new Mat();
        mGray = new Mat();
        mContours = new ArrayList<MatOfPoint>();
        mSaveContoursData = new ArrayList<>();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        unbindService(mConnection);
        unregisterServiceCallback();
        release();
        super.finish();
    }

    private void release() {
        if (mOpenCVCameraView != null) {
            mOpenCVCameraView.stopRecording();
            mOpenCVCameraView.disableView();
            mOpenCVCameraView = null;
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
        public void valueChanged(int what, String data) throws RemoteException {

        }
    };

    private void initCamera() {
        Logger.i(TAG, "initCamera mOpenCVCameraView = " + mOpenCVCameraView);
        if (mOpenCVCameraView == null) {
            mOpenCVCameraView = (JavaCameraView) findViewById(R.id.surface_camera);
            mOpenCVCameraView.setCvCameraViewListener(SecureActivity.this);
//            mOpenCVCameraView.setJavaCamViewListener(onMediaListner);
            mOpenCVCameraView.enableView();
        }
    }

//    private JavaCameraView.OnJavaCamViewListener onMediaListner = new JavaCameraView.OnJavaCamViewListener() {
//        @Override
//        public void onPictureTaken(final String file, final String time, final String err) {
//            if (err.equals("sdcard_error")) {
//                Logger.i(TAG, "sdcard_error == ");
//                finish();
//                return;
//            }
//            if (file == null || file.equals("")) {
//                Logger.i(TAG, "file == " + file);
//                finish();
//                return;
//            }
//            Logger.i(TAG, "photo file == " + file);
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    setData2Database(OuiBotPreferences.getLoginId(SecureActivity.this), file, String.valueOf(getCurrentMode()), time);
//                    startMediaScanner(file);
//                    sendFile2Master(file, getCurrentMode(), time);
//                }
//            }).start();
//            reStartCameraORVideoRecording();
//        }
//
//        //영상촬영 후 다시 시작하기 위함(영상 촬영중 SecureAcitivty 가 종료된 경우도 호출됨)
//        //시작 카운트 다운 시 onRecordingStop이 호출 될 수 있음에 유의
//        @Override
//        public void onRecordingStop(final String file) {
//            Logger.i(TAG, "onRecordingStop call == " + file);
//            isRecording = false;
//            startMediaScanner(file);
//            setOverLaySubText("");
//            if (SecurePreference.getDetectOnOff().equals(Config.DETECT_ON)) {
//                mOpenCVCameraView.reconnectCamera();
//                reStartSecureTimer(mDetectMode, RETRY_DETECT_TIME, mNoneActivityCheckTime);
//            }
//        }
//
//        @Override
//        public void onRecordingError() {
//            Logger.i(TAG, "onRecordingError call !!!!");
//            finish();
//        }
//    };


    ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                mService = IDataService.Stub.asInterface(service);
                registerServiceCallback();
                if (!OpenCVLoader.initDebug()) {
                    OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, SecureActivity.this, mLoaderCallback);
                } else {
                    mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                }

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Logger.i(TAG, "OpenCV Manager Connected");
                    initCamera();
                    break;
                case LoaderCallbackInterface.INIT_FAILED:
                    Logger.i(TAG, "Init Failed");
                    break;
                case LoaderCallbackInterface.INSTALL_CANCELED:
                    Logger.i(TAG, "Install Cancelled");
                    break;
                case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
                    Logger.i(TAG, "Incompatible Version");
                    break;
                case LoaderCallbackInterface.MARKET_ERROR:
                    Logger.i(TAG, "Market Error");
                    break;
                default:
                    Logger.i(TAG, "OpenCV Manager Install");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
}
