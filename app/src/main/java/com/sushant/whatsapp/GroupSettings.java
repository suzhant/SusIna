package com.sushant.whatsapp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.sushant.whatsapp.Adapters.MemberAdapter;
import com.sushant.whatsapp.Models.Users;
import com.sushant.whatsapp.databinding.ActivityGroupSettingsBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class GroupSettings extends AppCompatActivity {

    ActivityGroupSettingsBinding binding;
    ValueEventListener valueEventListener1, valueEventListener;
    ArrayList<Users> list = new ArrayList<>();
    FirebaseDatabase database;
    LinearLayoutManager layoutManager;
    MemberAdapter adapter;
    DatabaseReference ref;
    String Gid, GName, GPP, CreatedOn, CreatedBy;
    AlertDialog dialog;
    ConstraintLayout constraintLayout;
    String role;
    Query participant;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().hide();
        Gid = getIntent().getStringExtra("GId1");
        GName = getIntent().getStringExtra("GName1");
        GPP = getIntent().getStringExtra("GPic1");
        CreatedOn = getIntent().getStringExtra("CreatedOn1");
        CreatedBy = getIntent().getStringExtra("CreatedBy1");

        database = FirebaseDatabase.getInstance();


        DatabaseReference reference = database.getReference().child("Groups").child(FirebaseAuth.getInstance().getUid()).child(Gid).child("participant");
        participant = reference.orderByChild("userId").equalTo(FirebaseAuth.getInstance().getUid());
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        Users users = snapshot1.getValue(Users.class);
                        assert users != null;
                        role = users.getRole();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        participant.addValueEventListener(valueEventListener);


        binding.btnGroupSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ChangeGroupProfile.class);
                intent.putExtra("Gid", Gid);
                intent.putExtra("GName", GName);
                startActivity(intent);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(GroupSettings.this);
        builder.setMessage("Do you want to leave Group?")
                .setTitle("Leave Group");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Gid);
                if (!role.equals("Admin")) {
                    DatabaseReference reference = database.getReference().child("Groups").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child(Gid).child("participant");
                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                Users users = snapshot1.getValue(Users.class);
                                assert users != null;
                                if (!users.getUserId().equals(FirebaseAuth.getInstance().getUid())) {
                                    HashMap<String, Object> map = new HashMap<>();
                                    map.put(FirebaseAuth.getInstance().getUid(), null);
                                    database.getReference().child("Groups").child(users.getUserId()).child(Gid).child("participant").updateChildren(map);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                } else {
                    DatabaseReference reference1 = database.getReference().child("Groups").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child(Gid).child("participant");
                    reference1.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                Users users = snapshot1.getValue(Users.class);
                                assert users != null;
                                if (!users.getUserId().equals(FirebaseAuth.getInstance().getUid())) {
                                    DatabaseReference reference2 = database.getReference().child("Groups").child(users.getUserId()).child(Gid).child("participant");
                                    HashMap<String, Object> map = new HashMap<>();
                                    map.put(FirebaseAuth.getInstance().getUid(), null);
                                    reference2.updateChildren(map);
                                    Query query = reference2.orderByValue().limitToLast(1);
                                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for (DataSnapshot snapshot3 : snapshot.getChildren()) {
                                                String userId = snapshot3.child("userId").getValue(String.class);
                                                reference2.child(userId).child("role").setValue("Admin");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
                database.getReference().child("Groups").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child(Gid).setValue(null);

                Intent intent = new Intent(GroupSettings.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        dialog = builder.create();

        binding.txtGroupName.setText(GName);

//        adapter = new MemberAdapter(list,Gid,this);
//        binding.participantRecycler.setItemAnimator(new DefaultItemAnimator());
//        binding.participantRecycler.setAdapter(adapter);
//        binding.participantRecycler.addItemDecoration(new DividerItemDecoration(binding.participantRecycler.getContext(), DividerItemDecoration.VERTICAL));
//        layoutManager = new LinearLayoutManager(this);
//        binding.participantRecycler.setLayoutManager(layoutManager);

        binding.btnShowMember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(),
//                        R.anim.bottom_up)
                final Dialog memberDialog = new Dialog(GroupSettings.this);
                memberDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                memberDialog.setContentView(R.layout.bottom_sheet_menu);
                RecyclerView participantRecycler = memberDialog.findViewById(R.id.participantRecyclerInBottom);
//                constraintLayout=memberDialog.findViewById(R.id.rootBottomSheet);
//                constraintLayout.startAnimation(bottomUp);
                adapter = new MemberAdapter(list, Gid, getApplicationContext());
                participantRecycler.setItemAnimator(new DefaultItemAnimator());
                participantRecycler.setAdapter(adapter);
                participantRecycler.addItemDecoration(new DividerItemDecoration(participantRecycler.getContext(), DividerItemDecoration.VERTICAL));
                layoutManager = new LinearLayoutManager(getApplicationContext());
                participantRecycler.setLayoutManager(layoutManager);
                getAllUsers();

                memberDialog.show();
                memberDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                memberDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                memberDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
                memberDialog.getWindow().setGravity(Gravity.BOTTOM);
                memberDialog.getWindow().getAttributes().windowAnimations = R.style.NoAnimation;
            }
        });

        binding.backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

//        getAllUsers();

        binding.btnAddParticipant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), AddParticipant.class);
                intent.putExtra("GId1", Gid);
                intent.putExtra("GName1", GName);
                intent.putExtra("GPic1", GPP);
                intent.putExtra("CreatedOn1", CreatedOn);
                intent.putExtra("CreatedBy1", CreatedBy);
                startActivity(intent);
            }
        });

        binding.btnLeaveGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

    }

    private void getAllUsers() {
        valueEventListener1 = new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Users users = dataSnapshot.getValue(Users.class);
                    assert users != null;
                    users.setUserId(dataSnapshot.getKey());
                    list.add(users);

                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        ref = FirebaseDatabase.getInstance().getReference("Groups").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child(Gid).child("participant");
        ref.addValueEventListener(valueEventListener1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ref != null) {
            ref.removeEventListener(valueEventListener1);
        }
        if (participant != null) {
            participant.removeEventListener(valueEventListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ref != null) {
            ref.removeEventListener(valueEventListener1);
        }
        if (participant != null) {
            participant.removeEventListener(valueEventListener);
        }
    }
}