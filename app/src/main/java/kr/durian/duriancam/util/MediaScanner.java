package kr.durian.duriancam.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;

/**
 * Created by tbzm on 15. 10. 15.
 */
public class MediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {
    private MediaScannerConnection mMs;
    private File mFile;

    public MediaScanner(Context context, File f) {
        mFile = f;
        mMs = new MediaScannerConnection(context, this);
        mMs.connect();
    }
    @Override
    public void onMediaScannerConnected() {
        mMs.scanFile(mFile.getPath(), null);
    }
    @Override
    public void onScanCompleted(String path, Uri uri) {
        mMs.disconnect();
    }


}
