package com.example.help_buddy_chat_app.Requests;

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
import com.google.firebase.database.ServerValue;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private Context context;
    private List<Request> requestList;

    //Database references
    private DatabaseReference databaseReferenceRequests, databaseReferenceChats;
    private FirebaseUser currentUser;

    public RequestAdapter(Context context, List<Request> requestList) {
        this.context = context;
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.requests_layout,
                parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RequestViewHolder holder, int position) {

        //Get a record from requests list
        Request request = requestList.get(position);

        //Display user name
        holder.tvUserName.setText(request.getUserName());

        databaseReferenceRequests = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.friendRequests);
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.chats);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //Click listener for accept button
        holder.btnAcceptRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.btnAcceptRequest.setVisibility(View.GONE);
                holder.btnRemoveRequest.setVisibility(View.GONE);

                final String userId = request.getUserId();

                databaseReferenceChats.child(currentUser.getUid()).child(userId)
                        .child(NodeNames.time_stamp).setValue(ServerValue.TIMESTAMP)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful())
                                {
                                    databaseReferenceChats.child(userId).child(currentUser.getUid())
                                            .child(NodeNames.time_stamp)
                                            .setValue(ServerValue.TIMESTAMP)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful())
                                                    {
                                                        databaseReferenceRequests.child(currentUser.getUid())
                                                                .child(userId)
                                                                .child(NodeNames.requestType)
                                                                .setValue(Constants.request_accepted)
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if(task.isSuccessful())
                                                                        {
                                                                            databaseReferenceRequests.child(userId)
                                                                                    .child(currentUser.getUid())
                                                                                    .child(NodeNames.requestType)
                                                                                    .setValue(Constants.request_accepted)
                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                            if (task.isSuccessful())
                                                                                            {
                                                                                                holder.btnAcceptRequest.setVisibility(View.VISIBLE);
                                                                                                holder.btnRemoveRequest.setVisibility(View.VISIBLE);
                                                                                            }
                                                                                            else
                                                                                            {
                                                                                                errorHandling(holder, task.getException());
                                                                                            }
                                                                                        }
                                                                                    });
                                                                        }
                                                                        else
                                                                        {
                                                                            errorHandling(holder, task.getException());
                                                                        }
                                                                    }
                                                                });


                                                    }
                                                    else
                                                    {
                                                        errorHandling(holder, task.getException());
                                                    }
                                                }
                                            });


                                }
                                else
                                {
                                    errorHandling(holder, task.getException());
                                }
                            }
                        });


            }
        });

        //Click listener for remove button
        holder.btnRemoveRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.btnAcceptRequest.setVisibility(View.GONE);
                holder.btnRemoveRequest.setVisibility(View.GONE);

                final String userId = request.getUserId();

                //Delete request record on database
                databaseReferenceRequests.child(currentUser.getUid()).child(userId)
                        .child(NodeNames.requestType).setValue(null)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful())
                        {
                            databaseReferenceRequests.child(userId).child(currentUser.getUid())
                                    .child(NodeNames.requestType).setValue(null)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful())
                                    {
                                        Toast.makeText(context,
                                            R.string.friend_request_removed,
                                            Toast.LENGTH_SHORT).show();
                                        holder.btnAcceptRequest.setVisibility(View.VISIBLE);
                                        holder.btnRemoveRequest.setVisibility(View.VISIBLE);
                                    }
                                    else
                                    {
                                        Toast.makeText(context,
                                                context.getString(R.string.failed_to_remove_request,
                                                        task.getException()),
                                                Toast.LENGTH_SHORT).show();
                                        holder.btnAcceptRequest.setVisibility(View.VISIBLE);
                                        holder.btnRemoveRequest.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                        }
                        else
                        {
                            Toast.makeText(context,
                                    context.getString(R.string.failed_to_remove_request,
                                    task.getException()),
                                    Toast.LENGTH_SHORT).show();
                            holder.btnAcceptRequest.setVisibility(View.VISIBLE);
                            holder.btnRemoveRequest.setVisibility(View.VISIBLE);
                        }
                    }
                });


            }
        });
    }

    private void errorHandling(RequestViewHolder holder, Exception exception) {
        Toast.makeText(context,
                context.getString(R.string.failed_to_accept,
                        exception),
                Toast.LENGTH_SHORT).show();
        holder.btnAcceptRequest.setVisibility(View.VISIBLE);
        holder.btnRemoveRequest.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public class RequestViewHolder extends RecyclerView.ViewHolder{

        //Declare UI elements in request layout xml file which will display a user request
        private TextView tvUserName;
        private Button btnAcceptRequest, btnRemoveRequest;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            //initialise views
            tvUserName = itemView.findViewById(R.id.tvUserNameRequest);
            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
            btnRemoveRequest = itemView.findViewById(R.id.btnRemoveRequest);
        }
    }
}
