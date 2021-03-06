package com.sushant.whatsapp;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sushant.whatsapp.Models.Users;
import com.sushant.whatsapp.databinding.ActivityProfileBinding;

import java.util.HashMap;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {
    ActivityProfileBinding binding;
    FirebaseDatabase database;
    boolean friend = false;
    String sendername, pp, userStatus;
    boolean notify = false;
    String userToken;
    Handler handler;
    Runnable runnable;
    ValueEventListener eventListener;
    DatabaseReference reference;
    String Receiverid, receiverEmail, receiverStatus, receiverName, receiverProfilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPurple));

        database = FirebaseDatabase.getInstance();

        Receiverid = getIntent().getStringExtra("UserIdPA");
//       receiverName = getIntent().getStringExtra("UserNamePA");
//        receiverProfilePic = getIntent().getStringExtra("ProfilePicPA");
//        receiverEmail = getIntent().getStringExtra("EmailPA");
//        receiverStatus = getIntent().getStringExtra("StatusPA");

        //receiverInfo
        database.getReference().child("Users").child(Receiverid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users users = snapshot.getValue(Users.class);
                        assert users != null;
                        receiverName = users.getUserName();
                        receiverProfilePic = users.getProfilePic();
                        receiverStatus = users.getStatus();
                        receiverEmail = users.getMail();
                        Glide.with(getApplicationContext()).load(receiverProfilePic).placeholder(R.drawable.avatar).into(binding.imgProfile);
                        binding.txtEmail.setText(receiverEmail);
                        binding.txtUserName.setText(receiverName);
                        binding.txtAbout.setText(receiverStatus);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        //senderInfo
        database.getReference().child("Users").child(Objects.requireNonNull(user.getUid()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users users = snapshot.getValue(Users.class);
                        assert users != null;
                        sendername = users.getUserName();
                        pp = users.getProfilePic();
                        userStatus = users.getStatus();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        reference = database.getReference("Users").child(user.getUid()).child("Friends");
        eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        Users users = snapshot1.getValue(Users.class);
                        assert users != null;
                        if (Receiverid.equals(users.getUserId())) {
                            if (users.getRequest().equals("Accepted")) {
                                binding.btnAddFriend.setText("Unfriend");
                                binding.btnAddFriend.setBackgroundTintList(getColorStateList(R.color.colorUnfriend)); //using material design button color
//                                binding.btnAddFriend.setBackgroundColor(Color.parseColor("#FF3D00"));
                                friend = true;
                            }
                            if (users.getRequest().equals("Req_Sent")) {
                                binding.btnAddFriend.setText("Cancel Friend Request");
                                binding.btnAddFriend.setBackgroundTintList(getColorStateList(R.color.colorUnfriend));
                                //   binding.btnAddFriend.setBackgroundColor(Color.RED);
                                friend = true;
                            }

                            if (users.getRequest().equals("Req_Pending")) {
                                binding.btnAddFriend.setVisibility(View.GONE);
                                binding.btnLayout.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    if (!snapshot.child(Receiverid).exists()) {
                        binding.btnAddFriend.setVisibility(View.VISIBLE);
                        binding.btnAddFriend.setText("Add friend");
                        binding.btnAddFriend.setBackgroundTintList(getColorStateList(R.color.colorAddFriend));
                        //  binding.btnAddFriend.setBackgroundColor(0x09af00);
                        binding.btnLayout.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        reference.addValueEventListener(eventListener);

        binding.topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        binding.btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.getReference().child("Users").child(user.getUid()).child("Friends").child(Receiverid).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        database.getReference().child("Users").child(Receiverid).child("Friends").child(user.getUid()).removeValue();
                    }
                });
                binding.btnLayout.setVisibility(View.GONE);
                binding.btnAddFriend.setVisibility(View.VISIBLE);
                friend = false;
            }
        });

        binding.btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, Object> obj1 = new HashMap<>();
                obj1.put("request", "Accepted");

                HashMap<String, Object> obj2 = new HashMap<>();
                obj2.put("request", "Accepted");

                database.getReference().child("Users").child(user.getUid()).child("Friends").child(Receiverid).updateChildren(obj1).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        database.getReference().child("Users").child(Receiverid).child("Friends").child(user.getUid()).updateChildren(obj2);
                    }
                });
                binding.btnLayout.setVisibility(View.GONE);
                binding.btnAddFriend.setVisibility(View.VISIBLE);
                friend = true;
            }
        });

        binding.btnAddFriend.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onClick(View view) {
                if (friend) {
                    database.getReference().child("Users").child(user.getUid()).child("Friends").child(Receiverid).removeValue();
                    database.getReference().child("Users").child(Receiverid).child("Friends").child(user.getUid()).removeValue();
                    binding.btnAddFriend.setText("Add friend");
                    binding.btnAddFriend.setBackgroundTintList(getColorStateList(R.color.colorAddFriend));
                    // binding.btnAddFriend.setBackgroundColor(0x09af00);
                    friend = false;
                } else {
                    notify = true;
                    Users user1 = new Users();
                    user1.setMail(receiverEmail);
                    user1.setUserName(receiverName);
                    user1.setUserId(Receiverid);
                    user1.setProfilePic(receiverProfilePic);
                    user1.setStatus(receiverStatus);
                    user1.setTyping("Not Typing");
                    user1.setLastMessage("Say Hi!!");
                    user1.setRequest("Req_Sent");

                    Users user2 = new Users();
                    user2.setMail(user.getEmail());
                    user2.setUserName(sendername);
                    user2.setUserId(user.getUid());
                    user2.setProfilePic(pp);
                    user2.setStatus(userStatus);
                    user2.setTyping("Not Typing");
                    user2.setLastMessage("Say Hi!!");
                    user2.setRequest("Req_Pending");

                    if (notify) {
                        sendNotification(Receiverid, sendername, pp);
                    }
                    notify = false;


                    database.getReference().child("Users").child(user.getUid()).child("Friends").child(Receiverid).setValue(user1).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            database.getReference().child("Users").child(Receiverid).child("Friends").child(user.getUid()).setValue(user2).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(@NonNull Void unused) {
                                    Toast.makeText(getApplicationContext(), "Friend request sent to " + receiverName, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });

                    binding.btnAddFriend.setText("Unfriend");
                    binding.btnAddFriend.setBackgroundTintList(getColorStateList(R.color.colorUnfriend));
                    //    binding.btnAddFriend.setBackgroundColor(Color.RED);
                    friend = true;
                }

            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void sendNotification(String receiver, String userName, String image) {
        database.getReference().child("Users").child(receiver).child("Token").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userToken = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                FcmNotificationsSender fcmNotificationsSender = new FcmNotificationsSender(userToken, userName, "sent you a friend request.", image, Receiverid, receiverEmail, FirebaseAuth.getInstance().getUid(), "request",
                        "FriendRequest", ".ProfileActivity", getApplicationContext(), ProfileActivity.this);
                fcmNotificationsSender.SendNotifications();
            }
        };
        if (notify) {
            handler.postDelayed(runnable, 2000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reference != null) {
            Log.d("REF1", "onDestroy: called");
            reference.removeEventListener(eventListener);
        }
    }

    @Override
    public void onBackPressed() {
        finishAfterTransition();
        super.onBackPressed();
    }
}