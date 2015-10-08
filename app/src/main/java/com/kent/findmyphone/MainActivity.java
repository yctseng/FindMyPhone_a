package com.kent.findmyphone;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

public class MainActivity extends Activity {
    private static final String TAG = "PebbleFindMyPhone";
    protected final boolean DEBUG = false;

    // Must sync with the definitions on pebble app
    public static final int KEY_BUTTON_UP = 0;
    public static final int KEY_BUTTON_DOWN = 1;
    public static final int KEY_BUTTON_SELECT = 2;
    public static final int KEY_QUERY_MODE = 3;


    private static final int  OUTGOING_KEY_MODE = 1;

    public static final String STRING_MODE_INIT = "MODE_INIT";
    public static final String STRING_MODE_STOP = "MODE_STOP";
    public static final String STRING_MODE_VIBRATE = "MODE_VIBRATE";
    public static final String STRING_MODE_RINGING = "MODE_RINGING";

    // Must the same as the UUID of the pebble app. Check the UUID in the settings of the project.
    public static final UUID APP_UUID = UUID.fromString("3783cff2-5a14-477d-baee-b77bd423d079");
    private PebbleKit.PebbleDataReceiver mDataReceiver = null;
    private String mStartMode;
    // id of the notification
    private Button mBtnStop;
    public static int notifyID = 334455;

    private int mOrigVolume = -1;
    private boolean mShowVolumeUI = false;
    private int mVolumeFlags = mShowVolumeUI ? AudioManager.FLAG_SHOW_UI : 0;
    private boolean mStoped = false;

    private void restoreVolume() {
        if (DEBUG) Log.v(TAG,"restoreVolume");
        if(mOrigVolume > -1) {
            AudioManager am=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
            if( am != null) {
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mOrigVolume, mVolumeFlags);
            }
            mOrigVolume = -1;
            if (DEBUG)
                Log.d(TAG, "Volume after restoreVolume:" + am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if (DEBUG) Log.v(TAG, "savedInstanceState:"+savedInstanceState);
        if(savedInstanceState != null) {
            mOrigVolume = savedInstanceState.getInt("origVolume");
            if (DEBUG) Log.d(TAG, "onCreate getInt from savedInstanceState origVolume:" + mOrigVolume);
       }

        Intent initIntent = this.getIntent();
        mStartMode = initIntent.getStringExtra("mode");
        if(mStartMode==null)
            mStartMode = STRING_MODE_INIT;
        if(DEBUG) Log.d(TAG,"mode:"+mStartMode+" vs "+STRING_MODE_STOP);

        if(STRING_MODE_STOP.equals(mStartMode)) {
            if(DEBUG) Log.d(TAG, "on create should finish!!");
            mStoped = true;
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        mBtnStop = (Button) findViewById(R.id.button_stop);
        mBtnStop.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(MainActivity.notifyID);
                } else {
                    Log.e(TAG, getResources().getString(R.string.err_notification_mgr_null));
                }
                Toast.makeText(getApplicationContext(), R.string.action_app_terminate, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

            if (mOrigVolume == -1 && mStartMode !=null) {
                mOrigVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                if(DEBUG) Log.d(TAG, "GET mOrigVolume:" + mOrigVolume);

                if(DEBUG) Log.d(TAG, "MAX:" + am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION));

                int v = DEBUG ? 4 :  am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, v, mVolumeFlags);
                if(DEBUG) Log.d(TAG, "Volume after onCreate:" + am.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
            }
        }
        catch(Exception e) {
            Log.e(TAG, "onCreate exception:"+e);
            finish();
        }
    }

    @Override
    public void onPause() {
        if(DEBUG) Log.v(TAG, "onPause");
        super.onPause();
        if(mDataReceiver != null) {
            unregisterReceiver(mDataReceiver);
            mDataReceiver = null;
        }
    }

    @Override
    protected void onResume() {
        if(DEBUG) Log.v(TAG, "onResume");
        super.onResume();

        if(mStoped) {
            if(DEBUG) Log.w(TAG, "mStoped:"+mStoped+" onResume STOP!!!");
            finish();
            return;
        }
        if(mStartMode != null)
            notifyPebbleState(mStartMode);

        Context context = getApplicationContext();
        boolean isConnected = PebbleKit.isWatchConnected(context);
        if(isConnected) {
            // We don't need to launch the app on pebble yet in current design.
            //PebbleKit.startAppOnPebble(context, APP_UUID);
            if (mDataReceiver == null) {
                // For now, we handled the keyevents in MyPebbleMsgReceiver.java. This receiver is left for future design.
                mDataReceiver = new PebbleKit.PebbleDataReceiver(APP_UUID) {
                    @Override
                    public void receiveData(Context context, int transactionId, PebbleDictionary dict) {
                        // Message received, over!
                        PebbleKit.sendAckToPebble(context, transactionId);
                        if(DEBUG) Log.i(TAG, "Got message from Pebble!");
                        if (dict.getInteger(KEY_BUTTON_UP) != null) {
                            if(DEBUG) Log.i(TAG, "KEY_BUTTON_UP");
                        }

                        if (dict.getInteger(KEY_BUTTON_DOWN) != null) {
                            if(DEBUG) Log.i(TAG, "KEY_BUTTON_DOWN");
                        }

                        if (dict.getInteger(KEY_BUTTON_SELECT) != null) {
                            if(DEBUG) Log.i(TAG, "KEY_BUTTON_SELECT");
                        }
                        if (dict.getInteger(KEY_QUERY_MODE) != null) {
                            if(DEBUG) Log.i(TAG, "KEY_QUERY_MODE");
                            if(mStartMode != null)
                                notifyPebbleState(mStartMode);
                        }
                    }
                };
            }
            PebbleKit.registerReceivedDataHandler(this, mDataReceiver);
        }
        else {
            Toast.makeText(getApplicationContext(), "Pebble is not connected!!!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        if(DEBUG) Log.d(TAG, "onRestoreInstanceState:"+savedInstanceState);

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if(DEBUG) Log.v(TAG, "onSaveInstanceState");
        savedInstanceState.putInt("origVolume", mOrigVolume);
        if(DEBUG) Log.d(TAG, "savedInstanceState:"+savedInstanceState);
    }
    @Override
    public void finish() {
        super.finish();
        restoreVolume();
        notifyPebbleState(STRING_MODE_STOP);
    }

    protected void onNewIntent(Intent intent) {
        if(DEBUG) Log.v(TAG, "onNewIntent:"+intent);
        super.onNewIntent(intent);
        setIntent(intent);//must store the new intent unless getIntent() will return the old one

        mStartMode = intent.getStringExtra("mode");
        if(mStartMode==null)
            mStartMode = STRING_MODE_INIT;
        if(DEBUG) Log.d(TAG,"initIntent:"+intent+" mode:"+mStartMode);
        if(STRING_MODE_STOP.equals(mStartMode)) {
            if(DEBUG) Log.d(TAG, "onNewIntent should finish!!");
            mStoped = true;
        }
    }

    private void notifyPebbleState(String mode) {
        if(DEBUG) Log.d(TAG, "notifyPebbleState mode:"+mode);
        PebbleDictionary outgoing = new PebbleDictionary();
        outgoing.addString(OUTGOING_KEY_MODE, mode);
        PebbleKit.sendDataToPebble(getApplicationContext(), APP_UUID, outgoing);
    }
}
