package com.example.help_buddy_chat_app.Notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.help_buddy_chat_app.Common.Connection;
import com.example.help_buddy_chat_app.Common.Constants;
import com.example.help_buddy_chat_app.Login.LoginActivity;
import com.example.help_buddy_chat_app.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ChatMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Connection.updateDeviceToken(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = message.getData().get(Constants.notification_title);
        String newMessage = message.getData().get(Constants.notification_message);

        //Redirect user to chats fragment
        Intent intent = new Intent(this, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        //notifications alert sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        //Build a notification
        NotificationCompat.Builder notificationBuilder;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(Constants.channel_id,
                    Constants.channel_name, NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(Constants.channel_description);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder = new NotificationCompat.Builder(this, Constants.channel_id);
        }
        else
        {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        //define notification properties
        notificationBuilder.setSmallIcon(R.drawable.ic_app_logo);
        notificationBuilder.setColor(getResources().getColor(R.color.color_primary_dark));
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setAutoCancel(true);  //once the user clicks on the notification, it is removed from notification bar
        notificationBuilder.setSound(defaultSoundUri);
        notificationBuilder.setContentIntent(pendingIntent);

        if(newMessage.startsWith("https://firebasestorage."))
        {
            //if the message is a video/image, show a thumbnail
            try
            {
                NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
                Glide.with(this)
                        .asBitmap()
                        .load(newMessage)
                        .into(new CustomTarget<Bitmap>(200, 100) {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                bigPictureStyle.bigPicture(resource);
                                notificationBuilder.setStyle(bigPictureStyle);
                                notificationManager.notify(999, notificationBuilder.build());
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });
            }
            catch(Exception e)
            {
                notificationBuilder.setContentText("New File Received");
            }
        }
        else
        {
            notificationBuilder.setContentText(newMessage);
            notificationManager.notify(999, notificationBuilder.build());
        }
    }
}