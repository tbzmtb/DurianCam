package org.opencv.android;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class JavaCameraView extends CameraBridgeViewBase implements PreviewCallback, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "JavaCameraView";
    private byte mBuffer[];
    private Mat[] mFrameChain;
    private int mChainIdx = 0;
    private Thread mThread;
    private boolean mStopThread;
    private String mStillcutFilePath = "";
    private String mStillcutTime = "";
    private OnJavaCamViewListener mCameraViewListener = null;
    protected Camera mCamera;
    protected JavaCameraFrame[] mCameraFrame;
    private SurfaceTexture mSurfaceTexture;
    private String mRecordingPath = null;
    private boolean mRecordingFlag = false;
    private MediaRecorder mRecorder = null;

    public interface OnJavaCamViewListener {

        void onPictureTaken(String file, String time, String err);

        void onRecordingStop(String file);

        void onRecordingError();
    }

    public static class JavaCameraSizeAccessor implements ListItemAccessor {

        @Override
        public int getWidth(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.width;
        }

        @Override
        public int getHeight(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.height;
        }
    }

    public JavaCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public JavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected boolean initializeCamera(int width, int height) {
        Log.d(TAG, "Initialize java camera");
        boolean result = true;
        synchronized (this) {
            mCamera = null;

            if (mCameraIndex == CAMERA_ID_ANY) {
                Log.d(TAG, "Trying to open camera with old open()");
                try {
                    mCamera = Camera.open();
                } catch (Exception e) {
                    Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
                }

                if (mCamera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    boolean connected = false;
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                        try {
                            mCamera = Camera.open(camIdx);
                            connected = true;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                        }
                        if (connected) break;
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    int localCameraIndex = mCameraIndex;
                    if (mCameraIndex == CAMERA_ID_BACK) {
                        Log.i(TAG, "Trying to open back camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo(camIdx, cameraInfo);
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    } else if (mCameraIndex == CAMERA_ID_FRONT) {
                        Log.i(TAG, "Trying to open front camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo(camIdx, cameraInfo);
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    }
                    if (localCameraIndex == CAMERA_ID_BACK) {
                        Log.e(TAG, "Back camera not found!");
                    } else if (localCameraIndex == CAMERA_ID_FRONT) {
                        Log.e(TAG, "Front camera not found!");
                    } else {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(localCameraIndex) + ")");
                        try {
                            mCamera = Camera.open(localCameraIndex);
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + localCameraIndex + "failed to open: " + e.getLocalizedMessage());
                        }
                    }
                }
            }
            if (mCamera == null)
                return false;
            try {
                Camera.Parameters params = mCamera.getParameters();
                Log.d(TAG, "getSupportedPreviewSizes()");
                List<android.hardware.Camera.Size> sizes = params.getSupportedPreviewSizes();

                if (sizes != null) {
                    /* Select the size that fits surface considering maximum size allowed */
                    Size frameSize = calculateCameraFrameSize(sizes, new JavaCameraSizeAccessor(), width, height);

                    params.setPreviewFormat(ImageFormat.NV21);
                    Log.d(TAG, "Set preview size to " + Integer.valueOf((int) frameSize.width) + "x" + Integer.valueOf((int) frameSize.height));
                    params.setPreviewSize((int) frameSize.width, (int) frameSize.height);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !android.os.Build.MODEL.equals("GT-I9100"))
                        params.setRecordingHint(true);

                    List<String> FocusModes = params.getSupportedFocusModes();
                    if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    }

                    mCamera.setParameters(params);
                    params = mCamera.getParameters();

                    mFrameWidth = params.getPreviewSize().width;
                    mFrameHeight = params.getPreviewSize().height;
                    if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT))
                        mScale = Math.min(((float) height) / mFrameHeight, ((float) width) / mFrameWidth);
                    else
                        mScale = 0;

                    if (mFpsMeter != null) {
                        mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
                    }

                    int size = mFrameWidth * mFrameHeight;
                    size = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                    mBuffer = new byte[size];

                    mCamera.addCallbackBuffer(mBuffer);
                    mCamera.setPreviewCallbackWithBuffer(this);

                    mFrameChain = new Mat[2];
                    mFrameChain[0] = new Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1);
                    mFrameChain[1] = new Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1);

                    AllocateCache();

                    mCameraFrame = new JavaCameraFrame[2];
                    mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], mFrameWidth, mFrameHeight);
                    mCameraFrame[1] = new JavaCameraFrame(mFrameChain[1], mFrameWidth, mFrameHeight);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                        mCamera.setPreviewTexture(mSurfaceTexture);
                    } else
                        mCamera.setPreviewDisplay(null);

                    /* Finally we are ready to start the preview */
                    mCamera.startPreview();
                } else
                    result = false;
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }
        }

        return result;
    }

    public synchronized void releaseCamera() {
        Log.d(TAG,"releaseCamera call");

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        if (mFrameChain != null) {
            mFrameChain[0].release();
            mFrameChain[1].release();
        }
        if (mCameraFrame != null) {
            mCameraFrame[0].release();
            mCameraFrame[1].release();
        }

    }

    private boolean mCameraFrameReady = false;
    private int mWidth = 0;
    private int mHeight = 0;

    @Override
    protected boolean connectCamera(int width, int height) {
        Log.d(TAG, "Connecting to camera = " + width + " , " + height);
        mWidth = width;
        mHeight = height;
        if (!initializeCamera(width, height)) {
            return false;
        }
        mCameraFrameReady = false;

        Log.d(TAG, "Starting processing thread");
        mStopThread = false;
        mThread = new Thread(new CameraWorker());
        mThread.start();

        return true;
    }

    @Override
    protected void disconnectCamera() {
        Log.d(TAG, "Disconnecting from camera");
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Wating for thread");
            if (mThread != null)
                mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread = null;
        }

        /* Now release camera */
        releaseCamera();

        mCameraFrameReady = false;
    }

    public synchronized void reconnectCamera() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.setPreviewCallback(null);
                    mCamera.release();

                    Log.d(TAG, "RE Connecting to camera");
//                    try {
//                        Thread.sleep(1500);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                    if (!initializeCamera(mWidth, mHeight)) {
                        Log.d(TAG, "initializeCamera fail");
                        return;
                    }
                }
                if (mCamera != null) {
                    mCamera.lock();/* very important */
                }
            }
        }).start();
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        synchronized (this) {
            mFrameChain[mChainIdx].put(0, 0, frame);
            mCameraFrameReady = true;
            this.notify();
        }
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }

    private class JavaCameraFrame implements CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            return mRgba;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        public void release() {
            mRgba.release();
        }

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
    }

    ;

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            do {
                boolean hasFrame = false;
                synchronized (JavaCameraView.this) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            JavaCameraView.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady) {
                        mChainIdx = 1 - mChainIdx;
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!mStopThread && hasFrame) {
                    if (!mFrameChain[1 - mChainIdx].empty())
                        deliverAndDrawFrame(mCameraFrame[1 - mChainIdx]);
                }
            } while (!mStopThread);
        }
    }
    public void beginRecording
            (Context con, String output_file, int recording_time /*sec*/) {
        mRecordingPath = output_file;
        String path = mRecordingPath;
        File outFile = new File(path);
        if (outFile.exists()) {
            outFile.delete();
        }

        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
        }
        CamcorderProfile profile = CamcorderProfile.get(0, CamcorderProfile.QUALITY_480P);
        mCamera.unlock();
//        mCamera.stopPreview(); unlock 이후에 하면 퍽소리가 안나는듯
        mRecorder.setCamera(mCamera);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

//        mRecorder.setOutputFormat(profile.fileFormat);
//        mRecorder.setVideoEncoder(profile.videoCodec);
//        mRecorder.setVideoEncodingBitRate(profile.videoBitRate);
//        mRecorder.setVideoFrameRate(profile.videoFrameRate);
//        mRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
//        mRecorder.setCaptureRate(15);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setProfile(profile);
        mRecorder.setMaxDuration(recording_time * 1000);
        mRecorder.setOutputFile(path);
        mRecorder.setOnErrorListener(this);
        mRecorder.setOnInfoListener(this);

        try {
            mRecorder.prepare();
            mRecorder.start();
            mRecordingFlag = true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            if (outFile.exists()) {
                outFile.delete();
            }
            stopRecording();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            if (outFile.exists()) {
                outFile.delete();
            }
            stopRecording();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            if (outFile.exists()) {
                outFile.delete();
            }
            stopRecording();
            return;
        }
    }

    public synchronized void stopRecording() {

        if (mRecorder != null && mRecordingFlag) {
            mRecordingFlag = false;
            try {
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                System.gc();

            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        if (mCameraViewListener != null) {
            mCameraViewListener.onRecordingStop(mRecordingPath);
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            stopRecording();
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        stopRecording();
        if (mCameraViewListener != null) {
            mCameraViewListener.onRecordingError();
        }
    }

    public void pictureTaken(String file_path, String log, String time) {
        mStillcutFilePath = file_path;
        mStillcutTime = time;
        if (mCamera != null) {
            mCamera.takePicture(null, null, myPictureCallback);
        }
    }


    Camera.PictureCallback myPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            try {
                FileOutputStream fos = new FileOutputStream(mStillcutFilePath);
                fos.write(arg0);
                fos.flush();
                fos.close();

            } catch (Exception e) {
                e.printStackTrace();
                if (mCameraViewListener != null) {
                    mCameraViewListener.onPictureTaken(mStillcutFilePath, mStillcutTime, "sdcard_error");
                    return;
                }
            }
            try {
                Thread.sleep(300);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
            if (mCameraViewListener != null) {
                mCameraViewListener.onPictureTaken(mStillcutFilePath, mStillcutTime, "taken_success");
            }
        }
    };

    public void setJavaCamViewListener(OnJavaCamViewListener evt_listener) {
        mCameraViewListener = evt_listener;
    }
}