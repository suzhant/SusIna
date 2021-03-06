package com.sushant.whatsapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sushant.whatsapp.Models.Groups;
import com.sushant.whatsapp.Models.Users;
import com.sushant.whatsapp.Utils.ImageUtils;
import com.sushant.whatsapp.databinding.ActivityFinalCreateGroupBinding;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class FinalCreateGroup extends AppCompatActivity {

    ActivityFinalCreateGroupBinding binding;
    ProgressDialog dialog;
    FirebaseDatabase database;
    ArrayList<Users> list;
    String uid, name, profilePic, mail;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    FirebaseStorage storage;
    String id, image;
    ValueEventListener eventListener;
    DatabaseReference reference1;
    ActivityResultLauncher<Intent> someActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinalCreateGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ColorDrawable colorDrawable
                = new ColorDrawable(Color.parseColor("#7C4DFF"));
        getSupportActionBar().setBackgroundDrawable(colorDrawable);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image");
        dialog.setCancelable(false);

        list = (ArrayList<Users>) getIntent().getSerializableExtra("participantList");

        database.getReference().child("Users").child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Users user = snapshot.getValue(Users.class);
                uid = user.getUserId();
                name = user.getUserName();
                profilePic = user.getProfilePic();
                mail = user.getMail();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //Generating group id
        UUID uuid = UUID.randomUUID();
        id = uuid + "";

        binding.btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.editName.getEditText().getText().toString().isEmpty()) {
                    binding.editName.setError("Field cannot be empty");
                    return;
                }
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
                Groups groups = new Groups();
                String GroupName = binding.editName.getEditText().getText().toString();
                groups.setGroupName(GroupName);
                groups.setGroupId(id);
                groups.setCreatedBy(uid);
                groups.setCreatedOn(timeStamp);
                database.getReference().child("GroupList").child(id).setValue(groups);
                createLastMessage();

                for (int i = 0; i < list.size(); i++) {
                    Users users = list.get(i);
                    DatabaseReference reference = database.getReference().child("Groups").child(users.getUserId()).child(id);
                    reference.setValue(groups).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            for (int i = 0; i < list.size(); i++) {
                                Users users = list.get(i);
                                users.setJoinedGroupOn(timeStamp);
                                if (FirebaseAuth.getInstance().getUid() != null) {
                                    if (FirebaseAuth.getInstance().getUid().equals(users.getUserId())) {
                                        users.setRole("Admin");
                                    } else {
                                        users.setRole("normal");
                                    }
                                    reference.child("participant").child(users.getUserId()).setValue(users);
                                }
                            }
                        }
                    }).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Users admin = new Users();
                            admin.setUserId(uid);
                            admin.setUserName(name);
                            admin.setProfilePic(profilePic);
                            admin.setMail(mail);
                            admin.setJoinedGroupOn(timeStamp);
                            admin.setRole("Admin");
                            reference.child("participant").child(admin.getUserId()).setValue(admin);
                        }
                    });
                }

                DatabaseReference reference = database.getReference().child("Groups").child(user.getUid()).child(id);
                reference.setValue(groups).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        for (int i = 0; i < list.size(); i++) {
                            Users users = list.get(i);
                            users.setJoinedGroupOn(timeStamp);
                            if (FirebaseAuth.getInstance().getUid() != null) {
                                if (FirebaseAuth.getInstance().getUid().equals(users.getUserId())) {
                                    users.setRole("Admin");
                                } else {
                                    users.setRole("normal");
                                }
                                reference.child("participant").child(users.getUserId()).setValue(users);
                            }
                        }
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Users admin = new Users();
                        admin.setUserId(uid);
                        admin.setUserName(name);
                        admin.setProfilePic(profilePic);
                        admin.setMail(mail);
                        admin.setRole("Admin");
                        admin.setJoinedGroupOn(timeStamp);
                        reference.child("participant").child(admin.getUserId()).setValue(admin);
                    }
                });

                database.getReference().child("Groups").child(FirebaseAuth.getInstance().getUid()).child(id).child("groupPP").setValue(image);
                reference1 = database.getReference().child("Groups").child(FirebaseAuth.getInstance().getUid()).child(id).child("participant");
                eventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                Users users = snapshot1.getValue(Users.class);
                                assert users != null;
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Groups").child(users.getUserId()).child(id);
                                HashMap<String, Object> map = new HashMap<>();
                                Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.avatar, null);
                                if (image != null) {
                                    map.put("groupPP", image);
                                }
                                reference.updateChildren(map);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                };
                reference1.addValueEventListener(eventListener);

                Intent intent = new Intent(FinalCreateGroup.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                Toast.makeText(getApplicationContext(), "Group Created Successfully", Toast.LENGTH_SHORT).show();
            }
        });

        binding.imgPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                someActivityResultLauncher.launch(intent);
            }
        });

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            // There are no request codes
                            Uri sFile = result.getData().getData();
                            Bitmap bitmap = null;
                            try {
                                bitmap = ImageUtils.handleSamplingAndRotationBitmap(FinalCreateGroup.this, sFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            assert bitmap != null;
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
                            byte[] img = baos.toByteArray();
                            binding.imgProfile.setImageBitmap(bitmap);

                            final StorageReference reference = storage.getReference().child("Group Pictures").child(id);
                            dialog.show();

                            reference.putBytes(img).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            dialog.dismiss();
                                            image = uri.toString();
                                            Toast.makeText(FinalCreateGroup.this, "Profile Pic Updated", Toast.LENGTH_SHORT).show();
                                            binding.btnCreate.setEnabled(true);
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
    }

    private void createLastMessage() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("lastMessage", "Say Hi!!");
        map.put("senderName", "name");
        map.put("senderId", "uid");
        database.getReference().child("Group Chat").child("Last Messages").child(id).setValue(map);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reference1 != null) {
            Log.d("REF", "onDestroy: Removed Event");
            reference1.removeEventListener(eventListener);
        }
    }

}