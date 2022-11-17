package com.example.apicalltest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.example.apicalltest.ui.main.MainFragment;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.w3c.dom.Text;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.apicalltest.APIStructures.Message;
import com.example.apicalltest.APIStructures.MessageOut;
import com.example.apicalltest.APIStructures.StringOut;
import com.example.apicalltest.ui.main.TestFragment;

public class MainActivity extends AppCompatActivity {

    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }

        if (OpenCVLoader.initDebug()) {
            Log.d("myTag", "OpenCV loaded");
        }
        Log.d("myTag", ":)");


    }

    public void postMessageFromMeTo(String myUsername, String destUsername, String message){
        Call<Message> call = RetrofitClient.getInstance().getMyApi().sendMessage(destUsername, myUsername, message, null);
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

    public void retrieveMessageForMe(String username){
        Call<MessageOut> call = RetrofitClient.getInstance().getMyApi().getMessages(username, null);
        call.enqueue(new Callback<MessageOut>() {
            @Override
            public void onResponse(Call<MessageOut> call, Response<MessageOut> response) {
                MessageOut result = response.body();
                try {
                    Message msg = result.get();
                    String msgString = msg.getMessageString();
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

    public void retrieveTestCall(){
        Call<StringOut> call = RetrofitClient.getInstance().getMyApi().getHello();
        call.enqueue(new Callback<StringOut>() {
            @Override
            public void onResponse(Call<StringOut> call, Response<StringOut> response) {
                StringOut result = response.body();
                try {
                    myViewer(result.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<StringOut> call, Throwable t) {
                myViewer("ERROR");
            }

        });
    }

    public void myViewer(String str){
        TextView textView = (TextView) findViewById(R.id.message2);
        textView.setText(str);
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }

    /** Called when the user touches the button */
    public void callApi(View view) {
        TextView textView = (TextView) findViewById(R.id.message2);
        textView.setText("cool");
        retrieveTestCall();
    }

    public void testSend(View view) {
        postMessageFromMeTo("green", "blue", "hi");
    }

    public void testRetrieve(View view){
        retrieveMessageForMe("blue");
    }

    public void goToPairing(View view) {
        Intent nextActivityIntent = new Intent(this, PairingActivity.class);
        startActivity(nextActivityIntent);
    }
    public void goToQRScan(View view) {
        Intent nextActivityIntent = new Intent(this, QRScanActivity.class);
        startActivity(nextActivityIntent);
    }
    public void nextActivity3(View view) {
        Intent nextActivityIntent = new Intent(this, HandsActivity.class);
        startActivity(nextActivityIntent);
    }
    public void goToTest(View view) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.container,TestFragment.newInstance("", ""));
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

