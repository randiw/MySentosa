package com.mysentosa.android.sg.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mysentosa.android.sg.R;
import com.mysentosa.android.sg.SplashScreenActivity;
import com.mysentosa.android.sg.receiver.GcmBroadcastReceiver;

import sg.edu.smu.livelabs.integration.LiveLabsApi;

/**
 * Created by randiwaranugraha on 6/26/15.
 */
public class GcmIntentService extends IntentService {

    public static final String TAG = GcmIntentService.class.getSimpleName();
    public static final int NOTIFICATION_ID = 1;

    public GcmIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        // The getMessageType() intent parameter must be the intent you received in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                String message = LiveLabsApi.getInstance().processNotification(extras);
                if (message != null) {
                    Intent notifyIntent = new Intent(this, SplashScreenActivity.class);
                    notifyIntent.putExtra("NOTI_TYPE", "NewLiveLabsPromotion");
                    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    PendingIntent contentIntent = PendingIntent.getActivity(this, 0,notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.mipmap.ic_launcher)
                                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                                    .setContentTitle("Sentosa")
                                    .setSound(alarmSound)
                                    .setContentText(message);

                    mBuilder.setContentIntent(contentIntent);

                    NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                }
            }
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
}