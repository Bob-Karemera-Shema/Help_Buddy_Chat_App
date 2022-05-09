package com.example.help_buddy_chat_app.Chatting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.help_buddy_chat_app.Common.Connection;
import com.example.help_buddy_chat_app.Common.Constants;
import com.example.help_buddy_chat_app.Common.Extras;
import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChattingActivity extends AppCompatActivity implements View.OnClickListener {

    //Declare UI views
    private ImageView ivSend;
    private EditText etMessage;
    private RecyclerView rvMessages;
    private SwipeRefreshLayout srlMessages;

    private MessageAdapter messageAdapter;
    private List<MessageModel> messageModelList;

    //On each screen a page of messages is displayed and when the user scrolls up for old messages
    //the currentPage number is incremented to load from messages
    private int currentPage =1;
    private static final  int messages_per_page = 30;

    //Declare Database reference to root
    private DatabaseReference mRootRef;
    private FirebaseAuth firebaseAuth;
    //Reference to messages node on database
    private DatabaseReference databaseReferenceMessages;

    //listener for new message records on database
    private ChildEventListener childEventListener;

    private String currentUserId, chatUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);

        //initialise UI views and database references
        ivSend = findViewById(R.id.ivSend);
        etMessage = findViewById(R.id.etMessage);

        ivSend.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = firebaseAuth.getCurrentUser().getUid();

        if(getIntent().hasExtra(Extras.user_key))
        {
            chatUserId = getIntent().getStringExtra(Extras.user_key);
        }

        rvMessages = findViewById(R.id.rvMessages);
        srlMessages = findViewById(R.id.srlMessages);

        messageModelList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageModelList);

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);

        //getMessages
        getMessages();
        //ensure screen displays the latest record
        rvMessages.scrollToPosition(messageModelList.size()-1);

        //set listener for swipe event when the user wants to load older messages
        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //increment message page and load more messages
                currentPage++;
                getMessages();
            }
        });
    }

    @Override
    public void onClick(View view) {

        switch (view.getId())
        {
            case R.id.ivSend:
                if(Connection.connected(this)) {
                    DatabaseReference userMessagePush = mRootRef.child(NodeNames.messages)
                            .child(currentUserId).child(chatUserId).push();
                    String pushId = userMessagePush.getKey();
                    sendMessage(etMessage.getText().toString().trim(), Constants.messageTypeText, pushId);
                }
                else
                {
                    Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void sendMessage(String message, String messageType, String pushId)
    {
        try {
            if(!message.equals(""))
            {
                //Declare HashMap to store message to database
                HashMap messageMap = new HashMap();
                messageMap.put(NodeNames.messageId, pushId);
                messageMap.put(NodeNames.message, message);
                messageMap.put(NodeNames.messageType, messageType);
                messageMap.put(NodeNames.messageFrom, currentUserId);
                messageMap.put(NodeNames.messageTime, ServerValue.TIMESTAMP);

                String currentUserRef = NodeNames.messages + "/" + currentUserId + "/" + chatUserId;
                String chatUserRef = NodeNames.messages + "/" + chatUserId + "/" + currentUserId;

                HashMap messageUserMap = new HashMap();
                messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                etMessage.setText("");

                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if(error != null)
                        {
                            Toast.makeText(ChattingActivity.this,
                                    getString(R.string.failed_to_message, error.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(ChattingActivity.this, R.string.message_sent,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
        catch(Exception exception)
        {
            Toast.makeText(ChattingActivity.this,
                    getString(R.string.failed_to_message, exception.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void getMessages()
    {
        //first clear message list to load a new list
        messageModelList.clear();
        //get reference to database messages node
        databaseReferenceMessages = mRootRef.child(NodeNames.messages)
                .child(currentUserId).child(chatUserId);

        /*query the database for message records and only limit them to the 30 records
          if the user swipes up 30 more records are retrieved
          that means everytime the current page will be incremented, it will be multiplied by the
          number of messages per page resulting in  the desired number of messages*/
        Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * messages_per_page);

        if(childEventListener != null)
            messageQuery.removeEventListener(childEventListener);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                /*For each message object received, all object attributes are initialised.
                Unlike in previous attempts in the home page fragments where a single value from
                the database is fetched and assigned to a string variable.*/

                MessageModel message = snapshot.getValue(MessageModel.class);

                messageModelList.add(message);
                //notify message adapter about new message record from database
                messageAdapter.notifyDataSetChanged();


                //ensure screen immediately displays the latest message as soon as it is received
                rvMessages.scrollToPosition(messageModelList.size()-1);
                //set swipe refresh to false until message list is completely loaded
                srlMessages.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                srlMessages.setRefreshing(false);
            }
        };
        messageQuery.addChildEventListener(childEventListener);
    }
}