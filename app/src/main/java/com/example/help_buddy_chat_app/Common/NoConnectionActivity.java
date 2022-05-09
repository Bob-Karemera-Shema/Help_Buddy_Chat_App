package com.example.help_buddy_chat_app.Common;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.help_buddy_chat_app.Common.Connection;
import com.example.help_buddy_chat_app.R;

public class NoConnectionActivity extends AppCompatActivity {

    //Declare UI components
    private TextView tVNoConnection;
    private ProgressBar progressBarNotify;

    //Create object of ConnectivityManager.NetworkCallback to get info about internet connection on a device
    private ConnectivityManager.NetworkCallback networkCallback;
    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_connection);

        tVNoConnection = findViewById(R.id.tVNoConnection);
        progressBarNotify = findViewById(R.id.progressBarNotify);

        checkConnectivity();
    }

    private void checkConnectivity()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            //if user device API is greater or equal to 21
            networkCallback = new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(@NonNull Network network) {
                    //if connection is available, go back to previous page
                    super.onAvailable(network);
                    finish();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    //if no connection, notify user
                    super.onLost(network);
                    tVNoConnection.setText(R.string.not_connected);
                }
            };

            ConnectivityManager connectivityManager  = (ConnectivityManager)
                    getSystemService(CONNECTIVITY_SERVICE);

            connectivityManager.registerNetworkCallback(new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(), networkCallback);
        }
    }

    public void retryClick(View view)
    {
        progressBarNotify.setVisibility(View.VISIBLE);

        //check connection
        if(Connection.connected(this))
        {
            //if connected, redirect user to previous page
            finish();
        }
        else{
            //if not connected show progress bar for 1 second and show no connection screen again
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressBarNotify.setVisibility(View.GONE);
                }
            }, 1000);
        }
    }

    public void closeClick(View view)
    {
        this.view = view;
        finishAffinity();
    }
}