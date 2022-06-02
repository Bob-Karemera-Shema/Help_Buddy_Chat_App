package com.example.help_buddy_chat_app.ChatList;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    //Declare UI view objects
    private RecyclerView rvChats;
    private View loadingChats;
    private TextView tvNoChats;
    private ChatListAdapter chatListAdapter;
    private List<ChatModel> chatModelList;

    //Declare database references
    private DatabaseReference databaseReferenceChats, databaseReferenceUsers;
    private FirebaseUser currentUser;

    private ChildEventListener childEventListener;
    private Query query;

    private List<String> userIds;

    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChats = view.findViewById(R.id.rvChat);
        tvNoChats = view.findViewById(R.id.tvNoConvo);
        loadingChats = view.findViewById(R.id.progressBarChat);

        chatModelList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getActivity(),chatModelList);
        userIds = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        //To allow messages to be displayed from the latest
        //chats will be displayed in reverse order allowing the latest to come first
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        rvChats.setLayoutManager(linearLayoutManager);
        rvChats.setAdapter(chatListAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        //initialise database references to users and chats nodes
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.users);
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.chats).child(currentUser.getUid());

        loadingChats.setVisibility(View.VISIBLE);
        tvNoChats.setVisibility(View.VISIBLE);

        //Query database to fetch chat records from database
        query = databaseReferenceChats.orderByChild(NodeNames.time_stamp);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateChatList(snapshot,true, snapshot.getKey());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateChatList(snapshot,false, snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        query.addChildEventListener(childEventListener);

        loadingChats.setVisibility(View.GONE);
        tvNoChats.setVisibility(View.VISIBLE);
    }

    private void updateChatList(DataSnapshot dataSnapshot, boolean isNew, String userId)
    {
        loadingChats.setVisibility(View.GONE);
        tvNoChats.setVisibility(View.GONE);

        final String lastMessage, lastMessageTime, unreadCount;

        if(dataSnapshot.child(NodeNames.last_message).getValue() != null)
        {
            lastMessage = dataSnapshot.child(NodeNames.last_message).getValue().toString();
        }
        else
        {
            lastMessage = "";
        }

        if(dataSnapshot.child(NodeNames.last_message_time).getValue() != null)
        {
            lastMessageTime = dataSnapshot.child(NodeNames.last_message_time).getValue().toString();
        }
        else
        {
            lastMessageTime = "";
        }

        unreadCount = dataSnapshot.child(NodeNames.unread_count).getValue() == null?
                "0":dataSnapshot.child(NodeNames.unread_count).getValue().toString();

        databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check whether userName is not null
                //if blank give it a blank value;
                String userName = snapshot.child(NodeNames.name).getValue()!=null?
                        snapshot.child(NodeNames.name).getValue().toString():"Unknown User";

                /*
                String userPicture = snapshot.child(NodeNames.picture).getValue() != null ?
                        snapshot.child(NodeNames.picture).getValue().toString() : "";*/

                String userPicture = userId + ".jpg";

                ChatModel chatModel = new ChatModel(userId, userName, userPicture, unreadCount,
                        lastMessage, lastMessageTime);

                if(isNew)
                {
                    //add new chat to chat list
                    chatModelList.add(chatModel);
                    userIds.add(userId);
                }
                else
                {
                    int indexOfClickedChat = userIds.indexOf(userId);
                    chatModelList.set(indexOfClickedChat, chatModel);
                }

                chatListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), getActivity().getString(R.string.failed_to_get_chats,
                        error.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        query.removeEventListener(childEventListener);
    }
}