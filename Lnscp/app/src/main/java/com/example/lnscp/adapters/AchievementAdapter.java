package com.example.lnscp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lnscp.R;
import com.example.lnscp.models.Achievement;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {
    private List<Achievement> achievements;
    private Context context;

    public AchievementAdapter(Context context, List<Achievement> achievements) {
        this.context = context;
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);
        
        holder.titleText.setText(achievement.getTitle());
        holder.descriptionText.setText(achievement.getDescription());
        holder.progressText.setText(context.getString(R.string.achievement_progress_format, 
            achievement.getCurrentProgress(), achievement.getTargetProgress()));
        
        // Set achievement icon
        int iconResId = context.getResources().getIdentifier(
            achievement.getIconResId(), "drawable", context.getPackageName());
        holder.achievementIcon.setImageResource(iconResId);
        
        // Set status icon based on achievement state
        holder.statusIcon.setImageResource(achievement.isUnlocked() ? 
            R.drawable.ic_check_circle : R.drawable.ic_lock);
        
        // Set content descriptions for accessibility
        holder.achievementIcon.setContentDescription(context.getString(R.string.achievement_icon));
        holder.statusIcon.setContentDescription(achievement.isUnlocked() ? 
            context.getString(R.string.achievement_completed) : 
            context.getString(R.string.achievement_locked));
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    public void updateAchievements(List<Achievement> newAchievements) {
        this.achievements = newAchievements;
        notifyDataSetChanged();
    }

    static class AchievementViewHolder extends RecyclerView.ViewHolder {
        ImageView achievementIcon;
        TextView titleText;
        TextView descriptionText;
        TextView progressText;
        ImageView statusIcon;

        AchievementViewHolder(View itemView) {
            super(itemView);
            achievementIcon = itemView.findViewById(R.id.achievement_icon);
            titleText = itemView.findViewById(R.id.achievement_title);
            descriptionText = itemView.findViewById(R.id.achievement_description);
            progressText = itemView.findViewById(R.id.achievement_progress);
            statusIcon = itemView.findViewById(R.id.achievement_status);
        }
    }
} 