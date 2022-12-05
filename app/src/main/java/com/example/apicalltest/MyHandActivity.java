package com.example.apicalltest;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsResult;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyHandActivity extends HandsActivity{
    
    protected static double DISTANCE_THRESHOLD = 0.25;
    protected static int N_SECONDS_COOLDOWN = 5;
    protected static int N_FRAMES_TO_CHANGE = 3;

    private enum GestureTypes{
        NO_GESTURE_INITIALIZED,
        OPEN_FINGERS,
        CLOSED_FINGERS
    };

    Timer cooldownTimer;
    int cooldown = 0;
    GestureTypes gesture = GestureTypes.NO_GESTURE_INITIALIZED;
    int counterFingerOpen = 0;
    int counterFingerTogether = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cooldownTimer = new Timer();
        startTimer();
    }

    @Override
    public void onPause(){
        super.onPause();
        cooldownTimer.cancel();
    }

    @Override
    public void onResume(){
        super.onResume();
        startTimer();
    }

    public void startTimer(){
        cooldownTimer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                updateDebug();
                if (cooldown > 0){
                    cooldown -= 1;
                }
            }
        }, 1000, 1000);
    }

    @Override
    protected void setupStreamingModePipeline(InputSource inputSource) {
        super.setupStreamingModePipeline(inputSource);
        Hands hands = getHands();
        hands.setResultListener(
                handsResult -> {
                    checkGesture(handsResult);
                    logWristLandmark(handsResult, /*showPixelValues=*/ false);
                    SolutionGlSurfaceView<HandsResult> glSurfaceView = getGlSurfaceView();
                    getGlSurfaceView().setRenderData(handsResult);
                    getGlSurfaceView().requestRender();
                    setGlSurfaceView(glSurfaceView);
                });
        setHands(hands);
    }

    private void checkGesture(HandsResult result){
        if (cooldown != 0){
            return;
        }
        if (result.multiHandLandmarks().isEmpty()) {
            return;
        }

        if(isRecognizingGesture(result)) {
            if(gesture == GestureTypes.CLOSED_FINGERS) {
                // PULL
                // TO uncomment when everything else is working, not before
                // else it will kill my credit card :)
                // postMessageToEveryone("myUsername", "message", "openableBy");
                animateColor("#FF0000");
                coolToast("START GESTURE RECOGNIZED");
                counterFingerOpen = 0;
                counterFingerTogether = 0;
                gesture = GestureTypes.NO_GESTURE_INITIALIZED;
            } else if ( gesture == GestureTypes.OPEN_FINGERS) {
                // DROP
                // TO uncomment when everything else is working, not before
                // else it will kill my credit card :)
                //retrieveMessage("username", "requester");
                animateColor("#00FF00");
                coolToast("RECEIVE GESTURE RECOGNIZED");
                counterFingerOpen = 0;
                counterFingerTogether = 0;
                gesture = GestureTypes.NO_GESTURE_INITIALIZED;
            }
            Log.d("cool", "gesture recogized");
            //list_z_coordinates.clear();
            cooldown = N_SECONDS_COOLDOWN;
        }
    }

    private void updateDebug(){
        runOnUiThread(new Runnable() {
            public void run()
            {
                TextView t = ((TextView)findViewById(R.id.debug1));
                if (t != null){
                    String s = "Cooldown: " + cooldown + "\n Gesture: " + gesture;
                    t.setText(s);
                }
            }
        });
    }

    private void coolToast(String str){
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setGestureFingerOpen(){
        if(gesture != GestureTypes.OPEN_FINGERS){
            counterFingerOpen = 0;
        }
        counterFingerOpen++;
        gesture = GestureTypes.OPEN_FINGERS;
    }

    private void setGestureFingerTogether(){
        if(gesture != GestureTypes.CLOSED_FINGERS){
            counterFingerTogether = 0;
        }
        counterFingerTogether++;
        gesture = GestureTypes.CLOSED_FINGERS;
    }

    private boolean AreThreeFingersTogether(float[] finger1, float[] finger2, float[] finger3) {
        // TODO PIA
        float x_coordinate_difference_pairwise_sum = Math.abs(finger1[0] - finger2[0]) + Math.abs(finger1[0] - finger3[0]) + Math.abs(finger2[0] - finger3[0]);
        float y_coordinate_difference_pairwise_sum = Math.abs(finger1[1] - finger2[1]) + Math.abs(finger1[1] - finger3[1]) + Math.abs(finger2[1] - finger3[1]);
        //float z_coordinate_difference_pairwise_sum = Math.abs(finger1[2] - finger2[2]) + Math.abs(finger1[2] - finger3[2]) + Math.abs(finger2[2] - finger3[2]);
        boolean x_close = x_coordinate_difference_pairwise_sum <= DISTANCE_THRESHOLD ;
        boolean y_close = y_coordinate_difference_pairwise_sum  <= DISTANCE_THRESHOLD;
        //float z_close = z_coordinate_difference_pairwise_sum;// <= 0.25;

        boolean together = (x_close) && (y_close);

        myViewer("x_close :" + x_close);
        myViewer("y_close :" + y_close);
        //myViewer("z_close :" + z_close);
       // myViewer("together :" + together);

        return together; // together;
    }

    private boolean AreThreeFingersOpen(float[] finger1, float[] finger2, float[] finger3) {
        // TODO PIA
        float x_coordinate_difference_pairwise_sum = Math.abs(finger1[0] - finger2[0]) + Math.abs(finger1[0] - finger3[0]) + Math.abs(finger2[0] - finger3[0]);
        float y_coordinate_difference_pairwise_sum = Math.abs(finger1[1] - finger2[1]) + Math.abs(finger1[1] - finger3[1]) + Math.abs(finger2[1] - finger3[1]);
        //float z_coordinate_difference_pairwise_sum = Math.abs(finger1[2] - finger2[2]) + Math.abs(finger1[2] - finger3[2]) + Math.abs(finger2[2] - finger3[2]);
        boolean x_open = x_coordinate_difference_pairwise_sum  >  0.25 ;
        boolean y_open = y_coordinate_difference_pairwise_sum  > 0.25;
        //float z_close = z_coordinate_difference_pairwise_sum;// <= 0.25;

        boolean open = (x_open) || (y_open);

        myViewer("x_open :" + x_open);
        myViewer("y_open :" + y_open);
        //myViewer("z_close :" + z_close);
        // myViewer("together :" + together);

        return open; // together;
    }

    private boolean isRecognizingGesture(HandsResult result) {
        List<LandmarkProto.NormalizedLandmark> landmarkList = result.multiHandLandmarks().get(0).getLandmarkList();
        // See here https://google.github.io/mediapipe/solutions/hands.html#hand-landmark-model
        float[] thumb_tip = {landmarkList.get(4).getX(), landmarkList.get(4).getY(), landmarkList.get(4).getZ()};
        float[] index_finger_tip = {landmarkList.get(8).getX(), landmarkList.get(8).getY(), landmarkList.get(8).getZ()};
        float[] middle_finger_tip = {landmarkList.get(12).getX(), landmarkList.get(12).getY(), landmarkList.get(12).getZ()};
        float[] ring_finger_tip = {landmarkList.get(16).getX(), landmarkList.get(16).getY(), landmarkList.get(16).getZ()};

        boolean threeFingersTogether = AreThreeFingersTogether(thumb_tip, index_finger_tip, middle_finger_tip) || AreThreeFingersTogether(thumb_tip, middle_finger_tip, ring_finger_tip);
        boolean threeFingersOpen = AreThreeFingersOpen(thumb_tip, index_finger_tip, middle_finger_tip) || AreThreeFingersOpen(thumb_tip, middle_finger_tip, ring_finger_tip);

        if (threeFingersTogether) {
            setGestureFingerTogether();
        } else if (threeFingersOpen) {
            setGestureFingerOpen();
        } else {
            gesture = GestureTypes.NO_GESTURE_INITIALIZED;
            counterFingerOpen = 0;
            counterFingerTogether = 0;
        }

        return (counterFingerOpen > N_FRAMES_TO_CHANGE) && (counterFingerTogether > N_FRAMES_TO_CHANGE);
    }

    private void postMessageToEveryone(String myUsername, String message, String openableBy) {
        Call<APIStructures.Message> call = RetrofitClient.getInstance().getMyApi().sendMessageToEveryone(myUsername, message, openableBy);
        Log.d("d", call.request().toString());
        call.enqueue(new Callback<APIStructures.Message>() {
            @Override
            public void onResponse(Call<APIStructures.Message> call, Response<APIStructures.Message> response) {
                APIStructures.Message result = response.body();
                Log.d("d", response.toString());
                try {
                    myViewer("Message uploaded.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<APIStructures.Message> call, Throwable t) {
                myViewer("ERROR on upload.");
            }
        });
    }

    private void retrieveMessage(String username, String requester) {
        Call<APIStructures.MessageOut> call = RetrofitClient.getInstance().getMyApi().getMessages(username, requester);
        call.enqueue(new Callback<APIStructures.MessageOut>() {

            @Override
            public void onResponse(Call<APIStructures.MessageOut> call, Response<APIStructures.MessageOut> response) {
                APIStructures.MessageOut result = response.body();
                try {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<APIStructures.MessageOut> call, Throwable t) {
                myViewer("ERROR");
            }
        });
    }

    public void myViewer(String str) {
        // TODO: add View to render Information text. Toast is not working here
        Log.d("GESTURE", str);
    }
    public void animateColor(String color) {
        FrameLayout frameLayout = findViewById(R.id.michel2);
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
        frameLayout.setAlpha(1);
        frameLayout.setBackgroundColor(Color.parseColor(color));
        frameLayout.startAnimation(animation1);
    }

}
