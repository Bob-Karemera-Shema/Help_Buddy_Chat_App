package com.example.help_buddy_chat_app.Requests;

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

import com.example.help_buddy_chat_app.Common.Constants;
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

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private RecyclerView rvRequests;
    private RequestAdapter requestAdapter;
    private List<Request> requestList;

    //Declare UI view objects
    private TextView tvNoRequests;      //Text view displayed if there are no friend requests
    private View progressBar;           //progressBar displayed while fetching requests

    //Declare database reference and current user objects
    private DatabaseReference databaseReferenceUsers, databaseReferenceRequests;
    private FirebaseUser currentUser;

    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //initialise view objects
        rvRequests = view.findViewById(R.id.rvRequests);
        tvNoRequests = view.findViewById(R.id.tvNoRequests);
        progressBar = view.findViewById(R.id.progressBarRequests);

        rvRequests.setLayoutManager(new LinearLayoutManager(getActivity()));
        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(getActivity(), requestList);
        rvRequests.setAdapter(requestAdapter);

        //Get user info from database
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        //initialise database references
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.users);
        databaseReferenceRequests = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.friendRequests).child(currentUser.getUid());

        tvNoRequests.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        //Get requests from database using requests reference
        databaseReferenceRequests.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                //clear existing records
                requestList.clear();

                //iterate through every database record in friend_requests node
                for(DataSnapshot dataSnapshot : snapshot.getChildren())
                {
                    if (dataSnapshot.exists())
                    {
                        String requestType = dataSnapshot.child(NodeNames.requestType)
                                .getValue().toString();

                        //show requestTypes with "received" value only
                        if (requestType.equals(Constants.request_received))
                        {
                            String userId = dataSnapshot.getKey();

                            //access information about the user sending friend request
                            databaseReferenceUsers.child(userId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    //fetch user info
                                    String userName = snapshot.child(NodeNames.name)
                                            .getValue()!=null?
                                            snapshot.child(NodeNames.name)
                                                    .getValue().toString():"Unknown User";
                                    String userPicture = "";

                                    if(!snapshot.child(NodeNames.picture).getValue().toString().equals(""))
                                    {
                                        userPicture = snapshot.child(NodeNames.picture)
                                                .getValue().toString();
                                    }

                                    //create request object and add it to lists of requests to be shown
                                        Request request = new Request(userId,
                                                userName, userPicture);
                                        requestList.add(request);
                                        //notify adapter of change in list of requests
                                        requestAdapter.notifyDataSetChanged();

                                        tvNoRequests.setVisibility(View.GONE);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(getActivity(),
                                            getActivity().getString(R.string.failed_to_get_requests,
                                                    error.getMessage()),
                                            Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(),
                        getActivity().getString(R.string.failed_to_get_requests,
                                error.getMessage()),
                        Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}