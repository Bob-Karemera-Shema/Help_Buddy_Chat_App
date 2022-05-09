package com.example.help_buddy_chat_app.People;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.help_buddy_chat_app.Common.Constants;
import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.AddPeopleViewHolder> {

    private Context context;
    private List<People> peopleList;

    //create reference to database
    private DatabaseReference databaseReferenceRequests;
    private FirebaseUser currentUser;
    private String userID;

    public PeopleAdapter(Context context, List<People> peopleList) {
        this.context = context;
        this.peopleList = peopleList;
    }

    @NonNull
    @Override
    public AddPeopleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.people_layout, parent,false);
        return new AddPeopleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddPeopleViewHolder holder, int position)
    {
        //method to fetch user info from firebase database and load to the people screen
        People people = peopleList.get(position);

        //load user information to UI
        holder.userName.setText(people.getUserName());

        //Get reference to friend_request node on Firebase database
        databaseReferenceRequests = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.friendRequests);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //check whether current user already sent a friend request to this user
        //if yes, hide the add friend button
        //else show add friend button
        if(people.isPersonAdded())
        {
            holder.btnAddPerson.setVisibility(View.GONE);
            holder.btnCancelRequest.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.btnAddPerson.setVisibility(View.VISIBLE);
            holder.btnCancelRequest.setVisibility(View.GONE);
        }

        //Add Friend Request Implementation
        holder.btnAddPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.btnAddPerson.setVisibility(View.GONE);
                //get ID of user clicked on
                userID = people.getUserID();

                /*
                    Send friend request
                Friend request is stored on the database as follows:
                    1. Under Current user's Id:
                        +Current user Id
                            +user receiving request Id
                                +request: sent

                    2. Under receiving user's Id:
                        + Receiving user Id
                            +Current user
                                +request: received
                */

                databaseReferenceRequests.child(currentUser.getUid()).child(userID)
                        .child(NodeNames.requestType).setValue(Constants.request_sent)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful())
                                {
                                    databaseReferenceRequests.child(userID)
                                            .child(currentUser.getUid())
                                            .child(NodeNames.requestType)
                                            .setValue(Constants.request_received)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful())
                                            {
                                                Toast.makeText(context, R.string.friend_request_sent_string,
                                                        Toast.LENGTH_SHORT).show();
                                                holder.btnAddPerson.setVisibility(View.GONE);
                                                holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                            }
                                            else
                                            {
                                                Toast.makeText(context,
                                                        context.getString(R.string.friend_request_not_sent,
                                                                task.getException()),
                                                        Toast.LENGTH_SHORT).show();
                                                holder.btnAddPerson.setVisibility(View.VISIBLE);
                                                holder.btnCancelRequest.setVisibility(View.GONE);
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    Toast.makeText(context,
                                            context.getString(R.string.friend_request_not_sent, task.getException()),
                                            Toast.LENGTH_SHORT).show();
                                    holder.btnAddPerson.setVisibility(View.VISIBLE);
                                    holder.btnCancelRequest.setVisibility(View.GONE);
                                }
                            }
                        });
            }
        });

        //Cancel Request button implementation
        holder.btnCancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.btnCancelRequest.setVisibility(View.GONE);
                //get ID of user clicked on
                userID = people.getUserID();

                //send friend request
                databaseReferenceRequests.child(currentUser.getUid()).child(userID)
                        .child(NodeNames.requestType).setValue(null)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful())
                                {
                                    databaseReferenceRequests.child(userID).child(currentUser.getUid())
                                            .child(NodeNames.requestType)
                                            .setValue(null)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful())
                                                    {
                                                        Toast.makeText(context, R.string.friend_request_cancelled,
                                                                Toast.LENGTH_SHORT).show();
                                                        holder.btnAddPerson.setVisibility(View.VISIBLE);
                                                        holder.btnCancelRequest.setVisibility(View.GONE);
                                                    }
                                                    else
                                                    {
                                                        Toast.makeText(context,
                                                                context.getString(R.string.friend_request_not_cancelled,
                                                                        task.getException()),
                                                                Toast.LENGTH_SHORT).show();
                                                        holder.btnAddPerson.setVisibility(View.GONE);
                                                        holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            });
                                }
                                else
                                {
                                    Toast.makeText(context,
                                            context.getString(R.string.friend_request_not_cancelled,
                                                    task.getException()),
                                            Toast.LENGTH_SHORT).show();
                                    holder.btnAddPerson.setVisibility(View.GONE);
                                    holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                }
                            }
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return peopleList.size();
    }

    public class AddPeopleViewHolder extends RecyclerView.ViewHolder{

        //Declare UI components objects in people layout file
        private TextView userName;
        private Button btnAddPerson, btnCancelRequest;

        public AddPeopleViewHolder(@NonNull View itemView) {
            super(itemView);

            //initialise UI component objects
            userName = itemView.findViewById(R.id.userNamePeople);
            btnAddPerson = itemView.findViewById(R.id.btnAddPerson);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
        }
    }
}
