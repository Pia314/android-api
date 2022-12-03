package com.example.apicalltest;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyHandActivity extends HandsActivity{
    private List<Float> list_z_coordinates=new ArrayList<Float>();

    protected static double DISTANCE = 0.2;
    protected static double Z_DISTANCE = 0.3;

    Timer cooldownTimer;
    int cooldown = 0;

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
        boolean gestureRecognized = false;

        if(isStartSharingPosition(result)){ // PULL
            gestureRecognized = true;
            // TO uncomment when everything else is wokring, not before
            // else it will kill my credit card :)
            // postMessageToEveryone("myUsername", "message", "openableBy");
            animateColor("#FF0000");
        }
        else if(isReceivingPosition(result)){ // DROP
            gestureRecognized = true;
            // TO uncomment when everything else is wokring, not before
            // else it will kill my credit card :)
            //retrieveMessage("username", "requester");
            animateColor("#00FF00");
        }

        if (gestureRecognized) {
            Log.d("cool", "gesture recogized");
            list_z_coordinates.clear();
            coolToast("GESTURE RECOGNIZED");
            cooldown = 5; // 5 SECOINDS OF COOLDOWN
        }
    }


    private void coolToast(String str){
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isStartSharingPosition(HandsResult result) {
        // TODO PIA
        // here i want to code if the start position (first three landmarks are together and changing z coordinates in the right direction)

        //to access the landmarks
        List<LandmarkProto.NormalizedLandmark> landmarkList = result.multiHandLandmarks().get(0).getLandmarkList();
        // See here https://google.github.io/mediapipe/solutions/hands.html#hand-landmark-model
        float[] thumb_tip = {landmarkList.get(4).getX(), landmarkList.get(4).getY(), landmarkList.get(4).getZ()};
        float[] index_finger_tip = {landmarkList.get(8).getX(), landmarkList.get(8).getY(), landmarkList.get(8).getZ()};
        float[] middle_finger_tip = {landmarkList.get(12).getX(), landmarkList.get(12).getY(), landmarkList.get(12).getZ()};
        float[] ring_finger_tip = {landmarkList.get(16).getX(), landmarkList.get(16).getY(), landmarkList.get(16).getZ()};
        // are three of the for point near together?
        boolean threeFingersTogether = AreThreeFingersTogether(thumb_tip, index_finger_tip, middle_finger_tip) || AreThreeFingersTogether(thumb_tip, middle_finger_tip, ring_finger_tip);

        if (threeFingersTogether) {
            // if yes add mean of z coordinates to List
            float mean_z_coordinates = (thumb_tip[2] + middle_finger_tip[2])/2;
            list_z_coordinates.add(mean_z_coordinates);
            myViewer("mean z coordinates" + mean_z_coordinates );
        }

        if (list_z_coordinates.size() >= 10) {
            myViewer("diff z coordinates" + (list_z_coordinates.get(list_z_coordinates.size()-1) - list_z_coordinates.get(0)));
            myViewer("last z coordinates" + list_z_coordinates.get(list_z_coordinates.size()-1) );
            myViewer("first z coordinates" + list_z_coordinates.get(0));
            if(((list_z_coordinates.get(list_z_coordinates.size()-1) - list_z_coordinates.get(0)) >= Z_DISTANCE) && (list_z_coordinates.get(list_z_coordinates.size()-1) >= list_z_coordinates.get(0))) {
                myViewer("StartSharingPosition");
                return true;
            }
        }

        return false;
    }

    private boolean AreThreeFingersTogether(float[] finger1, float[] finger2, float[] finger3) {
        // TODO PIA
        float x_coordinate_difference_pairwise_sum = Math.abs(finger1[0] - finger2[0]) + Math.abs(finger1[0] - finger3[0]) + Math.abs(finger2[0] - finger3[0]);
        float y_coordinate_difference_pairwise_sum = Math.abs(finger1[1] - finger2[1]) + Math.abs(finger1[1] - finger3[1]) + Math.abs(finger2[1] - finger3[1]);
        float z_coordinate_difference_pairwise_sum = Math.abs(finger1[2] - finger2[2]) + Math.abs(finger1[2] - finger3[2]) + Math.abs(finger2[2] - finger3[2]);
        boolean x_close = x_coordinate_difference_pairwise_sum <= DISTANCE;
        boolean y_close = x_coordinate_difference_pairwise_sum <= DISTANCE;
        boolean z_close = x_coordinate_difference_pairwise_sum <= DISTANCE;

        boolean together = (x_close) && (y_close) && (z_close);

        myViewer("x_close :" + x_close);
        myViewer("y_close :" + y_close);
        myViewer("z_close :" + z_close);
        myViewer("together :" + together);

        return together;
    }

    private boolean isReceivingPosition(HandsResult result) {
        // TODO PIA
        // here i want to code if receiving position (first three landmarks are together and changing z coordinates in the right (going closer to the screen) direction)

        //to access the landmarks
        List<LandmarkProto.NormalizedLandmark> landmarkList = result.multiHandLandmarks().get(0).getLandmarkList();
        // See here https://google.github.io/mediapipe/solutions/hands.html#hand-landmark-model
        float[] thumb_tip = {landmarkList.get(4).getX(), landmarkList.get(4).getY(), landmarkList.get(4).getZ()};
        float[] index_finger_tip = {landmarkList.get(8).getX(), landmarkList.get(8).getY(), landmarkList.get(8).getZ()};
        float[] middle_finger_tip = {landmarkList.get(12).getX(), landmarkList.get(12).getY(), landmarkList.get(12).getZ()};
        float[] ring_finger_tip = {landmarkList.get(16).getX(), landmarkList.get(16).getY(), landmarkList.get(16).getZ()};

        // are three of the for point near together?
        boolean threeFingersTogether = AreThreeFingersTogether(thumb_tip, index_finger_tip, middle_finger_tip) || AreThreeFingersTogether(thumb_tip, middle_finger_tip, ring_finger_tip);

        if (threeFingersTogether) {

        }

        if (threeFingersTogether) {
            // if yes add mean of z coordinates to List
            float mean_z_coordinates = (thumb_tip[2] + middle_finger_tip[2])/2;
            list_z_coordinates.add(mean_z_coordinates);
        }

        if (list_z_coordinates.size() >= 10) {
            myViewer("diff z coordinates" + (list_z_coordinates.get(list_z_coordinates.size()-1) - list_z_coordinates.get(0)));
            myViewer("last z coordinates" + list_z_coordinates.get(list_z_coordinates.size()-1) );
            myViewer("first z coordinates" + list_z_coordinates.get(0));
            if ( true) { //(list_z_coordinates.get(list_z_coordinates.size()-1) - list_z_coordinates.get(0) >= Z_DISTANCE)  && ( list_z_coordinates.get(list_z_coordinates.size()-1) >= list_z_coordinates.get(0)) ){
                myViewer("ReceivingPosition" );
                return true;
            }
        }

        return false;
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
        Log.d("d", str);
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
