package com.example.help_buddy_chat_app.PasswordRelated;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.help_buddy_chat_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    String email;
    private TextView tvInfo;
    private LinearLayout linearResetPassword, linearNotification;
    FirebaseAuth firebaseAuth;
    private Button btnTryAgain;
    private View progressBar;   //progress bar to be displayed when program is loading

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etEmail = findViewById(R.id.etEmailReset);
        tvInfo = findViewById(R.id.tvInfo);
        linearResetPassword = findViewById(R.id.linearResetPassword);
        linearNotification = findViewById(R.id.linearNotification);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        progressBar = findViewById(R.id.progressBarRequests);
    }

    public void resetPasswordClick(View view)
    {
        email = etEmail.getText().toString().trim();

        if(email.equals(""))
        {
            etEmail.setError(getString(R.string.enter_email));
        }
        else
        {
            firebaseAuth = FirebaseAuth.getInstance();

            progressBar.setVisibility(View.VISIBLE);
            //send reset password link to user email using firebase object and notify user
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    progressBar.setVisibility(View.GONE);
                    linearResetPassword.setVisibility(View.GONE);
                    linearNotification.setVisibility(View.VISIBLE);

                    if(task.isSuccessful())
                    {
                        tvInfo.setText(getString(R.string.reset_instructions, email));

                        //if user chooses to try reset password again
                        //a delay is implemented before trying again
                        //Delay is implemented using CountDownTimer class
                        new CountDownTimer(60000, 1000) {
                            @Override
                            public void onTick(long l) {
                                //notify user about remaining time before rest link is resent
                                btnTryAgain.setText(
                                        getString(R.string.try_again_delay,
                                                String.valueOf(l/1000)));
                                //Add OnClickListener to register every tick of time
                                btnTryAgain.setOnClickListener(null);
                            }

                            @Override
                            public void onFinish() {
                                tryAgain();
                            }
                        }.start();
                    }
                    else{
                        //notify user resetting password was unsuccessful and redirect the user
                        //back to reset password screen
                        tvInfo.setText(getString(R.string.reset_link_not_sent, task.getException()));
                        tryAgain();
                    }
                }
            });
        }
    }

    private void tryAgain()
    {
        //method to reload rest password screen
        btnTryAgain.setText(R.string.try_again);
        btnTryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //redirect user back to reset password screen
                linearResetPassword.setVisibility(View.VISIBLE);
                linearNotification.setVisibility(View.GONE);
            }
        });
    }

    public void closeClick(View view)
    {
        //method to close activity
        finish();
    }
}