package com.example.help_buddy_chat_app.ChatList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.help_buddy_chat_app.Chatting.ChattingActivity;
import com.example.help_buddy_chat_app.Common.Connection;
import com.example.help_buddy_chat_app.Common.Constants;
import com.example.help_buddy_chat_app.Common.Extras;
import com.example.help_buddy_chat_app.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private Context context;
    private List<ChatModel> chatModelList;

    public ChatListAdapter(Context context, List<ChatModel> chatModelList) {
        this.context = context;
        this.chatModelList = chatModelList;
    }

    @NonNull
    @Override
    public ChatListAdapter.ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_list_layout, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatListAdapter.ChatListViewHolder holder, int position) {

        ChatModel chatModel = chatModelList.get(position);

        holder.userName.setText(chatModel.getUserName());

        //Get user profile picture from firebase storage
        StorageReference fileLocation = FirebaseStorage.getInstance().getReference()
                .child(Constants.imageLocation + "/" + chatModel.getUserPicture());

        fileLocation.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.ivUserProfile);
            }
        });

        //get the last message, last message time and unread message count; and display them on each chat on the chat list
        String lastMessage = chatModel.getLastMessage();
        lastMessage = lastMessage.length()>30 ? lastMessage.substring(0,30) : lastMessage;
        holder.tvLastMessage.setText(lastMessage);

        String lastMessageTime = chatModel.getLastMessageTime();
        if(lastMessageTime == null)
        {
            lastMessageTime = "";
        }
        if(!TextUtils.isEmpty(lastMessageTime))
        {
            holder.tvLastMessageTime.setText(Connection.getTime(Long.parseLong(lastMessageTime)));
        }

        if(!chatModel.getUnreadCount().equals("0"))
        {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(chatModel.getUnreadCount());
        }
        else
        {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        holder.linearChatList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChattingActivity.class);
                intent.putExtra(Extras.user_key, chatModel.getUserId());
                intent.putExtra(Extras.user_name, chatModel.getUserName());
                intent.putExtra(Extras.user_picture, chatModel.getUserPicture());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatModelList.size();
    }

    public class ChatListViewHolder extends RecyclerView.ViewHolder{

        private LinearLayout linearChatList;
        private TextView userName, tvLastMessage, tvLastMessageTime, tvUnreadCount;
        private ImageView ivUserProfile;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);

            linearChatList = itemView.findViewById(R.id.linearChatList);
            userName = itemView.findViewById(R.id.tvUserNameChatList);
            ivUserProfile = itemView.findViewById(R.id.ivProfileChatList);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMesageTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}
