package com.sushant.whatsapp.Adapters;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sushant.whatsapp.GroupChatActivity;
import com.sushant.whatsapp.Models.Groups;
import com.sushant.whatsapp.R;

import java.util.ArrayList;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.viewHolder> {

    ArrayList<Groups> list;
    Context context;
    String lastMsg;

    public GroupAdapter(ArrayList<Groups> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sample_group, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        Groups groups = list.get(position);
//        FirebaseMessaging.getInstance().subscribeToTopic(groups.getGroupId());
        Glide.with(context).load(groups.getGroupPP()).placeholder(R.drawable.avatar).into(holder.image);
        holder.groupName.setText(groups.getGroupName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, GroupChatActivity.class);
                intent.putExtra("GId", groups.getGroupId());
                intent.putExtra("GPic", groups.getGroupPP());
                intent.putExtra("GName", groups.getGroupName());
                intent.putExtra("CreatedOn", groups.getCreatedOn());
                intent.putExtra("CreatedBy", groups.getCreatedBy());
                context.startActivity(intent);
            }
        });


        if (groups.getGroupId() != null) {
            FirebaseDatabase.getInstance().getReference().child("Group Chat").child("Last Messages").child(groups.getGroupId())
                    .addValueEventListener(new ValueEventListener() {
                        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            lastMsg = snapshot.child("lastMessage").getValue(String.class);
                            String senderName = snapshot.child("senderName").getValue(String.class);
                            String senderId = snapshot.child("senderId").getValue(String.class);
                            holder.lastMessage.setText(lastMsg);
                            if (Objects.equals(FirebaseAuth.getInstance().getUid(), senderId)) {
                                holder.lastMessage.setText("You: " + lastMsg);
                            } else if ("Say Hi!!".equals(lastMsg)) {
                                holder.lastMessage.setText(lastMsg);
                            } else {
                                holder.lastMessage.setText(senderName + ": " + lastMsg);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }

        if (groups.getGroupId() != null) {
            FirebaseDatabase.getInstance().getReference().child("Groups").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).child(groups.getGroupId())
                    .addValueEventListener(new ValueEventListener() {
                        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                Groups groups1 = snapshot.getValue(Groups.class);
                                assert groups1 != null;
                                if (groups1.getSeen() != null) {
                                    if (groups1.getSeen().equals("false")) {
                                        holder.groupName.setTextColor(Color.BLACK);
                                        holder.groupName.setTypeface(null, Typeface.BOLD);
                                        holder.lastMessage.setTypeface(null, Typeface.BOLD);
                                        holder.lastMessage.setTextColor(Color.BLACK);
                                    } else {
                                        holder.groupName.setTextColor(Color.parseColor("#757575"));
                                        holder.lastMessage.setTextColor(Color.parseColor("#757575"));
                                        holder.groupName.setTypeface(null, Typeface.NORMAL);
                                        holder.lastMessage.setTypeface(null, Typeface.NORMAL);
                                    }
                                }

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public static class viewHolder extends RecyclerView.ViewHolder {

        public CircleImageView image;
        public TextView groupName, lastMessage;
        public ImageView blackCircle;


        public viewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.profile_image);
            groupName = itemView.findViewById(R.id.groupName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            blackCircle = itemView.findViewById(R.id.black_circle);

        }
    }
}
