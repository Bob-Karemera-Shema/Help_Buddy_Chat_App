/*
I should be noted that the development of this application followed a Firebase Advanced - Real
Time Chat App in Android Studio 2021.
It is available on tutorialspoint.com
Link to the course: https://www.tutorialspoint.com/firebase_advanced_real_time_chat_app_in_android_studio_2021/index.asp
*/

package com.example.help_buddy_chat_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.help_buddy_chat_app.Login.LoginActivity;

public class WelcomeActivity extends AppCompatActivity {
    //Class to run animation when app is starting up
    //animation consists of app logo and name

    //Declare UI components
    private ImageView ivWelcome;
    private TextView tvWelcome;

    //Object to load animation file
    private Animation welcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //hide actionbar
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().hide();
        }

        //get UI components from xml file
        ivWelcome = findViewById(R.id.ivWelcome);
        tvWelcome = findViewById(R.id.tvWelcome);

        //load animation file to animation object
        welcome = AnimationUtils.loadAnimation(this, R.anim.welcome_animation);

        welcome.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ivWelcome.startAnimation(welcome);
        tvWelcome.startAnimation(welcome);
    }
}