package com.example.help_buddy_chat_app.PasswordRelated;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.help_buddy_chat_app.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword, etConfirmPassword;
    private String newPassword, confirmPassword;
    private View progressBar;   //progress bar to be displayed when program is loading

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        progressBar = findViewById(R.id.progressBarRequests);
    }

    public void changePassClick(View view)
    {
        //method to change user password
        newPassword = etNewPassword.getText().toString().trim();
        confirmPassword = etConfirmPassword.getText().toString().trim();

        if(newPassword.equals(""))
        {
            etNewPassword.setError(getString(R.string.enter_password));
        }
        else if(confirmPassword.equals(""))
        {
            etConfirmPassword.setError(getString(R.string.confirm_password));
        }
        else if(!newPassword.equals(confirmPassword))
        {
            etConfirmPassword.setError(getString(R.string.password_mismatch));
        }
        else
        {
            updatePass();
        }
    }

    private void updatePass()
    {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if(firebaseUser != null)
        {
            firebaseUser.updatePassword(newPassword)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            progressBar.setVisibility(View.GONE);
                            if(task.isSuccessful())
                            {
                                Toast.makeText(ChangePasswordActivity.this,
                                        R.string.password_changed,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            else
                            {
                                Toast.makeText(ChangePasswordActivity.this,
                                        getString(R.string.password_not_changed, task.getException()),
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });
        }
    }
}