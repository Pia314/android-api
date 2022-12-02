// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.apicalltest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.google.mediapipe.formats.proto.LandmarkProto.Landmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutioncore.VideoInput;
import com.google.mediapipe.solutions.hands.HandLandmark;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;
import com.google.mediapipe.solutions.hands.HandsResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.apicalltest.APIStructures.Message;
import com.example.apicalltest.APIStructures.MessageOut;
import com.example.apicalltest.ActionGestures.*;

/** Main activity of MediaPipe Hands app. */
public class HandsActivity extends AppCompatActivity {
  private static final String TAG = "HandsActivity";

  private static double DISTANCE = 0.2;
  private static double Z_DISTANCE = 0.3;

  private boolean isSendingMessage = false;

  private boolean isReceivingMessage = false;

  boolean canRecognize = true;

  private List<Float> list_z_coordinates=new ArrayList<Float>();

  private Hands hands;
  // Run the pipeline and the model inference on GPU or CPU.
  private static final boolean RUN_ON_GPU = true;

  private enum InputSource {
    UNKNOWN,
    IMAGE,
    VIDEO,
    CAMERA,
  }

  private InputSource inputSource = InputSource.UNKNOWN;

  // Image demo UI and image loader components.
  private ActivityResultLauncher<Intent> imageGetter;
  private HandsResultImageView imageView;
  // Video demo UI and video loader components.
  private VideoInput videoInput;
  private ActivityResultLauncher<Intent> videoGetter;
  // Live camera demo UI and camera components.
  private CameraInput cameraInput;

  private SolutionGlSurfaceView<HandsResult> glSurfaceView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hands);
    setupStaticImageDemoUiComponents();
    setupVideoDemoUiComponents();
    setupLiveDemoUiComponents();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (inputSource == InputSource.CAMERA) {
      // Restarts the camera and the opengl surface rendering.
      cameraInput = new CameraInput(this);
      cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
      glSurfaceView.post(this::startCamera);
      glSurfaceView.setVisibility(View.VISIBLE);
    } else if (inputSource == InputSource.VIDEO) {
      videoInput.resume();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (inputSource == InputSource.CAMERA) {
      glSurfaceView.setVisibility(View.GONE);
      cameraInput.close();
    } else if (inputSource == InputSource.VIDEO) {
      videoInput.pause();
    }
  }

  private Bitmap downscaleBitmap(Bitmap originalBitmap) {
    double aspectRatio = (double) originalBitmap.getWidth() / originalBitmap.getHeight();
    int width = imageView.getWidth();
    int height = imageView.getHeight();
    if (((double) imageView.getWidth() / imageView.getHeight()) > aspectRatio) {
      width = (int) (height * aspectRatio);
    } else {
      height = (int) (width / aspectRatio);
    }
    return Bitmap.createScaledBitmap(originalBitmap, width, height, false);
  }

  private Bitmap rotateBitmap(Bitmap inputBitmap, InputStream imageData) throws IOException {
    int orientation =
            new ExifInterface(imageData)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    if (orientation == ExifInterface.ORIENTATION_NORMAL) {
      return inputBitmap;
    }
    Matrix matrix = new Matrix();
    switch (orientation) {
      case ExifInterface.ORIENTATION_ROTATE_90:
        matrix.postRotate(90);
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        matrix.postRotate(180);
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        matrix.postRotate(270);
        break;
      default:
        matrix.postRotate(0);
    }
    return Bitmap.createBitmap(
            inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);
  }

  /**
   * Sets up the UI components for the static image demo.
   */
  private void setupStaticImageDemoUiComponents() {
    // The Intent to access gallery and read images as bitmap.
    imageGetter =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                      Intent resultIntent = result.getData();
                      if (resultIntent != null) {
                        if (result.getResultCode() == RESULT_OK) {
                          Bitmap bitmap = null;
                          try {
                            bitmap =
                                    downscaleBitmap(
                                            MediaStore.Images.Media.getBitmap(
                                                    this.getContentResolver(), resultIntent.getData()));
                          } catch (IOException e) {
                            Log.e(TAG, "Bitmap reading error:" + e);
                          }
                          try {
                            InputStream imageData =
                                    this.getContentResolver().openInputStream(resultIntent.getData());
                            bitmap = rotateBitmap(bitmap, imageData);
                          } catch (IOException e) {
                            Log.e(TAG, "Bitmap rotation error:" + e);
                          }
                          if (bitmap != null) {
                            hands.send(bitmap);
                          }
                        }
                      }
                    });
    Button loadImageButton = findViewById(R.id.button_load_picture);
    loadImageButton.setOnClickListener(
            v -> {
              if (inputSource != InputSource.IMAGE) {
                stopCurrentPipeline();
                setupStaticImageModePipeline();
              }
              // Reads images from gallery.
              Intent pickImageIntent = new Intent(Intent.ACTION_PICK);
              pickImageIntent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
              imageGetter.launch(pickImageIntent);
            });
    imageView = new HandsResultImageView(this);
  }

  /**
   * Sets up core workflow for static image mode.
   */
  private void setupStaticImageModePipeline() {
    this.inputSource = InputSource.IMAGE;
    // Initializes a new MediaPipe Hands solution instance in the static image mode.
    hands =
            new Hands(
                    this,
                    HandsOptions.builder()
                            .setStaticImageMode(true)
                            .setMaxNumHands(2)
                            .setRunOnGpu(RUN_ON_GPU)
                            .build());

    // Connects MediaPipe Hands solution to the user-defined HandsResultImageView.
    hands.setResultListener(
            handsResult -> {
              logWristLandmark(handsResult, /*showPixelValues=*/ true);
              imageView.setHandsResult(handsResult);
              runOnUiThread(() -> imageView.update());
            });
    hands.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Hands error:" + message));

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    frameLayout.removeAllViewsInLayout();
    imageView.setImageDrawable(null);
    frameLayout.addView(imageView);
    imageView.setVisibility(View.VISIBLE);
  }

  /**
   * Sets up the UI components for the video demo.
   */
  private void setupVideoDemoUiComponents() {
    // The Intent to access gallery and read a video file.
    videoGetter =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                      Intent resultIntent = result.getData();
                      if (resultIntent != null) {
                        if (result.getResultCode() == RESULT_OK) {
                          glSurfaceView.post(
                                  () ->
                                          videoInput.start(
                                                  this,
                                                  resultIntent.getData(),
                                                  hands.getGlContext(),
                                                  glSurfaceView.getWidth(),
                                                  glSurfaceView.getHeight()));
                        }
                      }
                    });
    Button loadVideoButton = findViewById(R.id.button_load_video);
    loadVideoButton.setOnClickListener(
            v -> {
              stopCurrentPipeline();
              setupStreamingModePipeline(InputSource.VIDEO);
              // Reads video from gallery.
              Intent pickVideoIntent = new Intent(Intent.ACTION_PICK);
              pickVideoIntent.setDataAndType(MediaStore.Video.Media.INTERNAL_CONTENT_URI, "video/*");
              videoGetter.launch(pickVideoIntent);
            });
  }

  /**
   * Sets up the UI components for the live demo with camera input.
   */
  private void setupLiveDemoUiComponents() {
    Button startCameraButton = findViewById(R.id.button_start_camera);
    startCameraButton.setOnClickListener(
            v -> {
              if (inputSource == InputSource.CAMERA) {
                return;
              }
              stopCurrentPipeline();
              Toast.makeText(getApplicationContext(), "is Receiving Message is true", Toast.LENGTH_LONG).show();
              setupStreamingModePipeline(InputSource.CAMERA);
            });
  }

  /**
   * Sets up core workflow for streaming mode.
   */
  private void setupStreamingModePipeline(InputSource inputSource) {
    this.inputSource = inputSource;
    // Initializes a new MediaPipe Hands solution instance in the streaming mode.
    hands =
            new Hands(
                    this,
                    HandsOptions.builder()
                            .setStaticImageMode(false)
                            .setMaxNumHands(2)
                            .setRunOnGpu(RUN_ON_GPU)
                            .build());
    hands.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Hands error:" + message));

    if (inputSource == InputSource.CAMERA) {
      cameraInput = new CameraInput(this);
      cameraInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
    } else if (inputSource == InputSource.VIDEO) {
      videoInput = new VideoInput(this);
      videoInput.setNewFrameListener(textureFrame -> hands.send(textureFrame));
    }

    // Initializes a new Gl surface view with a user-defined HandsResultGlRenderer.
    glSurfaceView =
            new SolutionGlSurfaceView<>(this, hands.getGlContext(), hands.getGlMajorVersion());
    glSurfaceView.setSolutionResultRenderer(new HandsResultGlRenderer());
    glSurfaceView.setRenderInputImage(true);
    hands.setResultListener(
            handsResult -> {
              logWristLandmark(handsResult, /*showPixelValues=*/ false);
              glSurfaceView.setRenderData(handsResult);
              glSurfaceView.requestRender();
            });

    // The runnable to start camera after the gl surface view is attached.
    // For video input source, videoInput.start() will be called when the video uri is available.
    if (inputSource == InputSource.CAMERA) {
      glSurfaceView.post(this::startCamera);
    }

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    imageView.setVisibility(View.GONE);
    frameLayout.removeAllViewsInLayout();
    frameLayout.addView(glSurfaceView);
    glSurfaceView.setVisibility(View.VISIBLE);
    frameLayout.requestLayout();
  }

  private void startCamera() {
    cameraInput.start(
            this,
            hands.getGlContext(),
            CameraInput.CameraFacing.FRONT,
            glSurfaceView.getWidth(),
            glSurfaceView.getHeight());
  }

  private void stopCurrentPipeline() {
    if (cameraInput != null) {
      cameraInput.setNewFrameListener(null);
      cameraInput.close();
    }
    if (videoInput != null) {
      videoInput.setNewFrameListener(null);
      videoInput.close();
    }
    if (glSurfaceView != null) {
      glSurfaceView.setVisibility(View.GONE);
    }
    if (hands != null) {
      hands.close();
    }
  }

  private void logWristLandmark(HandsResult result, boolean showPixelValues) {
    if (result.multiHandLandmarks().isEmpty()) {
      return;
    }
    NormalizedLandmark wristLandmark =
            result.multiHandLandmarks().get(0).getLandmarkList().get(HandLandmark.WRIST);
    // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
    if (showPixelValues) {
      int width = result.inputBitmap().getWidth();
      int height = result.inputBitmap().getHeight();
      Log.i(
              TAG,
              String.format(
                      "MediaPipe Hand wrist coordinates (pixel values): x=%f, y=%f",
                      wristLandmark.getX() * width, wristLandmark.getY() * height));
    } else {
      Log.i(
              TAG,
              String.format(
                      "MediaPipe Hand wrist normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                      wristLandmark.getX(), wristLandmark.getY()));
    }
    if (result.multiHandWorldLandmarks().isEmpty()) {
      return;
    }
    Landmark wristWorldLandmark =
            result.multiHandWorldLandmarks().get(0).getLandmarkList().get(HandLandmark.WRIST);
    Log.i(
            TAG,
            String.format(
                    "MediaPipe Hand wrist world coordinates (in meters with the origin at the hand's"
                            + " approximate geometric center): x=%f m, y=%f m, z=%f m",
                    wristWorldLandmark.getX(), wristWorldLandmark.getY(), wristWorldLandmark.getZ()));

    if(isStartSharingPosition(result)){
      list_z_coordinates.clear();
      isSendingMessage = true;
      //TODO Make it working
      //stopCurrentPipeline();
      if(canRecognize) {
        //Toast.makeText(getApplicationContext(), "is Receiving Message is true", Toast.LENGTH_LONG).show();
        canRecognize = false;
      }
      //
     // postMessageToEveryone("myUsername", "message", "openableBy");
    }

    if(isReceivingPosition(result)){
      list_z_coordinates.clear();
      isReceivingMessage = true;
      //TODO make ist working
      //stopCurrentPipeline();
      //Toast.makeText(getApplicationContext(), "is Receiving Message is true", Toast.LENGTH_LONG).show();
      //retrieveMessage("username", "requester");
      Log.d(
              "test", "te5st");

      if(canRecognize) {
       // Toast.makeText(getApplicationContext(), "is Receiving Message is true", Toast.LENGTH_LONG).show();
        canRecognize = false;
      }
    }
  }

  private boolean isStartSharingPosition(HandsResult result) {
    // here i want to code if the start position (first three landmarks are together and changing z coordinates in the right direction)

    //to access the landmarks
    List<NormalizedLandmark> landmarkList = result.multiHandLandmarks().get(0).getLandmarkList();
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
    // here i want to code if receiving position (first three landmarks are together and changing z coordinates in the right (going closer to the screen) direction)

    //to access the landmarks
    List<NormalizedLandmark> landmarkList = result.multiHandLandmarks().get(0).getLandmarkList();
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
    Call<MessageOut> call = RetrofitClient.getInstance().getMyApi().getMessages(username, requester);
    call.enqueue(new Callback<MessageOut>() {

      @Override
      public void onResponse(Call<MessageOut> call, Response<MessageOut> response) {
        MessageOut result = response.body();
        try {
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

  public void myViewer(String str) {
    // TODO: add View to render Information text. Toast is not working here
    Log.d("d", str);
  }
  public void animateColor(String color) {
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
    frameLayout.setAlpha(1);
    frameLayout.setBackgroundColor(Color.parseColor(color));
    frameLayout.startAnimation(animation1);
  }
  /*public void animateColor(String color){
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
    frameLayout.setAlpha(1);
    frameLayout.setBackgroundColor(Color.parseColor(color));
    frameLayout.startAnimation(animation1);

}
*/
}
