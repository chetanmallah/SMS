package com.example.defaultsmsapp;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {
    private Context context;
    private List<SmsModel> smsList;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;

    public SmsAdapter(Context context, List<SmsModel> smsList) {
        this.context = context;
        this.smsList = smsList;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sms, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SmsModel sms = smsList.get(position);
        
        // Set contact name or phone number
        holder.textViewSender.setText(sms.getDisplayName());
        
        // Set message body
        holder.textViewMessage.setText(sms.getBody());
        
        // Set timestamp
        holder.textViewTime.setText(getFormattedTime(sms.getDate()));
        
        // Set read/unread styling
        if (sms.isRead()) {
            holder.textViewSender.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.textViewMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.textViewTime.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            holder.textViewSender.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.textViewMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.textViewTime.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        
        // Set message type indicator (sent/received)
        if (sms.isOutgoing()) {
            holder.textViewSender.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
            holder.textViewMessage.setText("You: " + sms.getBody());
        } else {
            holder.textViewSender.setTextColor(context.getResources().getColor(android.R.color.black));
        }
        
        // Handle item click - open compose activity with this contact
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ComposeActivity.class);
            intent.putExtra("address", sms.getAddress());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }
    
    private String getFormattedTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        // If message is from today, show time
        if (DateUtils.isToday(timestamp)) {
            return timeFormat.format(new Date(timestamp));
        }
        // If message is from this week, show day name
        else if (diff < 7 * 24 * 60 * 60 * 1000) {
            return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(timestamp));
        }
        // Otherwise show date
        else {
            return dateFormat.format(new Date(timestamp));
        }
    }
    
    public void updateSms(List<SmsModel> newSmsList) {
        this.smsList.clear();
        this.smsList.addAll(newSmsList);
        notifyDataSetChanged();
    }

    public static class SmsViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSender;
        TextView textViewMessage;
        TextView textViewTime;

        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSender = itemView.findViewById(R.id.textViewSender);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewTime = itemView.findViewById(R.id.textViewTime);
        }
    }
}