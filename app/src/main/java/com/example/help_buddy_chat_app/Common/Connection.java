package com.example.help_buddy_chat_app.Common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.help_buddy_chat_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

    public static void updateDeviceToken(Context context, String token)
    {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if(currentUser != null)
        {
            DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
            DatabaseReference tokenReference = rootReference.child(NodeNames.tokens)
                    .child(currentUser.getUid());

            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put(NodeNames.device_token, token);

            tokenReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(!task.isSuccessful())
                    {
                        Toast.makeText(context, R.string.failed_to_register_token,Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            });
        }
    }

    public static void sendNotification(Context context, String title, String message, String userId)
    {
        DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference tokenReference = rootReference.child(NodeNames.tokens).child(userId);

        tokenReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(NodeNames.device_token).getValue() != null)
                {
                    String deviceToken = snapshot.child(NodeNames.device_token).getValue().toString();

                    JSONObject notification = new JSONObject();
                    JSONObject notificationData = new JSONObject();

                    try
                    {
                        notificationData.put(Constants.notification_title, title);
                        notificationData.put(Constants.notification_message, message);

                        notification.put(Constants.notification_to, deviceToken);
                        notification.put(Constants.notification_data, notificationData);

                        String FCMApiUrl = "https://fcm.googleapis.com/fcm/send";
                        String contentType = "application/json";

                        Response.Listener successListener = new Response.Listener() {
                            @Override
                            public void onResponse(Object response) {
                                Toast.makeText(context, "Notification Sent", Toast.LENGTH_SHORT).show();
                            }
                        };

                        Response.ErrorListener failureListener = new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(context,
                                        context.getString(R.string.notification_not_sent, error.getMessage()),
                                        Toast.LENGTH_SHORT).show();
                            }
                        };

                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(FCMApiUrl,
                                notification, successListener, failureListener){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {

                                Map<String, String> parameters = new HashMap<>();
                                parameters.put("Authorization", "key = " + Constants.firebaseKey);
                                parameters.put("Sender", "key= " + Constants.sender_ID);
                                parameters.put("Content-Type",contentType);

                                return parameters;
                            }
                        };

                        RequestQueue requestQueue = Volley.newRequestQueue(context);
                        requestQueue.add(jsonObjectRequest);
                    }
                    catch (JSONException e) {
                        Toast.makeText(context,
                                context.getString(R.string.notification_not_sent, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context,
                        context.getString(R.string.notification_not_sent, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void updateChatFeatures(Context context, String currentUserId, String chatUserId, String lastMessage)
    {
        DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chatReference = rootReference.child(NodeNames.chats)
                .child(chatUserId).child(currentUserId);

        chatReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentCount = "0";
                if(snapshot.child(NodeNames.unread_count).getValue() != null)
                {
                    currentCount = snapshot.child(NodeNames.unread_count).getValue().toString();
                }

                Map chatMap = new HashMap();
                chatMap.put(NodeNames.time_stamp, ServerValue.TIMESTAMP);
                chatMap.put(NodeNames.unread_count, Integer.valueOf(currentCount)+1);
                chatMap.put(NodeNames.last_message, lastMessage);
                chatMap.put(NodeNames.last_message_time, ServerValue.TIMESTAMP);

                Map chatUserMap = new HashMap();
                chatUserMap.put(NodeNames.chats + "/" + chatUserId + "/" + currentUserId, chatMap);

                rootReference.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if(error != null)
                            Toast.makeText(context,
                                    context.getString(R.string.something_went_wrong, error.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context,
                        context.getString(R.string.something_went_wrong, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static String getTime(long time)
    {
        final int milliseconds = 1000;
        final int minute_in_milliseconds = 60 * milliseconds;
        final int hour_in_milliseconds = 60 * minute_in_milliseconds;
        final int day_in_milliseconds = 24 * hour_in_milliseconds;

        time *= 1000;

        long now = System.currentTimeMillis();

        /*if(time > now || time <= 0)
        {
            return "";
        }*/

        final long time_difference = now - time;

        if(time_difference < minute_in_milliseconds)
        {
            return "just now";
        }
        if(time_difference < 2 * minute_in_milliseconds)
        {
            return "a minute ago";
        }
        if(time_difference < 59 * minute_in_milliseconds)
        {
            return time_difference/minute_in_milliseconds + " minutes ago";
        }
        if(time_difference < 90 * minute_in_milliseconds)
        {
            return "an hour ago";
        }
        if( time_difference < 24 * hour_in_milliseconds)
        {
            return time_difference/hour_in_milliseconds + " hours ago";
        }
        if(time_difference < 48 * hour_in_milliseconds)
        {
            return "yesterday";
        }
        return time_difference/day_in_milliseconds + " days ago";
    }

}
