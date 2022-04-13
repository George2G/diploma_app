package g2g.diploma.gps_realtime.AU;

import static g2g.diploma.gps_realtime.Constants.ERROR_DIALOG_REQUEST;
import static g2g.diploma.gps_realtime.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import g2g.diploma.gps_realtime.R;

public class Login extends AppCompatActivity implements  View.OnClickListener{

    private EditText mEmail,mPassword;
    private Button mLoginBtn;
    private TextView mCreateText, forgotTextLink;
    private ProgressBar progressBar;
    private FirebaseAuth fAuth;
    private FirebaseFirestore mDb;

    private static final String TAG = "Login";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mDb = FirebaseFirestore.getInstance();

        mEmail = findViewById(R.id.Email);
        mPassword = findViewById(R.id.Password);
        progressBar = findViewById(R.id.progressBar);
        fAuth = FirebaseAuth.getInstance();

        mLoginBtn = findViewById(R.id.loginButton);

        mCreateText = findViewById(R.id.loginAcc);
        mCreateText.setOnClickListener(this);

        forgotTextLink = findViewById(R.id.forgotPassword);
        forgotTextLink.setOnClickListener(this);



        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)){
                    mEmail.setError("Email is Required");
                    return;
                }
                if (TextUtils.isEmpty(password)){
                    mPassword.setError("Password is Required");
                    return;
                }
                if (password.length() < 6){
                    mPassword.setError("Password is Required to be At least 6 characters");
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);


                fAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){

                            DocumentReference isNewOrNot = mDb.collection("User Location").
                                    document(FirebaseAuth.getInstance().getUid());

                            isNewOrNot.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if (documentSnapshot.get("timestamp") != null){

                                    }
                                }
                            });

                            Toast.makeText(Login.this, "Logged in Successfully", Toast.LENGTH_SHORT).show();

                            // get the user to the main activity
                            startActivity(new Intent(getApplicationContext(),  MainActivity.class));
                            finish();
                            progressBar.setVisibility(View.GONE);

                        }else {
                            Toast.makeText(Login.this, "Error ! " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        forgotTextLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //editText filed so the user can enter his email to reset this password
                EditText resetMail = new EditText(v.getContext());

                AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(v.getContext());
                //tell the user what he needs to do in order to reset his password
                passwordResetDialog.setTitle("Reset Password");
                passwordResetDialog.setMessage("Enter Your Email To Receive Reset Link");
                passwordResetDialog.setView(resetMail);

                passwordResetDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //extract email and send link
                        String mail = resetMail.getText().toString();
                        fAuth.sendPasswordResetEmail(mail).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(Login.this, "Reset Link Sent To Your Email", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(Login.this, "Error ! Reset Link Is Not Sent"+ e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                });
                passwordResetDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //close the dialog and go back
                    }
                });
                passwordResetDialog.create().show();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        checkMapServices();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.loginAcc:
                startActivity(new Intent(getApplicationContext(), Register.class));
                finish();
                break;

        }
    }

    private void checkMapServices(){
        isServicesOK();
        isMapsEnabled();

    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if (!manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;

    }
    private void buildAlertMessageNoGps() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS location to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final android.app.AlertDialog alert = builder.create();
        alert.show();

    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(Login.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occurred but we can resolve it
            Log.d(TAG, "isServicesOK: an error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(Login.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }


}