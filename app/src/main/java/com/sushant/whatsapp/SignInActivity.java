package com.sushant.whatsapp;

import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.sushant.whatsapp.Models.Users;
import com.sushant.whatsapp.databinding.ActivitySignInBinding;

import java.util.concurrent.Executor;

public class SignInActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 10010;
    Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private ActivitySignInBinding binding;
    private ProgressDialog dialog;
    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    FirebaseDatabase database;
    BroadcastReceiver broadcastReceiver;
    int numberOfTries;
    boolean mTimerRunning;
    ActivityResultLauncher<Intent> someActivityResultLauncher;

    SharedPreferences sharedPreferences;
    boolean flag;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.chatNavColor));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.chatNavColor));

        broadcastReceiver = new InternetCheckServices();
//        registerBroadcastReceiver();

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        //automatically sign in user
        if (auth.getCurrentUser() != null && auth.getCurrentUser().isEmailVerified()) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }

        dialog = new ProgressDialog(this);
        dialog.setTitle("Login");
        dialog.setMessage("Login to your account");

        binding.txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignInActivity.this, ForgotPassword.class);
                startActivity(intent);
            }
        });


        //sign in using biometrics
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("MY_APP_TAG", "No biometric features available on this device.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // Prompts the user to create credentials that your app accepts.
                Toast.makeText(this, "Your device doesn't have fingerprint saved", Toast.LENGTH_SHORT).show();
                final Intent enrollIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    startActivity(new Intent(enrollIntent));
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    startActivity(new Intent(Settings.ACTION_FINGERPRINT_ENROLL));
                } else {
                    startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                }
                // startActivityForResult(enrollIntent, REQUEST_CODE);
                break;
        }
        executor = ContextCompat.getMainExecutor(SignInActivity.this);
        biometricPrompt = new BiometricPrompt(this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
                boolean isLogin = sharedPreferences.getBoolean("isGoogle", flag);
                if (!isLogin) {
                    String email = sharedPreferences.getString("email", "");
                    String pass = sharedPreferences.getString("password", "");
                    if (!email.isEmpty() || !pass.isEmpty()) {
                        performAuth(email, pass);
                    } else {
                        Toast.makeText(getApplicationContext(), "This is the first time you're using Biometric!! Please sign in manually", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    signIn();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for my app")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Use account password")
                .build();


        binding.imgFingerPrint.setOnClickListener(view -> {
            biometricPrompt.authenticate(promptInfo);
        });

        sharedPreferences = getSharedPreferences("data", MODE_PRIVATE);
        boolean isLogin = sharedPreferences.getBoolean("isLogin", false);
        if (isLogin) {
            binding.imgFingerPrint.setVisibility(View.VISIBLE);
        }

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("843849987770-2imhf1vifnfvsms5c512r88op2pjkqav.apps.googleusercontent.com") //json file client_id
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);


        binding.btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideSoftKeyboard(SignInActivity.this);
                CheckConnection checkConnection = new CheckConnection();
                if (checkConnection.isConnected(getApplicationContext())) {
                    showCustomDialog();
                    return;
                }
                String email = binding.editEmail.getEditText().getText().toString().trim();
                String pass = binding.editPass.getEditText().getText().toString().trim();
                if (email.isEmpty() && pass.isEmpty()) {
                    binding.editEmail.setErrorEnabled(true);
                    binding.editPass.setErrorEnabled(true);
                    binding.editEmail.setStartIconTintList(ColorStateList.valueOf(getResources().getColor(R.color.design_default_color_error)));
                    binding.editPass.setStartIconTintList(ColorStateList.valueOf(getResources().getColor(R.color.design_default_color_error)));
                    binding.editEmail.setError("Field cannot be empty");
                    binding.editPass.setError("Field cannot be empty");
                    return;
                } else if (!emailValidation() | !passValidation()) {
                    return;
                }
                if (numberOfTries < 5) {
                    performAuth(email, pass);
                } else {
                    if (!mTimerRunning) {
                        startCountDownTimer();
                    }
                }
            }
        });


        binding.txtSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });


        binding.btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        someActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        // Google Sign In was successful, authenticate with Firebase
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d("TAG", "firebaseAuthWithGoogle:" + account.getId());
                        firebaseAuthWithGoogle(account.getIdToken());

                    } catch (ApiException e) {
                        // Google Sign In failed, update UI appropriately
                        Log.w("TAG", "Google sign in failed", e);
                    }
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterNetwork();
    }

    //Google Authentication
    int RC_SIGN_IN = 65;

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        someActivityResultLauncher.launch(signInIntent);
    }


    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("TAG", "signInWithCredential:success");
//                            database.goOnline();
                            flag = true;
                            FirebaseUser user = auth.getCurrentUser();

                            Users users = new Users();
                            users.setMail(user.getEmail());
                            users.setUserId(user.getUid());
                            users.setUserName(user.getDisplayName());
                            users.setProfilePic(user.getPhotoUrl().toString());
//                            userStatus("online");
                            database.getReference().child("Users").child(user.getUid()).setValue(users);


                            SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
                            editor.putBoolean("isGoogle", flag);
                            editor.apply();

                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            intent.putExtra("googleToken", idToken);
                            startActivity(intent);
                            Toast.makeText(SignInActivity.this, "Sign In Successful", Toast.LENGTH_SHORT).show();

                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                            // updateUI(null);
                        }
                    }
                });
    }


    //Firebase Email Authentication
    public void performAuth(String email, String password) {
        dialog.show();
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        dialog.dismiss();
                        if (task.isSuccessful()) {
//                            database.goOnline();
                            flag = false;
                            binding.etEmail.setText("");
                            binding.etPass.setText("");
                            SharedPreferences.Editor editor = getSharedPreferences("data", MODE_PRIVATE).edit();
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.putBoolean("isLogin", true);
                            editor.putBoolean("isGoogle", flag);
                            editor.apply();

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            Toast.makeText(SignInActivity.this, "Sign In Successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SignInActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            numberOfTries++;
                        }
                    }
                });

    }

    private void startCountDownTimer() {
        binding.txtTimerMessage.setVisibility(View.VISIBLE);
        new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long l) {
                binding.txtTimerMessage.setText("Retry after: " + l / 1000 + " sec");
            }

            @Override
            public void onFinish() {
                binding.txtTimerMessage.setVisibility(View.GONE);
                numberOfTries = 0;
                mTimerRunning = false;
            }
        }.start();
        mTimerRunning = true;
    }

    public boolean emailValidation() {
        String email = binding.editEmail.getEditText().getText().toString().trim();
        if (email.isEmpty()) {
            binding.editEmail.setErrorEnabled(true);
            binding.editEmail.setError("Field cannot be empty");
            binding.editEmail.setStartIconTintList(ColorStateList.valueOf(getResources().getColor(R.color.design_default_color_error)));
            binding.editEmail.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmail.setErrorEnabled(true);
            binding.editEmail.setError("Please provide a valid email");
            binding.editEmail.setStartIconTintList(ColorStateList.valueOf(getResources().getColor(R.color.design_default_color_error)));
            binding.editEmail.requestFocus();
            return false;
        } else {
            binding.editEmail.setErrorEnabled(false);
            binding.editEmail.setStartIconTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPurple)));
            binding.editEmail.clearFocus();
            return true;
        }
    }

    public boolean passValidation() {
        String pass = binding.editPass.getEditText().getText().toString().trim();
        if (pass.isEmpty()) {
            binding.editPass.setErrorEnabled(true);
            binding.editPass.setError("Field cannot be empty");
            binding.editPass.setStartIconTintList(ColorStateList.valueOf(getResources().getColor(R.color.design_default_color_error)));
            binding.editPass.requestFocus();
            return false;
        } else {
            binding.editPass.setErrorEnabled(false);
            binding.editPass.setStartIconTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPurple)));
            binding.editPass.clearFocus();
            return true;
        }
    }

    private void showCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignInActivity.this);
        builder.setMessage("Please connect to the internet to proceed forward")
                .setTitle("No Connection")
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                }).setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        }).show();
    }

    public void hideSoftKeyboard(Activity activity) {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}