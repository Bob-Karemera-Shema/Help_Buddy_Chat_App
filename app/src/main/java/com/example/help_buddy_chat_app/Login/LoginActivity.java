package com.example.help_buddy_chat_app.Login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.help_buddy_chat_app.Common.Connection;
import com.example.help_buddy_chat_app.HomePage.HomePageActivity;
import com.example.help_buddy_chat_app.Common.NoConnectionActivity;
import com.example.help_buddy_chat_app.R;
import com.example.help_buddy_chat_app.PasswordRelated.ResetPasswordActivity;
import com.example.help_buddy_chat_app.SignUp.SignUpActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    //Declare UI components: textViews and buttons
    private TextInputEditText etEmail, etPassword;
    private TextView signUp;
    private String email, password;
    private Button login;
    private View progressBar;   //progress bar to be displayed when program is loading

    //Using a firebase object to load the app database
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmailReset);
        etPassword = findViewById(R.id.etNewPassword);
        login = findViewById(R.id.btnChangePassword);
        signUp = findViewById(R.id.txtSignUp);
        progressBar = findViewById(R.id.progressBarRequests);
    }

    @Override
    protected void onStart() {
        //method to check whether user is already logged in the app
        //if they have logged in before on their device, they won't be asked to log in again
        super.onStart();

        //get app database from Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        //get current user information from database
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if(firebaseUser != null)
        {
            //if user is already logged in, go to main page
            startActivity(new Intent(LoginActivity.this, HomePageActivity.class));
            finish();       //method to ensure back button clicks don't take user to login screen
        }
    }

    @Override
    public void onClick(View view)
    {
        //if user clicks login, check login information provided
        //if user clicks Sign Up, redirect user to Sign Up screen
        if(view.getId() == login.getId())
        {
            getInputs();
            checkValidInput();
        }
        if (view.getId() == signUp.getId())
        {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        }
    }

    private void getInputs() {
        email = etEmail.getText().toString().trim();
        password = etPassword.getText().toString().trim();
    }

    private void checkValidInput()
    {
        if (email.equals(""))
        {
            etEmail.setError(getString(R.string.enter_email));
        }
        else if (password.equals(""))
        {
            etPassword.setError(getString(R.string.enter_password));
        }
        else
        {
            //check for internet connection on device first
            if(Connection.connected(this)) {
                progressBar.setVisibility(View.VISIBLE);
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, R.string.log_in_success,
                                            Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(LoginActivity.this,
                                            HomePageActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Login Failed: " +
                                            task.getException(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
            else
            {
                startActivity(new Intent(LoginActivity.this, NoConnectionActivity.class));
            }
        }
    }

    public void resetPassClick(View view)
    {
        startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
    }
}