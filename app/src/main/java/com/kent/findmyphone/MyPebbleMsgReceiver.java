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

                Intent actionIntent = new Intent();
                actionIntent.setClassName("com.kent.findmyphone", "com.kent.findmyphone.MainActivity");
                //actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                if (data.getInteger(MainActivity.KEY_BUTTON_UP) != null) {
                    // Vibrate!!
                    if(DEBUG) Log.v(TAG, "KEY_BUTTON_UP. Vibrate!");
                    actionIntent.putExtra("mode", MainActivity.STRING_MODE_VIBRATE);

                } else if (data.getInteger(MainActivity.KEY_BUTTON_DOWN) != null) {
                    // Ring!!
                    if(DEBUG) Log.v(TAG, "KEY_BUTTON_DOWN. Ring!");
                    actionIntent.putExtra("mode", MainActivity.STRING_MODE_RINGING);

                } else if (data.getInteger(MainActivity.KEY_BUTTON_SELECT) != null) {
                    if(DEBUG) Log.v(TAG, "KEY_BUTTON_SELECT. Stop!");
                    actionIntent.putExtra("mode", MainActivity.STRING_MODE_STOP);
                } else if (data.getInteger(MainActivity.KEY_QUERY_MODE) != null) {
                    if(DEBUG) Log.v(TAG, "KEY_QUERY_MODE.");
                    actionIntent.putExtra("mode", MainActivity.STRING_QUERY_MODE);
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
