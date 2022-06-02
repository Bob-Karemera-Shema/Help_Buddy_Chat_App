package com.example.help_buddy_chat_app.SelectFriend;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.help_buddy_chat_app.Common.Extras;
import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SelectFriendActivity extends AppCompatActivity {

    private RecyclerView rvSelectFriend;
    private SelectFriendAdapter selectFriendAdapter;;
    private List<SelectFriendModel> selectFriendModels;
    private View progressBar;

    private DatabaseReference databaseReferenceUsers, databaseReferenceChats;
    private FirebaseUser currentUser;
    private ValueEventListener valueEventListener;

    private String selectedMessage, selectedMessageID, selectedMessageType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);

        if(getIntent().hasExtra(Extras.message))
        {
            selectedMessage = getIntent().getStringExtra(Extras.message);
            selectedMessageID = getIntent().getStringExtra(Extras.message_id);
            selectedMessageType = getIntent().getStringExtra(Extras.message_type);
        }

        rvSelectFriend = findViewById(R.id.rvSelectFriend);
        progressBar = findViewById(R.id.progressBar);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvSelectFriend.setLayoutManager(linearLayoutManager);

        selectFriendModels = new ArrayList<>();
        selectFriendAdapter = new SelectFriendAdapter(this, selectFriendModels);
        rvSelectFriend.setAdapter(selectFriendAdapter);

        progressBar.setVisibility(View.VISIBLE);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.chats).child(currentUser.getUid());
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.users);

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot dataSnapshot : snapshot.getChildren())
                {
                    String userId = dataSnapshot.getKey();
                    databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String userName = snapshot.child(NodeNames.name).getValue()!=null
                                    ?snapshot.child(NodeNames.name).getValue().toString():"";

                            SelectFriendModel friendModel = new SelectFriendModel(userId, userName,
                                    userId + ".jpg");
                            selectFriendModels.add(friendModel);
                            selectFriendAdapter.notifyDataSetChanged();

                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(SelectFriendActivity.this,
                                    getString(R.string.failed_to_fetch_friends, error.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SelectFriendActivity.this,
                        getString(R.string.failed_to_fetch_friends, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        };

        databaseReferenceChats.addValueEventListener(valueEventListener);
    }

    public void returnSelectedFriend(String userId, String userName, String userPicture)
    {
        //method to return selected friend to forward message

        databaseReferenceChats.removeEventListener(valueEventListener);
        Intent intent = new Intent();

        intent.putExtra(Extras.user_key, userId);
        intent.putExtra(Extras.user_name, userName);
        intent.putExtra(Extras.user_picture, userPicture);


        intent.putExtra(Extras.message, selectedMessage);
        intent.putExtra(Extras.message_id, selectedMessageID);
        intent.putExtra(Extras.message_type, selectedMessageType);

        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}