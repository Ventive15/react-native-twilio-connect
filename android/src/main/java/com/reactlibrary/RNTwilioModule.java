
package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.support.annotation.Nullable;
import android.support.annotation.NonNull;
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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;
import com.twilio.voice.RegistrationException;
import com.twilio.voice.RegistrationListener;
import com.twilio.voice.Voice;
import java.lang.annotation.Annotation;
import java.util.HashMap;

import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

public class RNTwilioModule extends ReactContextBaseJavaModule{

  private final ReactApplicationContext reactContext;
  private String eventName;
  private WritableMap params;


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

  private static final String TAG = "TwilioModule";

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
  public void call(String accessToken, ReadableMap twiMLParams){
    activeCall = Voice.call(getReactApplicationContext(), accessToken, convertToNativeMap(twiMLParams), callListener);
  }

  @ReactMethod
  public void disconnectCall(){
    disconnect();
  }

  @ReactMethod
  public void makeStatusOfMicrophoneTo(Boolean isTomute, Callback callback){
    AudioManager audioManager;
    Context context = reactContext.getBaseContext();
    audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    audioManager.setMode(AudioManager.MODE_IN_CALL);
    audioManager.setMicrophoneMute(isTomute);
    callback.invoke(null,true);
  }

  @ReactMethod
  public void isMicrophoneMute(Callback callback){
    Context context = reactContext.getBaseContext();
    audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    audioManager.setMode(AudioManager.MODE_IN_CALL);
    callback.invoke(null,audioManager.isMicrophoneMute());
  }

  @ReactMethod
  public void makeSpeakerStatusTo(Boolean isON, Callback callback){
    AudioManager audioManager;
    Context context = reactContext.getBaseContext();
    audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    audioManager.setMode(AudioManager.MODE_IN_CALL);
    audioManager.setSpeakerphoneOn(isON);
    callback.invoke(null,true);
  }

  @ReactMethod
  public void isSpeakerOn(Callback callback){
    AudioManager audioManager;
    Context context = reactContext.getBaseContext();
    audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    callback.invoke(null,audioManager.isSpeakerphoneOn());
  }

  //Method for Android function of twilio

  private Map<String, String> convertToNativeMap(ReadableMap readableMap) {
    Map<String, String> hashMap = new HashMap<String, String>();
    ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
    while (iterator.hasNextKey()) {
      String key = iterator.nextKey();
      ReadableType readableType = readableMap.getType(key);
      switch (readableType) {
        case String:
          hashMap.put(key, readableMap.getString(key));
          break;
        default:
          throw new IllegalArgumentException("Could not convert object with key: " + key + ".");
      }
    }
    return hashMap;
  }


  private void sendEvent(String eventName, @Nullable WritableMap params) {
    Log.d(TAG, ">>> sendEvent: start sending event (" + eventName + ")");
    if (params != null) {
      Log.d(TAG, ">>> sendEvent: params: " + params.toString() + ")");
    }
    DeviceEventManagerModule.RCTDeviceEventEmitter deviceEE = getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    Log.d(TAG, ">>> sendEvent: after creating device event emitter (" + eventName + ")");
    deviceEE.emit(eventName, params);
    Log.d(TAG, ">>> sendEvent: after emitting event (" + eventName + ")");
  }

  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "onReceive");
//    Device device = intent.getParcelableExtra(Device.EXTRA_DEVICE);
//    Connection incomingConnection = intent.getParcelableExtra(Device.EXTRA_CONNECTION);
//
//    if (incomingConnection == null && device == null) {
//      return;
//    }
//    intent.removeExtra(Device.EXTRA_DEVICE);
//    intent.removeExtra(Device.EXTRA_CONNECTION);
//
//    _pendingConnection = incomingConnection;
//
//    Map<String, String> connParams = _pendingConnection.getParameters();
//    WritableMap params = Arguments.createMap();
//    if (connParams != null) {
//      for (Map.Entry<String, String> entry : connParams.entrySet()) {
//        params.putString(entry.getKey(), entry.getValue());
//      }
//    }
//    sendEvent("deviceDidReceiveIncoming", params);
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

      }
    };
  }


  protected void onResume() {
    registerReceiver();
  }

  protected void onPause() {
    unregisterReceiver();
  }

//  public void onDestroy() {
//    soundPoolManager.release();
//  }

  private void handleIncomingCallIntent(Intent intent) {
    if (intent != null && intent.getAction() != null) {
      if (intent.getAction().equals(ACTION_INCOMING_CALL)) {
        activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
        if (activeCallInvite != null && (activeCallInvite.getState() == CallInvite.State.PENDING)) {
          soundPoolManager.playRinging();
          activeCallNotificationId = intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, 0);
        } else {
          if (alertDialog != null && alertDialog.isShowing()) {
            soundPoolManager.stopRinging();
            alertDialog.cancel();
          }
        }
      } else if (intent.getAction().equals(ACTION_FCM_TOKEN)) {
        //retrieveAccessToken();
      }
    }
  }



  private void registerReceiver() {
    if (!isReceiverRegistered) {
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(ACTION_INCOMING_CALL);
      intentFilter.addAction(ACTION_FCM_TOKEN);
      LocalBroadcastManager.getInstance(getReactApplicationContext()).registerReceiver(
              voiceBroadcastReceiver, intentFilter);
      isReceiverRegistered = true;
    }
  }

  private void unregisterReceiver() {
    if (isReceiverRegistered) {
      LocalBroadcastManager.getInstance(getReactApplicationContext()).unregisterReceiver(voiceBroadcastReceiver);
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
      Voice.register(getReactApplicationContext(), accessToken, Voice.RegistrationChannel.FCM, fcmToken, registrationListener);
    }
  }

  private View.OnClickListener callActionFabClickListener() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
//        alertDialog = createCallDialog(callClickListener(), cancelCallClickListener(), VoiceActivity.this);
//        alertDialog.show();
      }
    };
  }

  private View.OnClickListener hangupActionFabClickListener() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        soundPoolManager.playDisconnect();
//        resetUI();
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
    activeCallInvite.accept(getReactApplicationContext(), callListener);
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
        //Notify Mute
      } else {
        //Notify Unmute
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
    int resultMic = ContextCompat.checkSelfPermission(getReactApplicationContext(), Manifest.permission.RECORD_AUDIO);
    return resultMic == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermissionForMicrophone() {
//    if (ActivityCompat.shouldShowRequestPermissionRationale(getReactApplicationContext(), Manifest.permission.RECORD_AUDIO)) {
//      Snackbar.make(coordinatorLayout,
//              "Microphone permissions needed. Please allow in your application settings.",
//              SNACKBAR_DURATION).show();
//    } else {
//      ActivityCompat.requestPermissions(
//              getReactApplicationActivity(),
//              new String[]{Manifest.permission.RECORD_AUDIO},
//              MIC_PERMISSION_REQUEST_CODE);
//    }
  }

  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    /*
     * Check if microphone permissions is granted
     */
    if (requestCode == MIC_PERMISSION_REQUEST_CODE && permissions.length > 0) {
      if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        //Permission denied //TODO
      } else {
        // retrieveAccessToken();
      }
    }
  }








}