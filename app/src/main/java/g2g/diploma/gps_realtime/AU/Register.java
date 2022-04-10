package g2g.diploma.gps_realtime.AU;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import g2g.diploma.gps_realtime.R;
import g2g.diploma.gps_realtime.UserUsage.User;

public class Register extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "TAG";

    EditText mFullName,mEmail,mPassword,mPhone;
    Button mRegisterBtn;
    TextView mLoginTxt;
    CheckBox checkBox;

    ProgressBar progressBar;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    String user_id;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        mFullName = findViewById(R.id.fullName);
        mEmail = findViewById(R.id.Email);
        mPassword = findViewById(R.id.Password);
        mPhone = findViewById(R.id.nPhone);





        mRegisterBtn = findViewById(R.id.registerButton);
        mRegisterBtn.setOnClickListener(this);

        mLoginTxt = findViewById(R.id.loginAcc);
        mLoginTxt.setOnClickListener(this);


        progressBar = findViewById(R.id.progressBar2);


        //checking for login if you have logged in automatically to get you inside of the application
        /*if (fAuth.getCurrentUser() != null){
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
            finish();
        }
        */

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.loginAcc:
                startActivity(new Intent(getApplicationContext(), Login.class));
                finish();
                break;
            case R.id.registerButton:
                registerUser();
                break;

        }
    }


    private void registerUser() {

        String email = mEmail.getText().toString().trim();
        String password = mPassword.getText().toString().trim();
        String fName = mFullName.getText().toString().trim();
        String nPhone = mPhone.getText().toString().trim();


        checkBox = findViewById(R.id.checkIfAdmin);
        String fullAccessAD  = "";
        boolean isChecked = checkBox.isChecked();



        if (TextUtils.isEmpty(fName)){
            mFullName.setError("Full Name is Required");
            mFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)){
            mEmail.setError("Email Is Required");
            mEmail.requestFocus();
            return;
        }

        //check weather or not the email is valid and if it has (@,.,.com)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            mEmail.setError("Please Provide Us With A Valid Email!");
            mEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)){
            mPassword.setError("Password Is Required!");
            mPassword.requestFocus();
            return;
        }

        if (password.length() < 6){
            mPassword.setError("Password Is Required To Be At Least 6 Characters!");
            mPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(nPhone)){
            mPhone.setError("Phone Is Required For Security Purposes!");
            mPhone.requestFocus();
            return;
        }

        if (isChecked){
            fullAccessAD = "Granted";
        }
        else {
            fullAccessAD = "Not Granted";
        }


        progressBar.setVisibility(View.VISIBLE);

        String isFullAccess = fullAccessAD;
        fAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    User user = new User(fName,email,nPhone,user_id, isFullAccess);

                    FirebaseDatabase.getInstance().getReference("User")
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(Register.this, "User Has Been Registered Successfully!", Toast.LENGTH_SHORT).show();

                                //get the user id from FireStore Cloud
                                user_id = fAuth.getCurrentUser().getUid();


                                DocumentReference documentReference = fStore.collection("User").document(user_id);
                                Map<String,Object> user = new HashMap<>();
                                user.put("fName",fName);
                                user.put("user_id", user_id);
                                user.put("email",email);
                                user.put("phone",nPhone);
                                user.put("isFullAccess", isFullAccess);


                                documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG,"onSuccess: User Profile Is Created For "+ user_id);
                                    }
                                });

                                progressBar.setVisibility(View.INVISIBLE);
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                finish();

                            }else {
                                Toast.makeText(Register.this, "Failed To Register! Try Again! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    });

                }else {
                    Toast.makeText(Register.this, "Failed To Register! Try Again! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);

                }

            }
        });

    }
}