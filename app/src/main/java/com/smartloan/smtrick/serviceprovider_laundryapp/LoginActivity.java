package com.smartloan.smtrick.serviceprovider_laundryapp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private ProgressBar progressBar;
    private EditText inputMobile, inputPassword;
    private Button btnLogin, btnReset;
    private ProgressDialogClass progressDialog;
    private AppSharedPreference appSharedPreference;
    private DatabaseReference mDatabase;
    private UserRepository userRepository;
    private LeedRepository leedRepository;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity_User.class));
            finish();
        }

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        // Get new Instance ID token
                        token = task.getResult().getToken();

                    }
                });
        // set the view now
        setContentView(R.layout.activity_login);
        leedRepository = new LeedRepositoryImpl();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        appSharedPreference = new AppSharedPreference(this);
        userRepository = new UserRepositoryImpl(this);

        checkLoginState();

        inputMobile = (EditText) findViewById(R.id.number);
        inputPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnReset = (Button) findViewById(R.id.btn_reset_password);

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });

        if (isNetworkAvailable()) {
//            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
        }

//
//        btnReset.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
//            }
//        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Login();
                String number = inputMobile.getText().toString().trim();
                final String password = inputPassword.getText().toString().trim();

                DatabaseReference Dref = FirebaseDatabase.getInstance().getReference("ServiceProviders");
                Dref.orderByChild("number").equalTo(number).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getValue() != null) {
                            for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                                if (dataSnapshot.hasChildren()) {
                                    User upload = postSnapshot.getValue(User.class);
                                    setLeedStatus(upload);
                                    String userid = upload.getUserid();

                                    if (inputMobile.getText().toString().equalsIgnoreCase(upload.getNumber()) &&
                                            inputPassword.getText().toString().equalsIgnoreCase(upload.getPassword())) {
                                        appSharedPreference.addUserDetails(upload);
                                        appSharedPreference.createUserLoginSession();
                                        Toast.makeText(LoginActivity.this, "Login Successfull", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, MainActivity_User.class);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();

                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "No User Found", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed Please Register", Toast.LENGTH_SHORT).show();
                        }
                    }

                    private void setLeedStatus(User upload) {

                        upload.setTokan(token);
                        updateLeed(upload.getGeneratedId(), upload.getLeedStatusMap());
                    }

                    private void updateLeed(String generatedId, Map leedStatusMap) {
                        leedRepository.updateServiceProvider(generatedId, leedStatusMap, new CallBack() {
                            @Override
                            public void onSuccess(Object object) {


                            }

                            @Override
                            public void onError(Object object) {
                                Utility.showLongMessage(getApplicationContext(), getString(R.string.server_error));
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

    }

    private void Login() {
        progressDialog = new ProgressDialogClass(this);
        progressDialog.showDialog(this.getString(R.string.SIGNING_IN), this.getString(R.string.PLEASE_WAIT));
        String code = "+91";
        String number = inputMobile.getText().toString().trim();
        final String password = inputPassword.getText().toString().trim();

        if (number.isEmpty() || number.length() < 10) {
            inputMobile.setError("Valid number is required");
            inputMobile.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            inputPassword.setError("Password is required");
            inputPassword.requestFocus();
            return;
        }

        final String phoneNumber = number;


        DatabaseReference Dref = FirebaseDatabase.getInstance().getReference("users");
        Dref.orderByChild("number").equalTo(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.getValue() != null) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                        User upload = postSnapshot.getValue(User.class);

                        progressDialog.dismissDialog();
                        String userid = upload.getUserid();

                        if (inputPassword.getText().toString().equalsIgnoreCase(upload.getPassword())) {
                            appSharedPreference.addUserDetails(upload);
                            appSharedPreference.createUserLoginSession();
                            Toast.makeText(LoginActivity.this, "Login Successfull", Toast.LENGTH_SHORT).show();
                            if (upload.getRole().equalsIgnoreCase("USER")) {
                                logintoapp();
                            } else if (upload.getRole().equalsIgnoreCase("SERVICE PROVIDER")) {
                                logintoapp();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();

                        }
                    }

                } else {
                    progressDialog.dismissDialog();
                    Toast.makeText(LoginActivity.this, "Login failed Please Register", Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

//    private void signInUserData(final String userId) {
//        userRepository.readUserByUserId(userId, new CallBack() {
//            @Override
//            public void onSuccess(Object object) {
//                if (object != null) {
//                    User user = (User) object;
//                    appSharedPreference.createUserLoginSession();
//                    appSharedPreference.addUserDetails(user);
//
//                    logintoapp();
//
//                } else {
//                    Utility.showTimedSnackBar(LoginActivity.this, inputPassword, getMessage(R.string.login_fail_try_again));
//                }
//                if (progressDialog != null)
//                    progressDialog.dismissDialog();
//            }
//
//            @Override
//            public void onError(Object object) {
//                if (progressDialog != null)
//                    progressDialog.dismissDialog();
//                Utility.showTimedSnackBar(LoginActivity.this, inputPassword, getMessage(R.string.login_fail_try_again));
//            }
//        });
//    }

    private void logintoapp() {
        Intent i = new Intent(this, MainActivity_User.class);
        startActivity(i);
        finish();
    }


    public void checkLoginState() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (appSharedPreference != null && appSharedPreference.getUserLoginStatus()) {
                        if (appSharedPreference.getGeneratedId() != null && appSharedPreference.getUserid() != null) {

                            String number = appSharedPreference.getNumber();
                            userRepository.readServiceProviderByMobileNumber(number, new CallBack() {
                                @Override
                                public void onSuccess(Object object) {
                                    if (object != null) {
                                        User Service_Provider = (User) object;
                                        setLeedStatus(Service_Provider);
                                        logintoapp();
                                    }
                                }

                                @Override
                                public void onError(Object object) {

                                }
                            });


                        }
                    }
                } catch (Exception e) {
                    ExceptionUtil.logException(e);
                }
            }
        });
    }

    private void setLeedStatus(User upload) {

        upload.setTokan(token);
        updateLeed(upload.getGeneratedId(), upload.getLeedStatusMap());
    }


    private void updateLeed(String generatedId, Map leedStatusMap) {
        leedRepository.updateServiceProvider(generatedId, leedStatusMap, new CallBack() {
            @Override
            public void onSuccess(Object object) {

            }

            @Override
            public void onError(Object object) {
                Utility.showLongMessage(getApplicationContext(), getString(R.string.server_error));
            }
        });
    }

    private String getMessage(int id) {
        return getString(id);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}

