package com.example.help_buddy_chat_app.People;

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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PeopleFragment extends Fragment {

    //Declare UI components
    private RecyclerView rvPeople;
    private PeopleAdapter peopleAdapter;
    private List<People> peopleList;
    private TextView tvNoPeople;

    //Declare database reference and user objects
    private DatabaseReference databaseReferenceUsers, databaseReferenceRequests;
    private FirebaseUser currentUser;
    private View loadingPeopleProgress;

    public PeopleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_people, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //initialise UI components
        rvPeople = view.findViewById(R.id.rvPeople);
        loadingPeopleProgress = view.findViewById(R.id.loadingPeople);
        tvNoPeople = view.findViewById(R.id.tvNoPeople);

        rvPeople.setLayoutManager(new LinearLayoutManager(getActivity()));

        peopleList = new ArrayList<>();
        peopleAdapter = new PeopleAdapter(getActivity(), peopleList);
        rvPeople.setAdapter(peopleAdapter);

        //get reference to database node "Users"
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.users);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //get reference to database node "friend_requests"
        databaseReferenceRequests = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.friendRequests).child(currentUser.getUid());

        tvNoPeople.setVisibility(View.VISIBLE);
        loadingPeopleProgress.setVisibility(View.VISIBLE);

        //access data from database
        //Create Query object to retrieve data from database using a query in alphabetical order of names
        Query query = databaseReferenceUsers.orderByChild(NodeNames.name);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear existing people list
                peopleList.clear();
                //iterate through each user and add each user to people list to be shown
                for (DataSnapshot dataSnapshot : snapshot.getChildren())
                {
                    String userId = dataSnapshot.getKey();

                    if(userId.equals(currentUser.getUid())) {
                        //to avoid loading current user info and displaying it on people screen
                        //can't send a friend request to yourself
                        continue;
                    }


                    if (dataSnapshot.child(NodeNames.name).getValue() != null)
                    {
                            String userName = dataSnapshot.child(NodeNames.name).getValue()
                                    .toString();
                            String userPicture = dataSnapshot.child(NodeNames.picture).getValue()
                                    .toString();

                            databaseReferenceRequests.child(userId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    //check whether current user already sent a friend request to this user
                                    //if yes, personAdded is true
                                    //else personAdded is false
                                    if (snapshot.exists())
                                    {
                                        String requestType = snapshot.child(NodeNames.requestType)
                                                .getValue().toString();
                                        if (requestType.equals(Constants.request_sent))
                                        {
                                            //Add user to people list without add friend button
                                            //Because current user has already sent a friend request to the user
                                            peopleList.add(new People(userName, userPicture,
                                                    userId, true));
                                            //notify people adapter of new record fetched from database
                                            peopleAdapter.notifyDataSetChanged();
                                        }
                                    }
                                    else
                                    {
                                        //Add user to people list with add friend button visible
                                        peopleList.add(new People(userName, userPicture,
                                                userId, false));
                                        peopleAdapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(getContext(),
                                            getString(R.string.get_people_failed, error.getMessage()),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });

                            tvNoPeople.setVisibility(View.GONE);
                            loadingPeopleProgress.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingPeopleProgress.setVisibility(View.GONE);
                Toast.makeText(getContext(),
                        getString(R.string.get_people_failed, error.getMessage()),
                        Toast.LENGTH_SHORT).show();
                tvNoPeople.setVisibility(View.VISIBLE);
            }
        });
        tvNoPeople.setVisibility(View.VISIBLE);
        loadingPeopleProgress.setVisibility(View.GONE);
    }
}