package com.example.defaultsmsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS received, action: " + intent.getAction());
        
        if (intent.getAction() == null) {
            return;
        }
        
        switch (intent.getAction()) {
            case Telephony.Sms.Intents.SMS_RECEIVED_ACTION:
            case Telephony.Sms.Intents.SMS_DELIVER_ACTION:
                handleSmsReceived(context, intent);
                break;
            default:
                Log.d(TAG, "Unknown action: " + intent.getAction());
                break;
        }
    }
    
    private void handleSmsReceived(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.e(TAG, "No extras in SMS intent");
            return;
        }
        
        try {
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            
            if (messages == null || messages.length == 0) {
                Log.e(TAG, "No SMS messages found in intent");
                return;
            }
            
            for (SmsMessage smsMessage : messages) {
                if (smsMessage == null) {
                    continue;
                }
                
                String sender = smsMessage.getDisplayOriginatingAddress();
                String messageBody = smsMessage.getMessageBody();
                long timestamp = smsMessage.getTimestampMillis();
                
                Log.d(TAG, "SMS received from: " + sender + ", message: " + messageBody);
                
                // Store the message in the system SMS database
                // This is handled automatically by the system for default SMS apps
                
                // Notify the main activity to refresh the message list
                Intent refreshIntent = new Intent("com.example.defaultsmsapp.SMS_RECEIVED");
                refreshIntent.putExtra("sender", sender);
                refreshIntent.putExtra("message", messageBody);
                refreshIntent.putExtra("timestamp", timestamp);
                context.sendBroadcast(refreshIntent);
                
                // Show notification (you can customize this)
                showNotification(context, sender, messageBody);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing received SMS", e);
        }
    }
    
    private void showNotification(Context context, String sender, String message) {
        // Create a simple notification
        android.app.NotificationManager notificationManager = 
            (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager == null) {
            return;
        }
        
        // Create notification channel for Android O and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "sms_channel";
            String channelName = "SMS Messages";
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                channelId, channelName, android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for SMS messages");
            notificationManager.createNotificationChannel(channel);
        }
        
        // Create intent to open main activity when notification is tapped
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, notificationIntent, 
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M ? 
                android.app.PendingIntent.FLAG_IMMUTABLE : android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Build notification
        androidx.core.app.NotificationCompat.Builder builder = 
            new androidx.core.app.NotificationCompat.Builder(context, "sms_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("New SMS from " + sender)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Show notification
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        
        Log.d(TAG, "Notification shown for SMS from: " + sender);
    }
}