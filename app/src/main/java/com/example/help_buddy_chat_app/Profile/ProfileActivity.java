package com.example.help_buddy_chat_app.Profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.help_buddy_chat_app.PasswordRelated.ChangePasswordActivity;
import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.Login.LoginActivity;
import com.example.help_buddy_chat_app.R;
import com.example.help_buddy_chat_app.SignUp.SignUpActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
            serverImageUri = firebaseUser.getPhotoUrl();

            if(serverImageUri != null)
            {
                Glide.with(this).load(serverImageUri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(userProfile);
            }
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
                    HashMap<String, String> hashMap = new HashMap<>();
                    if(serverImageUri == null)
                    {
                        hashMap.put(NodeNames.name, etNameReset.getText().toString().trim());
                        hashMap.put(NodeNames.email, etEmailReset.getText().toString().trim());
                        hashMap.put(NodeNames.online, "true");
                        hashMap.put(NodeNames.picture, "");
                    }
                    else
                    {
                        hashMap.put(NodeNames.name, etNameReset.getText().toString().trim());
                        hashMap.put(NodeNames.email, etEmailReset.getText().toString().trim());
                        hashMap.put(NodeNames.online, "true");
                        hashMap.put(NodeNames.picture, serverImageUri.getPath());
                    }
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

    private void updateUserWithProfilePicture()
    {
        //method to add user with profile picture
        String filename = firebaseUser.getUid() + ".jpg";

        //path to image storage on firebase
        final StorageReference fileLocation = fileStorage.child("images/" + filename);

        //add the chosen image on Firebase
        fileLocation.putFile(localImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful())
                {
                    fileLocation.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            serverImageUri = uri;

                            //Add user with chosen profile picture
                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(etNameReset.getText().toString().trim())
                                    .setPhotoUri(serverImageUri)
                                    .build();
                            firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if(task.isSuccessful())
                                    {
                                        //get current user Id to update correct user
                                        String userID = firebaseUser.getUid();
                                        //assign database reference a reference to the user in the database
                                        databaseReference = FirebaseDatabase.getInstance().getReference()
                                                .child(NodeNames.users);

                                        //Upload data to database all in one go using HashMap
                                        HashMap<String,String> hashMap = new HashMap<>();
                                        hashMap.put(NodeNames.name, etNameReset.getText().toString().trim());
                                        hashMap.put(NodeNames.email, etEmailReset.getText().toString().trim());
                                        hashMap.put(NodeNames.online, "true");
                                        hashMap.put(NodeNames.picture, serverImageUri.getPath());

                                        //Upload to database using HashMap object
                                        databaseReference.child(userID).setValue(hashMap)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
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
                    });
                }
            }
        });
    }

    private void removeProfilePicture()
    {
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etNameReset.getText().toString().trim())
                .setPhotoUri(null)
                .build();

        firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if(task.isSuccessful())
                {
                    //get current user Id to update correct user
                    String userID = firebaseUser.getUid();
                    //assign database reference a reference to the user in the database
                    databaseReference = FirebaseDatabase.getInstance().getReference()
                            .child(NodeNames.users);

                    //Upload data to database all in one go using HashMap
                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.picture, "");

                    //Upload to database using HashMap object
                    databaseReference.child(userID).setValue(hashMap)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(ProfileActivity.this, R.string.profile_removed,
                                            Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(ProfileActivity.this,
                                            LoginActivity.class));
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

    private void chooseImage()
    {
        //as any android app an application has to be granted permission to access storage
        //to make sure it's only accessed if user provided permission
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
        {
            //Create an intent object and give it access to image storage location on user device
            Intent chooseImage = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(chooseImage, 101);
        }

        else{
            //Request storage access permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 101)
        {
            if(resultCode == RESULT_OK)
            {
                //obtain user profile image location
                localImageUri = data.getData();
                //Display image chosen on user profile image view
                userProfile.setImageURI(localImageUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 102)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                //Permission granted
                Intent chooseImage = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(chooseImage, 101);
            }
            else{
                //notify user about permission
                Toast.makeText(this, R.string.storage_permission,
                        Toast.LENGTH_SHORT).show();
            }
        }
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
        if(view.getId() == userProfile.getId())
        {
            if(serverImageUri == null)
            {
                chooseImage();
            }
            else
            {
                PopupMenu popupMenu = new PopupMenu(this, view);
                popupMenu.getMenuInflater()
                        .inflate(R.menu.menu_profile_picture, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int id = menuItem.getItemId();

                        if(id == R.id.menu_change_profile)
                        {
                            chooseImage();
                        }
                        if(id == R.id.menu_remove_profile)
                        {
                            removeProfilePicture();
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
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
            if(localImageUri != null)
            {
                updateUserWithProfilePicture();
            }
            else
            {
                updateUser();
            }
        }
    }

    private void logOut()
    {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
        DatabaseReference tokenReference = rootReference.child(NodeNames.tokens).child(currentUser.getUid());

        tokenReference.setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                    firebaseAuth.signOut();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                    finish();   //Ensures that even if user clicks back button on their screen
                    //they won't be taken back in app
                }
                else
                {
                    Toast.makeText(ProfileActivity.this,
                            getString(R.string.cant_log_out, task.getException()),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}