package kr.durian.duriancam.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import kr.durian.duriancam.R;
import kr.durian.duriancam.activity.NotificationActivity;
import kr.durian.duriancam.util.Config;
import kr.durian.duriancam.util.DataPreference;
import kr.durian.duriancam.util.Logger;

/**
 * Created by sjkim on 16. 5. 10.
 */
public class GcmIntentService extends IntentService {
    public static final String TAG = "IntentService";
    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String ENTRY_CAR = "50";
    public static final String EXIT_CAR = "45";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        String title = extras.getString(TITLE);
        String imageTime = extras.getString(MESSAGE);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Logger.d(TAG, "Received: " + extras.toString());

                if (DataPreference.getPushEnable()) {
                    sendNotification(getString(R.string.detect_notice), getString(R.string.detect_detail_notice), imageTime);

                }
                Intent newIntent = new Intent(Config.BROADCAST_SECURE_DETECTED);
                newIntent.putExtra(Config.PUSH_IMAGE_TIME_INTENT_KEY, imageTime);
                sendBroadcast(newIntent);
            }
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String title, String message, String imageTime) {

        Logger.d(TAG, "imageTime = " + imageTime);
        mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.putExtra(Config.PUSH_IMAGE_TIME_INTENT_KEY, imageTime);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setContentTitle(title)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS)
                        .setContentText(message)
                        .setPriority(Notification.PRIORITY_MAX);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
