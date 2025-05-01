package com.example.lnscp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lnscp.R;
import com.example.lnscp.models.Monument;

import java.util.List;

public class RecentsAdapter extends RecyclerView.Adapter<RecentsAdapter.RecentViewHolder> {
    private List<Monument> monuments;

    public RecentsAdapter(List<Monument> monuments) {
        this.monuments = monuments;
    }

    @NonNull
    @Override
    public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_monument, parent, false);
        return new RecentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
        Monument monument = monuments.get(position);
        holder.tvMonumentName.setText(monument.getName());
        holder.tvMonumentLocation.setText(monument.getLocation());
        // TODO: Load image using Glide or Picasso
    }

    @Override
    public int getItemCount() {
        return monuments.size();
    }

    static class RecentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivMonumentImage;
        TextView tvMonumentName;
        TextView tvMonumentLocation;

        RecentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMonumentImage = itemView.findViewById(R.id.ivMonumentImage);
            tvMonumentName = itemView.findViewById(R.id.tvMonumentName);
            tvMonumentLocation = itemView.findViewById(R.id.tvMonumentLocation);
        }
    }
} 