package com.example.help_buddy_chat_app.Chatting;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
//import android.view.ActionMode;
import androidx.appcompat.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.help_buddy_chat_app.Common.Constants;
import com.example.help_buddy_chat_app.R;
import com.example.help_buddy_chat_app.SelectFriend.SelectFriendActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<MessageModel> messageModelList;

    private ActionMode actionMode;
    private ConstraintLayout selectedView;

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

            if(message.getMessageType().equals(Constants.messageTypeText))
            {
                holder.linearSend.setVisibility(View.VISIBLE);
                holder.llSentImage.setVisibility(View.GONE);
            }
            else
            {
                holder.linearSend.setVisibility(View.GONE);
                holder.llSentImage.setVisibility(View.VISIBLE);
            }

            holder.linearReceived.setVisibility(View.GONE);
            holder.llReceivedImage.setVisibility(View.GONE);

            //Display message and message time on screen
            holder.tvSentMessage.setText(message.getMessage());
            holder.tvSentMessageTime.setText(messageTime);
            holder.tvSentImageTime.setText(messageTime);
            //show image shared
            Glide.with(context)
                    .load(message.getMessage())
                    .placeholder(R.drawable.ic_image)
                    .into(holder.ivSent);
        }
        else
        {
            //a message sent by another user
            //message to be displayed on the left-hand side of the screen

            if(message.getMessageType().equals(Constants.messageTypeText))
            {
                holder.linearReceived.setVisibility(View.VISIBLE);
                holder.llReceivedImage.setVisibility(View.GONE);
            }
            else
            {
                holder.linearReceived.setVisibility(View.GONE);
                holder.llReceivedImage.setVisibility(View.VISIBLE);
            }

            holder.linearSend.setVisibility(View.GONE);
            holder.llSentImage.setVisibility(View.GONE);

            //Display message and message time on screen
            holder.tvReceivedMessage.setText(message.getMessage());
            holder.tvReceivedMessageTime.setText(messageTime);
            holder.tvReceivedImageTime.setText(messageTime);
            //show image shared
            Glide.with(context)
                    .load(message.getMessage())
                    .placeholder(R.drawable.ic_image)
                    .into(holder.ivReceived);
        }

        holder.clMessage.setTag(R.id.TAG_MESSAGE, message.getMessage());
        holder.clMessage.setTag(R.id.TAG_MESSAGE_ID, message.getMessageId());
        holder.clMessage.setTag(R.id.TAG_MESSAGE_TYPE, message.getMessageType());

        holder.clMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageType = view.getTag(R.id.TAG_MESSAGE_TYPE).toString();
                Uri uri = Uri.parse(view.getTag(R.id.TAG_MESSAGE).toString());

                if(messageType.equals(Constants.messageTypeVideo))
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setDataAndType(uri, "video/mp4");
                    context.startActivity(intent);
                }
                if(messageType.equals(Constants.messageTypeImage))
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setDataAndType(uri, "image/jpg");
                    context.startActivity(intent);
                }
            }
        });

        holder.clMessage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                if(actionMode != null)
                    return false;

                selectedView = holder.clMessage;

                actionMode = ((AppCompatActivity)context).startSupportActionMode(actionModeCallBack);

                holder.clMessage.setBackgroundColor(context.getResources().getColor(R.color.color_accent));

                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder{

        //Declare message layouts on screen
        private LinearLayout linearSend, linearReceived, llSentImage, llReceivedImage;
        //Declare text views to display messages and time messages were received and sent
        private TextView tvSentMessage, tvSentMessageTime, tvReceivedMessage, tvReceivedMessageTime;
        //Declare constraint layout used to display each message on the screen
        private ConstraintLayout clMessage;
        //Image view to display images sent/received
        private ImageView ivSent, ivReceived;
        //Text views to show time images were sent/received
        private TextView tvSentImageTime, tvReceivedImageTime;

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

            llSentImage = itemView.findViewById(R.id.llSentImage);
            llReceivedImage = itemView.findViewById(R.id.llReceivedImage);

            ivSent = itemView.findViewById(R.id.ivSent);
            tvSentImageTime = itemView.findViewById(R.id.tvSentImageTime);

            ivReceived = itemView.findViewById(R.id.ivReceived);
            tvReceivedImageTime = itemView.findViewById(R.id.tvReceivedImageTime);
        }
    }

    public ActionMode.Callback actionModeCallBack = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {

            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.meu_chat_options, menu);
            String selectedMessageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));

            if(selectedMessageType.equals(Constants.messageTypeText))
            {
                MenuItem itemDownLoad = menu.findItem(R.id.menuDownload);
                itemDownLoad.setVisible(false);
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {

            //message details
            String selectedMessageID = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_ID));
            String selectedMessageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));
            String selectedMessage = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE));

            int itemId = menuItem.getItemId();

            switch (itemId)
            {
                case R.id.menuDelete:
                    if(context instanceof ChattingActivity)
                    {
                        ((ChattingActivity)context).deleteMessage(selectedMessageID, selectedMessageType);
                    }
                    actionMode.finish();
                    break;

                case R.id.menuDownload:
                    if(context instanceof ChattingActivity)
                    {
                        ((ChattingActivity)context).downloadFile(selectedMessageID, selectedMessageType, false);
                    }
                    actionMode.finish();
                    break;

                case R.id.menuShare:
                    if(selectedMessageType.equals(Constants.messageTypeText))
                    {
                        //sharing text message
                        Intent intentShare = new Intent();
                        intentShare.setAction(Intent.ACTION_SEND);
                        intentShare.putExtra(Intent.EXTRA_TEXT, selectedMessage);
                        intentShare.setType("text/plain");
                        context.startActivity(intentShare);
                    }
                    else
                    {
                        //sharing videos/images
                        if(context instanceof ChattingActivity)
                        {
                            ((ChattingActivity)context).downloadFile(selectedMessageID, selectedMessageType, true);
                        }
                    }
                    actionMode.finish();
                    break;

                case R.id.menuForward:
                    if(context instanceof ChattingActivity)
                    {
                        ((ChattingActivity)context).forwardMessage(selectedMessageID, selectedMessage, selectedMessageType);
                    }
                    actionMode.finish();
                    break;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionMode = null;
            selectedView.setBackgroundColor(context.getResources().getColor(R.color.chat_background));
        }
    };
}
