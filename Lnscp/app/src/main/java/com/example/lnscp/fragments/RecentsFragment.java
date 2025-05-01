package com.example.lnscp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lnscp.R;
import com.example.lnscp.database.Monument;
import com.example.lnscp.database.MonumentDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class RecentsFragment extends Fragment {
    private MonumentDatabaseHelper dbHelper;
    private ListView monumentsList;
    private MonumentAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new MonumentDatabaseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recents, container, false);
        monumentsList = view.findViewById(R.id.monuments_list);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadMonuments();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMonuments();
    }

    private void loadMonuments() {
        try {
            List<Monument> monuments = dbHelper.getAllMonuments();
            if (monuments.isEmpty()) {
                Toast.makeText(requireContext(), "No monuments found in database", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Found " + monuments.size() + " monuments", Toast.LENGTH_SHORT).show();
            }
            
            if (adapter == null) {
                adapter = new MonumentAdapter(monuments);
                monumentsList.setAdapter(adapter);
            } else {
                adapter.updateMonuments(monuments);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error loading monuments: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private class MonumentAdapter extends BaseAdapter {
        private List<Monument> monuments;

        public MonumentAdapter(List<Monument> monuments) {
            this.monuments = monuments != null ? monuments : new ArrayList<>();
        }

        public void updateMonuments(List<Monument> newMonuments) {
            this.monuments = newMonuments != null ? newMonuments : new ArrayList<>();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return monuments.size();
        }

        @Override
        public Object getItem(int position) {
            return monuments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return monuments.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_monument, parent, false);
                }

                Monument monument = monuments.get(position);
                ImageView imageView = convertView.findViewById(R.id.monument_image);
                TextView nameView = convertView.findViewById(R.id.monument_name);
                ImageButton bookmarkButton = convertView.findViewById(R.id.bookmark_button);

                if (monument.getImage() != null) {
                    imageView.setImageBitmap(monument.getImage());
                }
                nameView.setText(monument.getName());
                bookmarkButton.setImageResource(monument.isBookmarked() ? 
                    R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_border);

                bookmarkButton.setOnClickListener(v -> {
                    try {
                        dbHelper.toggleBookmark(monument.getId());
                        monument.setBookmarked(!monument.isBookmarked());
                        notifyDataSetChanged();
                        Toast.makeText(requireContext(), 
                            monument.isBookmarked() ? "Added to bookmarks" : "Removed from bookmarks", 
                            Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), 
                            "Error updating bookmark: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });

                return convertView;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(parent.getContext(), 
                    "Error displaying monument: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                return new View(parent.getContext());
            }
        }
    }
} 