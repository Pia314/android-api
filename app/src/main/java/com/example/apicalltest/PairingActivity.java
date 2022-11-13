package com.example.apicalltest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class PairingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
    }

    public void selectUsername(View view){
        // TODO mainactivity.username = what is in edittext; + check api
    }
}