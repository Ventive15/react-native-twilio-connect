
package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.EditText;

import com.google.firebase.iid.FirebaseInstanceId;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;
import com.twilio.voice.RegistrationException;
import com.twilio.voice.RegistrationListener;
import com.twilio.voice.Voice;

import java.util.HashMap;

public class RNTwilioModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;


  private static final int MIC_PERMISSION_REQUEST_CODE = 1;
  private static final int SNACKBAR_DURATION = 4000;

  private String accessToken;
  private AudioManager audioManager;
  private int savedAudioMode = AudioManager.MODE_INVALID;

  private boolean isReceiverRegistered = false;
  private VoiceBroadcastReceiver voiceBroadcastReceiver;

  // Empty HashMap, never populated for the Quickstart
  HashMap<String, String> twiMLParams = new HashMap<>();

  private CoordinatorLayout coordinatorLayout;
  private FloatingActionButton callActionFab;
  private FloatingActionButton hangupActionFab;
  private FloatingActionButton muteActionFab;
  private Chronometer chronometer;
  private SoundPoolManager soundPoolManager;

  public static final String INCOMING_CALL_INVITE = "INCOMING_CALL_INVITE";
  public static final String INCOMING_CALL_NOTIFICATION_ID = "INCOMING_CALL_NOTIFICATION_ID";
  public static final String ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL";
  public static final String ACTION_FCM_TOKEN = "ACTION_FCM_TOKEN";

  private NotificationManager notificationManager;
  private AlertDialog alertDialog;


  private CallInvite activeCallInvite;
  private Call activeCall;
  private int activeCallNotificationId;

  RegistrationListener registrationListener = registrationListener();
  Call.Listener callListener = callListener();


/*
* TVOCall* _call;
    TVOCallInvite* _callInvite;
    TVOError* _error;
    NSString* _deviceTokenString;
    NSString* _accessToken;
    PKPushRegistry* _voipRegistry;*/


  static String kCallSuccessfullyRegistered = "CallSuccessfullyRegistered";
  static String kCallSuccessfullyUnRegistered = "CallSuccessfullyUnRegistered";
  static String kCallConnected = "CallConnected";
  static String kCallDisconnected = "CallDisconnected";
  static String kCallFailedToConnectOnNetworkError = "CallFailedToConnectOnNetworkError";
  static String kCallFailedOnNetworkError = "CallFailedOnNetworkError";
  static String kCallInviteReceived = "CallInviteReceived";
  static String kCallInviteCancelled = "CallInviteCancelled";
  static String kCallStatePending = "pending";
  static String kCallStateAccepted = "accepted";
  static String kCallStateRejected = "rejected";
  static String kCallStateCancelled = "cancelled";
  static String kRnPushToken = "RnPushToken";

  private static final String TAG = TwilioModule.class.getName();

  public RNTwilioModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }
  @Override
  public String getName() {
    return "RNTwilio";
  }

  //Bridging methods

  @ReactMethod
  public void call(String eventName, ReadableMap twiMLParams){
    activeCall = Voice.call(RNTwilioModule.this, accessToken, twiMLParams, callListener);
  }

  @ReactMethod
  public void disconnectCall(){
    disconnect()
  }

  //Method for Android function of twilio

  private void sendEvent(ReactContext reactContext,
                         String eventName,
                         @Nullable WritableMap params) {
    reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(eventName, params);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handleIncomingCallIntent(intent);
  }

  private RegistrationListener registrationListener() {
    return new RegistrationListener() {
      @Override
      public void onRegistered(String accessToken, String fcmToken) {
        Log.d(TAG, "Successfully registered FCM " + fcmToken);
      }

      @Override
      public void onError(RegistrationException error, String accessToken, String fcmToken) {
        String message = String.format("Registration Error: %d, %s", error.getErrorCode(), error.getMessage());
        Log.e(TAG, message);
        Snackbar.make(coordinatorLayout, message, SNACKBAR_DURATION).show();
      }
    };
  }

  private Call.Listener callListener() {
    return new Call.Listener() {
      @Override
      public void onConnectFailure(Call call, CallException error) {
        setAudioFocus(false);
        Log.d(TAG, "Connect failure");
        String message = String.format("Call Error: %d, %s", error.getErrorCode(), error.getMessage());
        Log.e(TAG, message);
        Snackbar.make(coordinatorLayout, message, SNACKBAR_DURATION).show();
        resetUI();
      }

      @Override
      public void onConnected(Call call) {
        setAudioFocus(true);
        Log.d(TAG, "Connected");
        activeCall = call;
      }

      @Override
      public void onDisconnected(Call call, CallException error) {
        setAudioFocus(false);
        Log.d(TAG, "Disconnected");
        if (error != null) {
          String message = String.format("Call Error: %d, %s", error.getErrorCode(), error.getMessage());
          Log.e(TAG, message);
          Snackbar.make(coordinatorLayout, message, SNACKBAR_DURATION).show();
        }
        resetUI();
      }
    };
  }


  @Override
  protected void onResume() {
    super.onResume();
    registerReceiver();
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver();
  }

  @Override
  public void onDestroy() {
    soundPoolManager.release();
    super.onDestroy();
  }

  private void handleIncomingCallIntent(Intent intent) {
    if (intent != null && intent.getAction() != null) {
      if (intent.getAction().equals(ACTION_INCOMING_CALL)) {
        activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
        if (activeCallInvite != null && (activeCallInvite.getState() == CallInvite.State.PENDING)) {
          soundPoolManager.playRinging();
          alertDialog = createIncomingCallDialog(RNTwilioModule.this,
                  activeCallInvite,
                  answerCallClickListener(),
                  cancelCallClickListener());
          alertDialog.show();
          activeCallNotificationId = intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, 0);
        } else {
          if (alertDialog != null && alertDialog.isShowing()) {
            soundPoolManager.stopRinging();
            alertDialog.cancel();
          }
        }
      } else if (intent.getAction().equals(ACTION_FCM_TOKEN)) {
        retrieveAccessToken();
      }
    }
  }



  private void registerReceiver() {
    if (!isReceiverRegistered) {
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(ACTION_INCOMING_CALL);
      intentFilter.addAction(ACTION_FCM_TOKEN);
      LocalBroadcastManager.getInstance(this).registerReceiver(
              voiceBroadcastReceiver, intentFilter);
      isReceiverRegistered = true;
    }
  }

  private void unregisterReceiver() {
    if (isReceiverRegistered) {
      LocalBroadcastManager.getInstance(this).unregisterReceiver(voiceBroadcastReceiver);
      isReceiverRegistered = false;
    }
  }

  private class VoiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(ACTION_INCOMING_CALL)) {
        /*
         * Handle the incoming call invite
         */
        handleIncomingCallIntent(intent);
      }
    }
  }

  /*
   * Register your FCM token with Twilio to receive incoming call invites
   *
   * If a valid google-services.json has not been provided or the FirebaseInstanceId has not been
   * initialized the fcmToken will be null.
   *
   * In the case where the FirebaseInstanceId has not yet been initialized the
   * VoiceFirebaseInstanceIDService.onTokenRefresh should result in a LocalBroadcast to this
   * activity which will attempt registerForCallInvites again.
   *
   */
  private void registerForCallInvites() {
    final String fcmToken = FirebaseInstanceId.getInstance().getToken();
    if (fcmToken != null) {
      Log.i(TAG, "Registering with FCM");
      Voice.register(this, accessToken, Voice.RegistrationChannel.FCM, fcmToken, registrationListener);
    }
  }

  private View.OnClickListener callActionFabClickListener() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        alertDialog = createCallDialog(callClickListener(), cancelCallClickListener(), VoiceActivity.this);
        alertDialog.show();
      }
    };
  }

  private View.OnClickListener hangupActionFabClickListener() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        soundPoolManager.playDisconnect();
        resetUI();
        disconnect();
      }
    };
  }

  private View.OnClickListener muteActionFabClickListener() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mute();
      }
    };
  }

  /*
   * Accept an incoming Call
   */
  private void answer() {
    activeCallInvite.accept(this, callListener);
    notificationManager.cancel(activeCallNotificationId);
  }

  /*
   * Disconnect from Call
   */
  private void disconnect() {
    if (activeCall != null) {
      activeCall.disconnect();
      activeCall = null;
    }
  }

  private void mute() {
    if (activeCall != null) {
      boolean mute = !activeCall.isMuted();
      activeCall.mute(mute);
      if (mute) {
        muteActionFab.setImageDrawable(ContextCompat.getDrawable(VoiceActivity.this, R.drawable.ic_mic_white_off_24dp));
      } else {
        muteActionFab.setImageDrawable(ContextCompat.getDrawable(VoiceActivity.this, R.drawable.ic_mic_white_24dp));
      }
    }
  }

  private void setAudioFocus(boolean setFocus) {
    if (audioManager != null) {
      if (setFocus) {
        savedAudioMode = audioManager.getMode();
        // Request audio focus before making any device switch.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                  .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                  .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                  .build();
          AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                  .setAudioAttributes(playbackAttributes)
                  .setAcceptsDelayedFocusGain(true)
                  .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                    @Override
                    public void onAudioFocusChange(int i) {
                    }
                  })
                  .build();
          audioManager.requestAudioFocus(focusRequest);
        } else {
          audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                  AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        /*
         * Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
         * required to be in this mode when playout and/or recording starts for
         * best possible VoIP performance. Some devices have difficulties with speaker mode
         * if this is not set.
         */
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
      } else {
        audioManager.setMode(savedAudioMode);
        audioManager.abandonAudioFocus(null);
      }
    }
  }

  private boolean checkPermissionForMicrophone() {
    int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
    return resultMic == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermissionForMicrophone() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
      Snackbar.make(coordinatorLayout,
              "Microphone permissions needed. Please allow in your application settings.",
              SNACKBAR_DURATION).show();
    } else {
      ActivityCompat.requestPermissions(
              this,
              new String[]{Manifest.permission.RECORD_AUDIO},
              MIC_PERMISSION_REQUEST_CODE);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    /*
     * Check if microphone permissions is granted
     */
    if (requestCode == MIC_PERMISSION_REQUEST_CODE && permissions.length > 0) {
      if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        Snackbar.make(coordinatorLayout,
                "Microphone permissions needed. Please allow in your application settings.",
                SNACKBAR_DURATION).show();
      } else {
        retrieveAccessToken();
      }
    }
  }








}