package com.example.camera_lab.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.camera_lab.R;

import java.util.ArrayList;

public class ExifAdapter extends RecyclerView.Adapter<ExifAdapter.ViewHolder>{

    private ArrayList<String> items;

    public ExifAdapter(@NonNull ArrayList<String> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.listItem.setText(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView listItem;

        ViewHolder(View itemView) {
            super(itemView);
            listItem = itemView.findViewById(R.id.list_item);
        }
    }
}
