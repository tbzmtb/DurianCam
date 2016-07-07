package kr.durian.duriancam.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import kr.durian.duriancam.R;
import kr.durian.duriancam.util.DataPreference;

/**
 * Created by tbzm on 16. 5. 4.
 */
public class IntroActivity extends Activity {

    private Handler mHandler;
    private int mDelay = 2 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        DataPreference.PREF = PreferenceManager.getDefaultSharedPreferences(this);
        mHandler = new Handler();
        mHandler.postDelayed(mRun, mDelay);
    }

    final Runnable mRun = new Runnable() {
        @Override
        public void run() {
            Intent i = new Intent(IntroActivity.this, MainActivity.class);
            startActivity(i);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mHandler.removeCallbacks(mRun);
    }
}
