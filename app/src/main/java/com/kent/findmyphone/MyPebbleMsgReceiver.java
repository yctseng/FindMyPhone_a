package com.kent.findmyphone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.json.JSONException;

import java.util.UUID;

/**
 * Created by kent on 2015/10/5.
 */
public class MyPebbleMsgReceiver extends BroadcastReceiver {
    private static final String TAG = "MyPebbleMsgReceiver";
    private final boolean DEBUG = false;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.INTENT_APP_RECEIVE)) {
            final UUID receivedUuid = (UUID) intent.getSerializableExtra(Constants.APP_UUID);

            if(DEBUG) Log.d(TAG, "onReceive UUID:["+receivedUuid+"]");
            // Pebble-enabled apps are expected to be good citizens and only inspect broadcasts containing their UUID
            if (!MainActivity.APP_UUID.equals(receivedUuid)) {
                if(DEBUG) Log.w(TAG, "not my UUID");
                return;
            }

            final int transactionId = intent.getIntExtra(Constants.TRANSACTION_ID, -1);
            final String jsonData = intent.getStringExtra(Constants.MSG_DATA);
            if (jsonData == null || jsonData.isEmpty()) {
                if(DEBUG) Log.w(TAG, "jsonData null");
                return;
            }

            try {
                final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
                // do what you need with the data
                Log.d(TAG, "data:"+data);
                PebbleKit.sendAckToPebble(context, transactionId);

                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                final long[] vibrate_pattern = {200, 1500, 200, 1500, 200, 1500, 200, 1500, 200, 1500};

                Notification.Builder myNotiBuilder = new Notification.Builder(context);
                myNotiBuilder.setSmallIcon(R.mipmap.ic_launcher);
                myNotiBuilder.setContentTitle(context.getResources().getString(R.string.notification_title));
                myNotiBuilder.setContentText(context.getResources().getString(R.string.notification_body));
                myNotiBuilder.setAutoCancel(true);

                if(true) {
                    // The notiIntent would be sent to launch app when clear the notification on status bar. Actually, we don't need it for simpler user experience.
                    Intent notiIntent = new Intent();
                    notiIntent.setClassName("com.kent.findmyphone", "com.kent.findmyphone.MainActivity");
                    notiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    notiIntent.putExtra("mode", MainActivity.STRING_MODE_STOP);
                    PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notiIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    myNotiBuilder.setContentIntent(contentIntent);
                }
                else {
                    // Touch the notification to clear
                    myNotiBuilder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0));
                }

                // No matter what key event, cancel the notification first and maybe launch again to avoid some issues.
                notificationManager.cancel(MainActivity.notifyID);

                // Used to launch app to setVolume to max level.
                Intent actionIntent = new Intent();
                actionIntent.setClassName("com.kent.findmyphone", "com.kent.findmyphone.MainActivity");
                //actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                if (data.getInteger(MainActivity.KEY_BUTTON_UP) != null) {
                    // Vibrate!!
                    if(DEBUG) Log.v(TAG, "KEY_BUTTON_UP. Vibrate!");

                    AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                    // Disable priority mode
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    // Set vibrate pattern
                    myNotiBuilder.setVibrate(vibrate_pattern);
                    // build notification and send
                    Notification notification = myNotiBuilder.build();
                    notification.flags |= Notification.FLAG_INSISTENT;
                    notification.flags |= Notification.FLAG_NO_CLEAR;

                    notificationManager.notify(MainActivity.notifyID, notification);

                    actionIntent.putExtra("mode", MainActivity.STRING_MODE_VIBRATE);

                } else if (data.getInteger(MainActivity.KEY_BUTTON_DOWN) != null) {
                    // Ring!!
                    if(DEBUG) Log.v(TAG, "KEY_BUTTON_DOWN. Ring!");

                    AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                    // Disable priority mode
                    am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    // get ringtone and set into NotificationBuilder
                    Uri uri= RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
                    myNotiBuilder.setSound(uri, AudioAttributes.USAGE_NOTIFICATION);
                    // build notification and send
                    Notification notification = myNotiBuilder.build();
                    notification.flags |= Notification.FLAG_INSISTENT;
                    notification.flags |= Notification.FLAG_NO_CLEAR;
                    notificationManager.notify(MainActivity.notifyID, notification);

                    actionIntent.putExtra("mode", MainActivity.STRING_MODE_RINGING);

                } else if (data.getInteger(MainActivity.KEY_BUTTON_SELECT) != null) {
                    if(DEBUG) Log.v(TAG, "KEY_BUTTON_SELECT. Stop!");

                    actionIntent.putExtra("mode", MainActivity.STRING_MODE_STOP);
                } else {
                    if(DEBUG) Log.v(TAG, "Unknown key received. data:"+ data);
                    return;
                }
                context.startActivity(actionIntent);
            } catch (JSONException e) {
                Log.w(TAG, "failed reived -> dict" + e);
                return;
            }
        }
    }
}
