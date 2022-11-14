package com.example.apicalltest;

import static com.example.apicalltest.ActionGestures.retrieveAction;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.QRCodeDetector;

import android.animation.AnimatorSet;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.apicalltest.APIStructures.Message;
import com.example.apicalltest.APIStructures.MessageOut;
import com.example.apicalltest.ActionGestures.*;

public class QRScanActivity extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;

    QRCodeDetector qrCodeDetector;

    String username = "DEFAULT";

    HashMap<String, Integer> cooldownMap;
    Timer cooldownTimer;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public QRScanActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_qrscan);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(ARApplication.CAMERA);
        mOpenCvCameraView.setAlpha(0);

        mOpenCvCameraView.setCvCameraViewListener(this);
        qrCodeDetector = new QRCodeDetector();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        username = preferences.getString("username", "DEFAULT");
        cooldownTimer = new Timer();
        cooldownTimer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                if (!cooldownMap.isEmpty()){
                    for (String s: cooldownMap.keySet()){
                        Integer i = cooldownMap.get(s);
                        if (i == 0){
                            cooldownMap.remove(s);
                        }else{
                            cooldownMap.put(s, i-1);
                        }
                    }
                }
            }
        }, 1000, 1000);
        cooldownMap = new HashMap<String, Integer>();
        SwitchCompat mySwitch = (SwitchCompat) findViewById(R.id.switch1);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cooldownMap.clear();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = preferences.edit();
                if (isChecked){
                    mySwitch.setText("blue");
                    editor.putString("username", "blue");
                    username = "blue";
                }else{
                    mySwitch.setText("red");
                    editor.putString("username", "red");
                    username = "red";
                }
                editor.apply();
            }
        });
        SwitchCompat mySwitch2 = (SwitchCompat) findViewById(R.id.switch2);
        mySwitch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    findViewById(R.id.tutorial1_activity_java_surface_view).setAlpha(1);
                }else{
                    findViewById(R.id.tutorial1_activity_java_surface_view).setAlpha(0);
                }
            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        String out = qrCodeDetector.detectAndDecode(inputFrame.rgba());
        if (!Objects.equals(out, "")){
            Log.d("qrFound", out);
            Log.d("qrFound", username);
            MyAction action = retrieveAction(out);
            if (action != null){
                String hash = action.getUsername() + action.getGesture().toString();
                if (!cooldownMap.containsKey(hash)){
                    cooldownMap.put(hash, 5);
                    processAction(username, action);
                }
            }
        }
        return inputFrame.rgba();
    }


    boolean allowForeignControl = true; // Allows other users to take files from your phone

    public void processAction(String username, MyAction action){
        if (action.getGesture().equals(MyGesture.DROP)){
            //TODO Retrieve file from api linked with action username
            retrieveMessage(username, action.getUsername());
            animateColor("#00FF00");
        }else if (action.getGesture().equals(MyGesture.PICK)){
            animateColor("#FF0000");
            if (username.equals(action.getUsername())) {
                // My hand is on my phone
                //Send file to api EVERYONE, openable by me
                postMessageToEveryone(username, "sample message", action.getUsername());
            }else{
                if (!allowForeignControl){
                    return;
                }
                // someone else's hand is on my phone
                // Send file to api on action.username folder, he can see it
                postMessageFromTo(username, action.getUsername(), "sample message", action.getUsername());

            }
        }
    }

    public void postMessageToEveryone(String myUsername, String message, String openableBy){
        Call<Message> call = RetrofitClient.getInstance().getMyApi().sendMessageToEveryone(myUsername, message, openableBy);
        Log.d("d", call.request().toString());
        call.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                Message result = response.body();
                Log.d("d", response.toString());
                try {
                    myViewer("Message uploaded.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                myViewer("ERROR on upload.");
            }
        });
    }

    public void postMessageFromTo(String myUsername, String destUsername, String message, String openableBy){
        Call<Message> call = RetrofitClient.getInstance().getMyApi().sendMessage(destUsername, myUsername, message, openableBy);
        Log.d("d", call.request().toString());
        call.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                Message result = response.body();
                Log.d("d", response.toString());
                try {
                    myViewer("Message uploaded.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                myViewer("ERROR on upload.");
            }
        });
    }
    public void retrieveMessage(String username, String requester){
        Call<MessageOut> call = RetrofitClient.getInstance().getMyApi().getMessages(username, requester);
        call.enqueue(new Callback<MessageOut>() {
            @Override
            public void onResponse(Call<MessageOut> call, Response<MessageOut> response) {
                MessageOut result = response.body();
                try {
                    Message msg = result.get();
                    String msgString = msg.get();
                    if (msgString == "No message found") {
                        //TODO add a sucess int in the response to check instead of the message
                    }else {
                        myViewer(msgString);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<MessageOut> call, Throwable t) {
                myViewer("ERROR");
            }
        });
    }

    public void myViewer(String str){
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }

    public void animateColor(String color){
        FrameLayout frameLayout = findViewById(R.id.michel);
        Animation animation1 = new AlphaAnimation(0, 1); // Change alpha
        animation1.setDuration(100); // duration - half a second
        animation1.setInterpolator(new LinearInterpolator());
        animation1.setRepeatMode(Animation.REVERSE);
        animation1.setRepeatCount(1);
        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                frameLayout.setAlpha(0);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        //Animation animation2 = new AlphaAnimation(1, 0); // Change alpha
        //animation2.setDuration(200); // duration - half a second
        //animation2.setInterpolator(new LinearInterpolator());
        /*AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(animation1);
        animationSet.addAnimation(animation2);*/
        frameLayout.setAlpha(1);
        frameLayout.setBackgroundColor(Color.parseColor(color));
        frameLayout.startAnimation(animation1);
    }

}
