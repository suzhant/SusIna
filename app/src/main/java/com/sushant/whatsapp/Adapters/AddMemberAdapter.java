package com.sushant.whatsapp.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sushant.whatsapp.Interface.isClicked;
import com.sushant.whatsapp.Models.Users;
import com.sushant.whatsapp.R;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddMemberAdapter extends RecyclerView.Adapter<AddMemberAdapter.viewHolder> {

    ArrayList<Users> list;
    Context context;
    isClicked clicked;
    String Gid;

    public AddMemberAdapter(ArrayList<Users> list, Context context,isClicked clicked,String Gid) {
        this.list = list;
        this.context = context;
        this.clicked=clicked;
        this.Gid=Gid;
    }


    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_participant, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddMemberAdapter.viewHolder holder, @SuppressLint("RecyclerView") int position) {
        Users users = list.get(position);
        Glide.with(context).load(users.getProfilePic()).placeholder(R.drawable.avatar).diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.image);
        holder.userName.setText(users.getUserName());



        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseDatabase.getInstance().getReference().child("Groups").child(FirebaseAuth.getInstance().getUid())
                        .child(Gid).child("participant").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snapshot1:snapshot.getChildren()){
                            Users participant=snapshot1.getValue(Users.class);
                            if (participant.getUserId().equals(users.getUserId())){
                                Toast.makeText(context.getApplicationContext(), "Already Member of the group", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        if (holder.checkbox.getVisibility()==View.VISIBLE){
                            holder.checkbox.setVisibility(View.INVISIBLE);
                            clicked.isClicked(false,position);
                        }else {
                            holder.checkbox.setVisibility(View.VISIBLE);
                            clicked.isClicked(true,position);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }
        });


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    public class viewHolder extends RecyclerView.ViewHolder {

        public CircleImageView image;
        public ImageView blackCircle,checkbox;
        public TextView userName, lastMessage;


        public viewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.userName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            blackCircle=itemView.findViewById(R.id.black_circle);
            checkbox=itemView.findViewById(R.id.checkbox);

        }
    }


}