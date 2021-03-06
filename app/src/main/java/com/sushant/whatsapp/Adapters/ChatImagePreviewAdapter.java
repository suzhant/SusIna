package com.sushant.whatsapp.Adapters;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sushant.whatsapp.FullScreenImage;
import com.sushant.whatsapp.Models.Messages;
import com.sushant.whatsapp.R;

import java.util.ArrayList;

public class ChatImagePreviewAdapter extends RecyclerView.Adapter<ChatImagePreviewAdapter.viewHolder> {

    ArrayList<Messages> listImages;
    Context context;
    String recId;

    public ChatImagePreviewAdapter(ArrayList<Messages> listImages, Context context, String recId) {
        this.listImages = listImages;
        this.context = context;
        this.recId = recId;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_images, parent, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        Messages image = listImages.get(position);
        Glide.with(context).load(image.getImageUrl())
                .placeholder(R.drawable.placeholder)
                .dontTransform()
                .into(holder.image);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Pair[] pairs = new Pair[1];
                pairs[0] = new Pair(holder.image, "img");
                Intent intent = new Intent(context, FullScreenImage.class);
                intent.putExtra("UserId", recId);
                intent.putExtra("messageImage", image.getImageUrl());
                ActivityOptions options = ActivityOptions
                        .makeSceneTransitionAnimation((Activity) context, pairs);
                context.startActivity(intent, options.toBundle());
            }
        });

    }

    @Override
    public int getItemCount() {
        return listImages.size();
    }

    public static class viewHolder extends RecyclerView.ViewHolder {

        private final ImageView image;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgChatPreview);
        }
    }
}
