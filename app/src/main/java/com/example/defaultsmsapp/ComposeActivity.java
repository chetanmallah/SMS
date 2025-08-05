package com.example.defaultsmsapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class ComposeActivity extends AppCompatActivity {
    private static final String TAG = "ComposeActivity";
    private static final String SMS_SENT = "SMS_SENT";
    private static final String SMS_DELIVERED = "SMS_DELIVERED";
    
    private EditText editTextPhone;
    private EditText editTextMessage;
    private Button buttonSend;
    private Spinner spinnerSimCard;
    private TextView textViewCharCount;
    
    private List<SubscriptionInfo> subscriptionInfoList;
    private BroadcastReceiver sentReceiver;
    private BroadcastReceiver deliveredReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Compose Message");
        }
        
        initViews();
        setupSimCardSpinner();
        setupBroadcastReceivers();
        setupCharacterCounter();
        handleIncomingIntent();
    }
    
    private void initViews() {
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        spinnerSimCard = findViewById(R.id.spinnerSimCard);
        textViewCharCount = findViewById(R.id.textViewCharCount);
        
        buttonSend.setOnClickListener(v -> sendSms());
    }
    
    private void setupCharacterCounter() {
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                int parts = (int) Math.ceil(length / 160.0);
                if (parts == 0) parts = 1;
                
                String countText = length + "/160";
                if (parts > 1) {
                    countText += " (" + parts + " parts)";
                }
                textViewCharCount.setText(countText);
                
                // Change color if approaching limit
                if (length > 140) {
                    textViewCharCount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else if (length > 120) {
                    textViewCharCount.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                } else {
                    textViewCharCount.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupSimCardSpinner() {
        subscriptionInfoList = new ArrayList<>();
        List<String> simDisplayNames = new ArrayList<>();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(this);
            List<SubscriptionInfo> activeSubscriptions = subscriptionManager.getActiveSubscriptionInfoList();
            
            if (activeSubscriptions != null && !activeSubscriptions.isEmpty()) {
                for (SubscriptionInfo info : activeSubscriptions) {
                    subscriptionInfoList.add(info);
                    String displayName = "SIM " + (info.getSimSlotIndex() + 1);
                    if (!TextUtils.isEmpty(info.getDisplayName())) {
                        displayName += " (" + info.getDisplayName() + ")";
                    }
                    simDisplayNames.add(displayName);
                }
            }
        }
        
        if (simDisplayNames.isEmpty()) {
            simDisplayNames.add("Default SIM");
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, simDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSimCard.setAdapter(adapter);
        
        // Hide spinner if only one SIM
        if (simDisplayNames.size() <= 1) {
            spinnerSimCard.setVisibility(android.view.View.GONE);
            findViewById(R.id.textViewSimLabel).setVisibility(android.view.View.GONE);
        }
    }
    
    private void setupBroadcastReceivers() {
        sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case android.app.Activity.RESULT_OK:
                        Toast.makeText(ComposeActivity.this, "SMS sent successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "SMS sent successfully");
                        // Clear the message field after successful send
                        editTextMessage.setText("");
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(ComposeActivity.this, "SMS sending failed - Generic failure", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "SMS sending failed - Generic failure");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(ComposeActivity.this, "SMS sending failed - No service", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "SMS sending failed - No service");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(ComposeActivity.this, "SMS sending failed - Null PDU", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "SMS sending failed - Null PDU");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(ComposeActivity.this, "SMS sending failed - Radio off", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "SMS sending failed - Radio off");
                        break;
                }
            }
        };
        
        deliveredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case android.app.Activity.RESULT_OK:
                        Toast.makeText(ComposeActivity.this, "SMS delivered", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "SMS delivered");
                        break;
                    case android.app.Activity.RESULT_CANCELED:
                        Toast.makeText(ComposeActivity.this, "SMS not delivered", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "SMS not delivered");
                        break;
                }
            }
        };
        
        registerReceiver(sentReceiver, new IntentFilter(SMS_SENT));
        registerReceiver(deliveredReceiver, new IntentFilter(SMS_DELIVERED));
    }
    
    private void handleIncomingIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SENDTO.equals(action)) {
                String phoneNumber = null;
                String message = null;
                
                if (intent.getData() != null) {
                    phoneNumber = intent.getData().getSchemeSpecificPart();
                }
                
                if (intent.hasExtra("address")) {
                    phoneNumber = intent.getStringExtra("address");
                }
                
                if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                    message = intent.getStringExtra(Intent.EXTRA_TEXT);
                }
                
                if (!TextUtils.isEmpty(phoneNumber)) {
                    editTextPhone.setText(phoneNumber);
                }
                
                if (!TextUtils.isEmpty(message)) {
                    editTextMessage.setText(message);
                }
            }
        }
    }
    
    private void sendSms() {
        String phoneNumber = editTextPhone.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();
        
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
            editTextPhone.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            editTextMessage.requestFocus();
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED), 
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
            
            SmsManager smsManager;
            
            // Use specific SIM if dual SIM is available
            if (!subscriptionInfoList.isEmpty() && spinnerSimCard.getSelectedItemPosition() < subscriptionInfoList.size()) {
                int subscriptionId = subscriptionInfoList.get(spinnerSimCard.getSelectedItemPosition()).getSubscriptionId();
                smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
                Log.d(TAG, "Using SIM subscription ID: " + subscriptionId);
            } else {
                smsManager = SmsManager.getDefault();
                Log.d(TAG, "Using default SIM");
            }
            
            // Check if message needs to be divided into multiple parts
            ArrayList<String> messageParts = smsManager.divideMessage(message);
            
            if (messageParts.size() == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
            } else {
                ArrayList<PendingIntent> sentPIs = new ArrayList<>();
                ArrayList<PendingIntent> deliveredPIs = new ArrayList<>();
                
                for (int i = 0; i < messageParts.size(); i++) {
                    sentPIs.add(sentPI);
                    deliveredPIs.add(deliveredPI);
                }
                
                smsManager.sendMultipartTextMessage(phoneNumber, null, messageParts, sentPIs, deliveredPIs);
            }
            
            Log.d(TAG, "SMS sending initiated to: " + phoneNumber);
            
            // Disable send button temporarily to prevent multiple sends
            buttonSend.setEnabled(false);
            buttonSend.postDelayed(() -> buttonSend.setEnabled(true), 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS", e);
            Toast.makeText(this, "Error sending SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sentReceiver != null) {
            unregisterReceiver(sentReceiver);
        }
        if (deliveredReceiver != null) {
            unregisterReceiver(deliveredReceiver);
        }
    }
}