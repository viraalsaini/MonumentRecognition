package com.example.lnscp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.lnscp.ui.home.HomeFragment;
import com.example.lnscp.fragments.ProfileFragment;
import com.example.lnscp.fragments.RecentsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private static final int TAB_COUNT = 3;

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new RecentsFragment();
            case 2:
                return new ProfileFragment();
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
} 