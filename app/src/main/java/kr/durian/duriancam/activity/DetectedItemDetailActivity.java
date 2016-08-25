package kr.durian.duriancam.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;

import kr.durian.duriancam.R;
import kr.durian.duriancam.data.SecureDetectedData;
import kr.durian.duriancam.util.Logger;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by sunyungkim on 16. 7. 31..
 */
public class DetectedItemDetailActivity extends Activity implements View.OnClickListener {
    private final String TAG = getClass().getName();
    private ImageButton mBtnBack;
    private ImageView mDetailImage;
    private TextView mDateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "ShowDetectedImageListActivity oncreate call");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_detected_item_detail);
        mBtnBack = (ImageButton) findViewById(R.id.btn_back);
        mBtnBack.setOnClickListener(this);
        mDetailImage = (ImageView) findViewById(R.id.detail_image);
        mDateText = (TextView)findViewById(R.id.image_date);
        SecureDetectedData data = getIntent().getParcelableExtra("selected_data");
        Glide.with(this)
                .load(data.getImagePath())
                .into(mDetailImage);
        mDateText.setText(getTimeString(data.getTime()));
        PhotoViewAttacher mAttacher = new PhotoViewAttacher(mDetailImage);
    }

    public String getTimeString(long time) {
        Date now = new Date(time);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(now);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                finish();
                break;


        }
    }
}
