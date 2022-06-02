package com.example.help_buddy_chat_app.Requests;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.help_buddy_chat_app.Common.Connection;
import com.example.help_buddy_chat_app.Common.Constants;
import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

        //Reference to firebase storage location to get user profile picture
        StorageReference fileLocation = FirebaseStorage.getInstance().getReference()
                .child(Constants.imageLocation + "/" + request.getUserPicture());

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
                                                                                                //Send friend request accepted notification to person who sent the request
                                                                                                String title = "Friend Request Accepted";
                                                                                                String message = "Friend request accepted by " + currentUser.getDisplayName();
                                                                                                Connection.sendNotification(context, title, message, userId);

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

                                        //Send friend request declined notification to person who sent the request
                                        String title = "Friend Request Denied";
                                        String message = "Friend request denied by " + currentUser.getDisplayName();
                                        Connection.sendNotification(context, title, message, userId);

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
        private ImageView ivUserProfile;
        private Button btnAcceptRequest, btnRemoveRequest;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            //initialise views
            tvUserName = itemView.findViewById(R.id.tvUserNameRequest);
            ivUserProfile = itemView.findViewById(R.id.ivProfileRequest);
            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
            btnRemoveRequest = itemView.findViewById(R.id.btnRemoveRequest);
        }
    }
}
