package com.kent.findmyphone;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
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
    public static final String STRING_QUERY_MODE = "QUERY_MODE";

    // Must the same as the UUID of the pebble app. Check the UUID in the settings of the project.
    public static final UUID APP_UUID = UUID.fromString("3783cff2-5a14-477d-baee-b77bd423d079");
    private PebbleKit.PebbleDataReceiver mDataReceiver = null;
    private String mStartMode = STRING_MODE_INIT;
    // id of the notification
    private Button mBtnStop;
    public static int notifyID = 334455;

    private int mFinishTimer = 2000;
    private int mOrigVolume = -1;
    private boolean mShowVolumeUI = false;
    private int mVolumeFlags = mShowVolumeUI ? AudioManager.FLAG_SHOW_UI : 0;
    private boolean mStoped = false;
    private boolean mIsQueryMode = false;

	private Vibrator mVibrator;
	MediaPlayer mMediaPlayer;

	private void startVibrate() {
		Log.d(TAG, "startVibrate");
		long[] pattern = {0, 1000, 500};
		if( mVibrator == null) {
			mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		}
		mVibrator.vibrate(pattern, 0);
	}

	private void stopVibrate() {
		Log.d(TAG, "stopVibrate");
		if( mVibrator == null) {
			mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		}
		mVibrator.cancel();
	}

	private void startRinging() {
		Log.d(TAG, "startRinging");
		Uri uri= RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		if(mMediaPlayer == null) {
			mMediaPlayer = MediaPlayer.create(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
			mMediaPlayer.setLooping(true);
		}
		try {
			mMediaPlayer.setVolume((float)1.0, (float)1.0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		mMediaPlayer.start();

        try {
            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

            if (mOrigVolume == -1) {
                mOrigVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                if(DEBUG) Log.d(TAG, "GET mOrigVolume:" + mOrigVolume);
            }
			int v = DEBUG ? 7:  am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			am.setStreamVolume(AudioManager.STREAM_MUSIC, v, mVolumeFlags);
		}
        catch(Exception e) {
            e.printStackTrace();
        }
		if(DEBUG) Log.d(TAG, "Volume after startRinging:" + am.getStreamVolume(AudioManager.STREAM_MUSIC));
	}

	private void stopRinging() {
		Log.d(TAG, "stopRinging");

		if(mOrigVolume > -1) {
            AudioManager am=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
            if( am != null) {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, mOrigVolume, mVolumeFlags);
            }
            mOrigVolume = -1;
            if (DEBUG)
                Log.d(TAG, "Volume after stopRinging:" + am.getStreamVolume(AudioManager.STREAM_MUSIC));
        }

		if(mMediaPlayer == null) {
			Log.w(TAG, "mMediaPlayer is null when stop Ringing!!");
			return;
		}
		else {
			try {
				mMediaPlayer.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    private void updateState(Intent i) {
        String tmpMode;
        tmpMode = i.getStringExtra("mode");
        Log.d(TAG, "updateState tmpMode:"+tmpMode);
        if (tmpMode == null) {
            mStartMode = STRING_MODE_INIT;
            mIsQueryMode = false;
        }
        else if(tmpMode.equals(STRING_QUERY_MODE)) {
            mIsQueryMode = true;
        }
        else {
            mStartMode = tmpMode;
            mIsQueryMode = false;
        }

		if(STRING_MODE_VIBRATE.equals(mStartMode)) {
			startVibrate();
		}

		if(STRING_MODE_RINGING.equals(mStartMode)) {
			startRinging();
		}

		if(STRING_MODE_STOP.equals(mStartMode)) {
			stopRinging();
			stopVibrate();
		}

        Log.d(TAG, "updateState result mStartMode:" + mStartMode + " mIsQueryMode:" + mIsQueryMode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if (DEBUG) Log.d(TAG, "savedInstanceState:"+savedInstanceState);
        if(savedInstanceState != null) {
            mOrigVolume = savedInstanceState.getInt("origVolume");
            if (DEBUG) Log.d(TAG, "onCreate getInt from savedInstanceState origVolume:" + mOrigVolume);
       }

        Intent initIntent = this.getIntent();
        updateState(initIntent);

        if(DEBUG) Log.d(TAG,"mode:"+mStartMode);

        if(STRING_MODE_STOP.equals(mStartMode) ) {
            if(DEBUG) Log.d(TAG, "on create should finish!!");
            mStoped = true;
            finish();
            notifyPebbleState(STRING_MODE_STOP);
            return;
        }
        if(mIsQueryMode) {
            notifyPebbleState(mStartMode);
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
                notifyPebbleState(STRING_MODE_STOP);
            }
        });

    }

    @Override
    public void onPause() {
        if(DEBUG) Log.d(TAG, "onPause");
        super.onPause();
        if(mDataReceiver != null) {
            unregisterReceiver(mDataReceiver);
            mDataReceiver = null;
        }
    }

    @Override
    protected void onResume() {
        if(DEBUG) Log.d(TAG, "onResume");
        super.onResume();

        if(mStoped) {
            if(DEBUG) Log.w(TAG, "mStoped:"+mStoped+" onResume STOP!!!");
            notifyPebbleState(STRING_MODE_STOP);
            finish();
            return;
        }
        if(mStartMode != null)
            notifyPebbleState(mStartMode);
        else
            Toast.makeText(getApplicationContext(), R.string.warning_not_launch_on_pebble, Toast.LENGTH_SHORT).show();

        Context context = getApplicationContext();
        boolean isConnected = PebbleKit.isWatchConnected(context);

		if(!isConnected) {
			Toast.makeText(getApplicationContext(), "Pebble is not connected!", Toast.LENGTH_SHORT).show();
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
        if(DEBUG) Log.d(TAG, "onSaveInstanceState");
        savedInstanceState.putInt("origVolume", mOrigVolume);
        if(DEBUG) Log.d(TAG, "savedInstanceState:"+savedInstanceState);
    }
    @Override
    public void finish() {
        super.finish();
        stopRinging();
		stopVibrate();
        //notifyPebbleState(STRING_MODE_STOP);
    }

    protected void onNewIntent(Intent intent) {
        if(DEBUG) Log.d(TAG, "onNewIntent:"+intent);
        super.onNewIntent(intent);
        setIntent(intent);//must store the new intent unless getIntent() will return the old one

        updateState(intent);

        if(DEBUG) Log.d(TAG,"initIntent:"+intent+" mode:"+mStartMode);
        if(STRING_MODE_STOP.equals(mStartMode)) {
            if(DEBUG) Log.d(TAG, "onNewIntent should finish!!");
            mStoped = true;
        }

        if(mIsQueryMode) {
            notifyPebbleState(mStartMode);
            if(mStartMode == null) {
                if (DEBUG) Log.d(TAG, "onNewIntent Query mode, finish()!");
                //finish();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Close this activity
                        finish();
                    }
                }, mFinishTimer);
                //return;
            }
        }
    }

    private void notifyPebbleState(String mode) {
        if(DEBUG) Log.d(TAG, "notifyPebbleState mode:"+mode);
        String nowMode;

        if(mode == null) {
            nowMode = STRING_MODE_INIT;
        }
        else {
            nowMode = mode;
        }
        PebbleDictionary outgoing = new PebbleDictionary();
        outgoing.addString(OUTGOING_KEY_MODE, nowMode);
        PebbleKit.sendDataToPebble(getApplicationContext(), APP_UUID, outgoing);
    }
}
