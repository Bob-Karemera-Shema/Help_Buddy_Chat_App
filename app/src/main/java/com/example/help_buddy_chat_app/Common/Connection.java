package com.example.help_buddy_chat_app.Common;

import android.content.Context;
import android.net.ConnectivityManager;

public class Connection {
    //Class to return information about device connectivity to the internet
    public static boolean connected(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connectivityManager != null && connectivityManager.getActiveNetworkInfo() != null)
        {
            //connected
            return connectivityManager.getActiveNetworkInfo().isAvailable();
        }
        else {
            //not connected
            return false;
        }
    }
}
