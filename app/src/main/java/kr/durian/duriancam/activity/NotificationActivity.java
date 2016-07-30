package kr.durian.duriancam.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.Logger;

/**
 * Created by sunyungkim on 16. 7. 29..
 */
public class NotificationActivity extends AppCompatActivity {

    public static final String TAG = "NotificationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String imageTime = getIntent().getStringExtra(Config.PUSH_IMAGE_TIME_INTENT_KEY);
        Logger.d(TAG, "imageTime = " + imageTime);
        if (isTaskRoot()) {
            Logger.d(TAG, "isTaskRoot call ");
            Intent startIntent = new Intent(this, ViewerModeSelectActivity.class);
            startIntent.putExtra(Config.PUSH_IMAGE_TIME_INTENT_KEY, imageTime);
            startActivity(startIntent);
        }
        else {
            Logger.d(TAG, "already start app send broadcast");
        }
        finish();
    }
}

