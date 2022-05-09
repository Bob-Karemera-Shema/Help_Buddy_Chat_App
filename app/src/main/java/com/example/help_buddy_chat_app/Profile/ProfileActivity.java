package com.example.help_buddy_chat_app.Profile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.help_buddy_chat_app.PasswordRelated.ChangePasswordActivity;
import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.Login.LoginActivity;
import com.example.help_buddy_chat_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {

    //Declare UI component objects
    private TextInputEditText etNameReset, etEmailReset;
    private Button update, logOut;
    private ImageView userProfile;
    private TextView changePassword;
    private View progressBar;   //progress bar to be displayed when program is loading

    //Firebase database user object
    private FirebaseUser firebaseUser;
    //Firebase database object
    private DatabaseReference databaseReference;

    //Firebase file storage object
    private StorageReference fileStorage;
    //Declare user profile image of choice location and where it will be stored on the server
    private Uri localImageUri, serverImageUri;

    //Declare FirebaseAuth object to load current user when updating user info
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //Initialise File storage object with reference to database
        fileStorage = FirebaseStorage.getInstance().getReference();
        //Initialise FirebaseAuth object
        firebaseAuth = FirebaseAuth.getInstance();
        //Get current user
        firebaseUser = firebaseAuth.getCurrentUser();

        //Assign UI component objects their values
        etNameReset = findViewById(R.id.etNameReset);
        etEmailReset = findViewById(R.id.etEmailReset);
        update = findViewById(R.id.btnUpdateInfo);
        logOut = findViewById(R.id.btnLogout);
        userProfile = findViewById(R.id.ivAppLogo);
        changePassword = findViewById(R.id.tvChangePassword);
        progressBar = findViewById(R.id.progressBarRequests);

        initialiseUIComponents();
    }

    private void initialiseUIComponents()
    {
        if(firebaseUser != null)
        {
            //if user is logged in, download show existing information from server
            etNameReset.setText(firebaseUser.getDisplayName());
            etEmailReset.setText((firebaseUser.getEmail()));
        }
    }

    private void updateUser()
    {
        //Update user information per user request
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etNameReset.getText().toString().trim()).build();

        progressBar.setVisibility(View.VISIBLE);
        firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if(task.isSuccessful())
                {
                    //get current user Id to update correct user
                    String userID = firebaseUser.getUid();
                    //assign databaseReference a reference to the user in the database
                    databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.users);

                    //Upload data to database all in one go using HashMap
                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.name, etNameReset.getText().toString().trim());
                    hashMap.put(NodeNames.email, etEmailReset.getText().toString().trim());
                    hashMap.put(NodeNames.online, "true");
                    hashMap.put(NodeNames.picture, null);

                    //Upload to database using HashMap object
                    databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //redirect user to main page
                            finish();
                        }
                    });
                }
                else
                {
                    Toast.makeText(ProfileActivity.this,
                            getString(R.string.failed_update_profile, task.getException()),
                            Toast.LENGTH_SHORT);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        //method to handle click events

        if(view.getId() == logOut.getId())
        {
            logOut();
        }
        if(view.getId() == update.getId())
        {
            updateInfo();
        }
        if(view.getId() == changePassword.getId())
        {
            startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
        }
    }

    private void updateInfo()
    {
        //method checks whether inputs are correct and updates user info
        if(etNameReset.getText().toString().trim().equals(""))
        {
            etNameReset.setError(getString(R.string.enter_name));
        }
        else if(etEmailReset.getText().toString().trim().equals(""))
        {
            etEmailReset.setError(getString(R.string.enter_email));
        }
        else
        {
            updateUser();
        }
    }

    private void logOut()
    {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signOut();
        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
        finish();   //Ensures that even if user clicks back button on their screen
                    //they won't be taken back in app
    }
}