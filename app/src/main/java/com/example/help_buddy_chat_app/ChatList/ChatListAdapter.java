package com.example.help_buddy_chat_app.ChatList;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.help_buddy_chat_app.Chatting.ChattingActivity;
import com.example.help_buddy_chat_app.Common.Extras;
import com.example.help_buddy_chat_app.R;

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

        holder.linearChatList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ChattingActivity.class);
                intent.putExtra(Extras.user_key, chatModel.getUserId());
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

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);

            linearChatList = itemView.findViewById(R.id.linearChatList);
            userName = itemView.findViewById(R.id.tvUserNameChatList);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMesageTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}
