package com.sushant.whatsapp.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sushant.whatsapp.Models.Users;
import com.sushant.whatsapp.ProfileActivity;
import com.sushant.whatsapp.R;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.viewHolder> {

    ArrayList<Users> list;
    Context context;
    FirebaseDatabase database;

    public FriendRequestAdapter(ArrayList<Users> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_friend_request_users, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        Users users = list.get(position);
        database = FirebaseDatabase.getInstance();
        Glide.with(context).load(users.getProfilePic()).placeholder(R.drawable.avatar).diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.image);
        holder.userName.setText(users.getUserName());
        holder.txtAbout.setText(users.getStatus());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                Query checkStatus = reference.orderByChild("userId").equalTo(users.getUserId());
                checkStatus.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String StatusFromDB = snapshot.child(users.getUserId()).child("status").getValue(String.class);
                            Intent intent = new Intent(context, ProfileActivity.class);
                            intent.putExtra("UserIdPA", users.getUserId());
                            intent.putExtra("ProfilePicPA", users.getProfilePic());
                            intent.putExtra("UserNamePA", users.getUserName());
                            intent.putExtra("StatusPA", StatusFromDB);
                            intent.putExtra("EmailPA", users.getMail());
                            context.startActivity(intent);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        holder.btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, Object> obj1 = new HashMap<>();
                obj1.put("request", "Accepted");

                HashMap<String, Object> obj2 = new HashMap<>();
                obj2.put("request", "Accepted");

                database.getReference().child("Users").child(user.getUid()).child("Friends").child(users.getUserId()).updateChildren(obj1).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        database.getReference().child("Users").child(users.getUserId()).child("Friends").child(user.getUid()).updateChildren(obj2).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(@NonNull Void unused) {
                                Toast.makeText(context.getApplicationContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        holder.btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                database.getReference().child("Users").child(user.getUid()).child("Friends").child(users.getUserId()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        database.getReference().child("Users").child(users.getUserId()).child("Friends").child(user.getUid()).removeValue();
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public static class viewHolder extends RecyclerView.ViewHolder {

        public CircleImageView image;
        public ImageView blackCircle;
        public TextView userName, txtResponse, txtAbout;
        public Button btnAccept, btnReject;


        public viewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.userName);
            blackCircle = itemView.findViewById(R.id.black_circle);
            btnAccept = itemView.findViewById(R.id.btnAcceptReq);
            btnReject = itemView.findViewById(R.id.btnRejectReq);
            txtResponse = itemView.findViewById(R.id.txtResponse);
            txtAbout = itemView.findViewById(R.id.txtAbout);
        }
    }
}
