package com.example.help_buddy_chat_app.SignUp;

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
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.help_buddy_chat_app.Common.NodeNames;
import com.example.help_buddy_chat_app.Login.LoginActivity;
import com.example.help_buddy_chat_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    //Declare UI component objects
    private TextInputEditText etName, etEmailSignUp, etPasswordSignUp, etConfirmPassword;
    private String name, email, password, confirmPassword;
    private Button signUp;
    private ImageView userProfile;
    private View progressBar;   //progress bar to be displayed when program is loading

    //Firebase database user object
    private FirebaseUser currentUser;
    //Firebase database object
    private DatabaseReference databaseReference;

    //Firebase file storage object
    private StorageReference fileStorage;
    //Declare user profile image of choice location and where it will be stored on the server
    private Uri localImageUri, serverImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        //Initialise File storage object with reference to database
        fileStorage = FirebaseStorage.getInstance().getReference();

        //Assign UI component objects their values
        etName = findViewById(R.id.etNameReset);
        etEmailSignUp = findViewById(R.id.etEmailReset);
        etPasswordSignUp = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        signUp = findViewById(R.id.btnChangePassword);
        userProfile = findViewById(R.id.ivAppLogo);
        progressBar = findViewById(R.id.progressBarRequests);
    }

    @Override
    public void onClick(View view) {
        //method to handle click events

        if(view.getId() == signUp.getId())
        {
            getInputs();
            checkValidInput();
        }
        if(view.getId() == userProfile.getId())
        {
            chooseImage();
        }
    }

    private void getInputs()
    {
        name = etName.getText().toString().trim();
        email = etEmailSignUp.getText().toString().trim();
        password = etPasswordSignUp.getText().toString().trim();
        confirmPassword = etConfirmPassword.getText().toString().trim();
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

    private void addUserWithProfilePicture()
    {
        //method to add user with profile picture
        String filename = currentUser.getUid() + ".jpg";

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
                                    .setDisplayName(etName.getText().toString().trim())
                                    .setPhotoUri(serverImageUri)
                                    .build();
                            currentUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if(task.isSuccessful())
                                    {
                                        //get current user Id to update correct user
                                        String userID = currentUser.getUid();
                                        //assign database reference a reference to the user in the database
                                        databaseReference = FirebaseDatabase.getInstance().getReference()
                                                .child(NodeNames.users);

                                        //Upload data to database all in one go using HashMap
                                        HashMap<String,String> hashMap = new HashMap<>();
                                        hashMap.put(NodeNames.name, etName.getText().toString().trim());
                                        hashMap.put(NodeNames.email, etEmailSignUp.getText().toString().trim());
                                        hashMap.put(NodeNames.online, "true");
                                        hashMap.put(NodeNames.picture, serverImageUri.getPath());

                                        //Upload to database using HashMap object
                                        databaseReference.child(userID).setValue(hashMap)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        Toast.makeText(SignUpActivity.this, R.string.user_created,
                                                                Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(SignUpActivity.this,
                                                                LoginActivity.class));
                                                    }
                                                });
                                    }
                                    else
                                    {
                                        Toast.makeText(SignUpActivity.this,
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

    private void addUser()
    {
        //method to add user without profile picture
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etName.getText().toString().trim()).build();

        progressBar.setVisibility(View.VISIBLE);
        currentUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if(task.isSuccessful())
                {
                    //get current user Id to update correct user
                    String userID = currentUser.getUid();
                    //assign database reference a reference to the user in the database
                    databaseReference = FirebaseDatabase.getInstance().getReference()
                            .child(NodeNames.users);

                    //Upload data to database all in one go using HashMap
                    HashMap<String,String> hashMap = new HashMap<>();
                    hashMap.put(NodeNames.name, etName.getText().toString().trim());
                    hashMap.put(NodeNames.email, etEmailSignUp.getText().toString().trim());
                    hashMap.put(NodeNames.online, "true");
                    hashMap.put(NodeNames.picture, "");

                    //Upload to database using HashMap object
                    databaseReference.child(userID).setValue(hashMap)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(SignUpActivity.this, R.string.user_created,
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this,
                                    LoginActivity.class));
                        }
                    });
                }
                else
                {
                    Toast.makeText(SignUpActivity.this,
                            getString(R.string.failed_update_profile, task.getException()),
                            Toast.LENGTH_SHORT);
                }
            }
        });
    }

    private void checkValidInput()
    {
        if (name.equals(""))
        {
            etName.setError(getString(R.string.enter_name));
        }
        if (email.equals(""))
        {
            etEmailSignUp.setError(getString(R.string.enter_email));
        }
        else if (password.equals(""))
        {
            etPasswordSignUp.setError(getString(R.string.enter_password));
        }
        else if (confirmPassword.equals(""))
        {
            etConfirmPassword.setError(getString(R.string.confirm_password));
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches())
        {
            //Notify the user if email given is not valid
            //Depending on standard email address format
            etEmailSignUp.setError(getString(R.string.enter_correct_email));
        }
        else if(!password.equals(confirmPassword))
        {
            //Notify the user if passwords don't match
            etConfirmPassword.setError(getString(R.string.password_mismatch));
        }
        else
        {
            //if all inputs are okay
            //create user account using firebase
            //user information is stored using database provided by firebase

            final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

            progressBar.setVisibility(View.VISIBLE);
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    progressBar.setVisibility(View.GONE);
                    if(task.isSuccessful())
                    {
                        //if user is created successfully, redirect user to login page
                        currentUser = firebaseAuth.getCurrentUser();

                        //check whether user chose profile picture or not
                        if(localImageUri != null)
                            addUserWithProfilePicture();
                        else
                            addUser();
                    }
                    else
                    {
                        //notify user if account is not created
                        Toast.makeText(SignUpActivity.this,
                                getString(R.string.user_not_created, task.getException()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}