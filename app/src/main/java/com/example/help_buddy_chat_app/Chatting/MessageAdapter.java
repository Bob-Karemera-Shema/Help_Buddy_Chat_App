package com.example.help_buddy_chat_app.Chatting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.example.help_buddy_chat_app.R;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<MessageModel> messageModelList;

    //Database references
    private FirebaseAuth firebaseAuth;

    public MessageAdapter(Context context, List<MessageModel> messageModelList) {
        this.context = context;
        this.messageModelList = messageModelList;
    }

    @NonNull
    @Override
    public MessageAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        //Get a message from message list
        MessageModel message = messageModelList.get(position);

        //Get instance of app database
        firebaseAuth = FirebaseAuth.getInstance();

        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        //userId of the user sending a message as stored on the database
        String fromUserId = message.getMessageFrom();

        //Obtain time message is received from database
        //First retrieve message time and convert it into readable format for user
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String dateTime = simpleDateFormat.format(new Date(message.getMessageTime()));

        String[] splitString = dateTime.split(" ");
        String messageTime = splitString[1];

        if(fromUserId.equals(currentUserId))
        {
            //a message sent by the current user
            //message to be displayed on the right-hand side of the screen
            holder.linearSend.setVisibility(View.VISIBLE);
            holder.linearReceived.setVisibility(View.GONE);

            //Display message and message time on screen
            holder.tvSentMessage.setText(message.getMessage());
            holder.tvSentMessageTime.setText(messageTime);
        }
        else
        {
            //a message sent by another user
            //message to be displayed on the left-hand side of the screen
            holder.linearSend.setVisibility(View.GONE);
            holder.linearReceived.setVisibility(View.VISIBLE);

            //Display message and message time on screen
            holder.tvReceivedMessage.setText(message.getMessage());
            holder.tvReceivedMessageTime.setText(messageTime);
        }
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder{

        //Declare message layouts on screen
        private LinearLayout linearSend, linearReceived;
        //Declare text views to display messages and time messages were received and sent
        private TextView tvSentMessage, tvSentMessageTime, tvReceivedMessage, tvReceivedMessageTime;
        //Declare constraint layout used to display each message on the screen
        private ConstraintLayout clMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            //initialise UI views
            linearSend = itemView.findViewById(R.id.linearSend);
            linearReceived = itemView.findViewById(R.id.linearReceived);

            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentMessageTime = itemView.findViewById(R.id.tvSentMessageTime);

            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedMessageTime = itemView.findViewById(R.id.tvReceivedMessageTime);

            clMessage = itemView.findViewById(R.id.clMessage);
        }
    }
}
