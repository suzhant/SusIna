package com.sushant.whatsapp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sushant.whatsapp.Adapters.ShareAdapter;
import com.sushant.whatsapp.Interface.isClicked;
import com.sushant.whatsapp.Models.Messages;
import com.sushant.whatsapp.Models.Users;
import com.sushant.whatsapp.databinding.ActivityShareBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class ShareActivity extends AppCompatActivity {
    ActivityShareBinding binding;
    ArrayList<Users> list = new ArrayList<>();
    FirebaseDatabase database;
    LinearLayoutManager layoutManager;
    ShareAdapter adapter;
    DatabaseReference ref, tokenRef, senderRef;
    ValueEventListener valueEventListener1, tokenListener, senderListener;
    ArrayList<Users> receiver = new ArrayList<>();
    int size = 0;
    isClicked clicked;
    DatabaseReference databaseReference;
    FirebaseStorage storage;
    String senderId;
    FirebaseAuth auth;
    Parcelable image, text;
    ProgressDialog dialog;
    Handler handler;
    Runnable runnable;
    String userToken, sendername, senderPP, email, stringContainingYoutubeLink;
    int i = 0;
    BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            image = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            text = intent.getParcelableExtra(Intent.EXTRA_TEXT);
            Bundle extras = getIntent().getExtras();
            stringContainingYoutubeLink = extras.getString(Intent.EXTRA_TEXT);
        }

        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

        senderId = auth.getUid();
        broadcastReceiver = new InternetCheckServices();
        registerBroadcastReceiver();

        clicked = new isClicked() {
            @Override
            public void isClicked(Boolean b, int position) {
                Users users = list.get(position);
                if (b) {
                    receiver.add(users);
                    size++;
                } else {
                    receiver.remove(users);
                    size--;
                }
            }
        };

        adapter = new ShareAdapter(list, this, clicked);
        binding.sendRecyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.sendRecyclerView.setAdapter(adapter);
        binding.sendRecyclerView.addItemDecoration(new DividerItemDecoration(binding.sendRecyclerView.getContext(), DividerItemDecoration.VERTICAL));
        layoutManager = new LinearLayoutManager(this);
        binding.sendRecyclerView.setLayoutManager(layoutManager);
        database = FirebaseDatabase.getInstance();
        getAllUsers();

        binding.txtSend.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onClick(View view) {
                if (size > 0) {
                    CheckConnection checkConnection = new CheckConnection();
                    if (checkConnection.isConnected(getApplicationContext())) {
                        Toast.makeText(ShareActivity.this, "Please connect to the internet", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (intent.getType().contains("image/")) {
                        for (int i = 0; i < receiver.size(); i++) {
                            dialog.setMessage("Sending Image");
                            dialog.show();
                            Users users = receiver.get(i);
                            String receiverId = users.getUserId();
                            String senderRoom = senderId + receiverId;
                            String receiverRoom = receiverId + senderId;
                            String profilePic = users.getProfilePic();
                            uploadImageToFirebase(Uri.parse(image.toString()), senderRoom, receiverRoom, receiverId, profilePic);
                        }
                    } else if (intent.getType().contains("text/plain")) {
                        for (int i = 0; i < receiver.size(); i++) {
                            dialog.setMessage("Sending link");
                            dialog.show();
                            Users users = receiver.get(i);
                            String receiverId = users.getUserId();
                            String senderRoom = senderId + receiverId;
                            String receiverRoom = receiverId + senderId;
                            String profilePic = users.getProfilePic();
                            sendMessage(stringContainingYoutubeLink, profilePic, receiverId, senderRoom, receiverRoom);
                        }
                    }
                } else {
                    Toast.makeText(ShareActivity.this, "Please Select User", Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                binding.sendRecyclerView.setAdapter(new ShareAdapter(list, ShareActivity.this, clicked));
                receiver.clear();
                i = 0;
                size = 0;
                Toast.makeText(ShareActivity.this, "sent successfully", Toast.LENGTH_SHORT).show();
            }
        });

        binding.imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(ShareActivity.this, MainActivity.class);
                startActivity(intent1);
                finish();
            }
        });

        senderRef = database.getReference().child("Users").child(Objects.requireNonNull(auth.getUid()));
        senderListener = new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users users = snapshot.getValue(Users.class);
                sendername = users.getUserName();
                senderPP = users.getProfilePic();
                email = users.getMail();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        senderRef.addValueEventListener(senderListener);

    }


    private void getAllUsers() {
        valueEventListener1 = new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    list.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Users users = dataSnapshot.getValue(Users.class);
                        assert users != null;
                        if (users.getUserId() != null && !users.getUserId().equals(FirebaseAuth.getInstance().getUid())) {
                            if ("Accepted".equals(users.getRequest())) {
                                list.add(users);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        ref = FirebaseDatabase.getInstance().getReference("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child("Friends");
        ref.addValueEventListener(valueEventListener1);
    }

    private void uploadImageToFirebase(Uri uri, String senderRoom, String receiverRoom, String receiverId, String profilePic) {
        i++;
        Calendar calendar = Calendar.getInstance();
        final StorageReference reference = storage.getReference().child("Chats Images").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child(calendar.getTimeInMillis() + "");
        UploadTask uploadTask = reference.putFile(uri);
        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                // Uri fdelete = Uri.fromFile(new File(uri.toString()));
                // File fdelete= new File(uri.toString());
                //File fdelete = new File(Objects.requireNonNull(getFilePath(uri)));

                if (task.isSuccessful()) {
                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @RequiresApi(api = Build.VERSION_CODES.P)
                        @Override
                        public void onSuccess(Uri uri) {
                            String filePath = uri.toString();
                            Date date = new Date();
                            final Messages model = new Messages(senderId, profilePic, date.getTime());
                            model.setMessage("send you a photo");
                            model.setImageUrl(filePath);
                            model.setType("photo");
                            updateLastMessage(receiverId, "photo.jpg");

                            sendNotification(receiverId, sendername, filePath, senderPP, email, senderId, "photo");

                            database.getReference().child("Chats").child(senderRoom).push().setValue(model)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            database.getReference().child("Chats").child(receiverRoom).push().setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    if (i == receiver.size()) {
                                                        dialog.dismiss();
                                                    }
                                                }
                                            });
                                        }
                                    });
                        }
                    });
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void sendMessage(String message, String profilePic, String receiverId, String senderRoom, String receiverRoom) {
        if (!message.isEmpty()) {
            i++;
            final Messages model = new Messages(senderId, message, profilePic);
            Date date = new Date();
            model.setTimestamp(date.getTime());
            model.setType("text");
            updateLastMessage(receiverId, "sent a link");

            sendNotification(receiverId, sendername, message, senderPP, email, senderId, "text");

            database.getReference().child("Chats").child(senderRoom).push().setValue(model)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            database.getReference().child("Chats").child(receiverRoom).push().setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    if (i == receiver.size()) {
                                        dialog.dismiss();
                                    }
                                }
                            });
                        }
                    });
        }
    }

    private void updateLastMessage(String receiverId, String message) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("lastMessage", message);
        database.getReference().child("Users").child(senderId).child("Friends").child(receiverId).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                database.getReference().child("Users").child(receiverId).child("Friends").child(senderId).updateChildren(map);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void sendNotification(String receiver, String userName, String msg, String image, String email, String senderId, String msgType) {
        tokenRef = database.getReference().child("Users").child(receiver).child("Token");
        tokenListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userToken = snapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        tokenRef.addValueEventListener(tokenListener);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                FcmNotificationsSender fcmNotificationsSender = new FcmNotificationsSender(userToken, userName, msg, image, receiver, email, senderId, msgType,
                        "Chat", ".ShareActivity", getApplicationContext(), ShareActivity.this);
                fcmNotificationsSender.SendNotifications();
            }
        };
        handler.postDelayed(runnable, 2000);
    }

    private void registerBroadcastReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private void unregisterNetwork() {
        try {
            unregisterReceiver(broadcastReceiver);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (senderRef != null) {
            senderRef.removeEventListener(senderListener);
        }
        if (tokenRef != null) {
            tokenRef.removeEventListener(tokenListener);
        }
        if (ref != null) {
            ref.removeEventListener(valueEventListener1);
        }
        unregisterNetwork();
    }
}