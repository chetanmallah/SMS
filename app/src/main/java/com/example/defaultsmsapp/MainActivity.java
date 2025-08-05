package com.example.defaultsmsapp;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int DEFAULT_SMS_REQUEST_CODE = 101;
    
    private RecyclerView recyclerView;
    private SmsAdapter smsAdapter;
    private List<SmsModel> smsList;
    private FloatingActionButton fabCompose;
    
    private BroadcastReceiver smsRefreshReceiver;
    
    private final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupSmsRefreshReceiver();
        checkPermissionsAndSetupApp();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewSms);
        fabCompose = findViewById(R.id.fabCompose);
        
        smsList = new ArrayList<>();
        smsAdapter = new SmsAdapter(this, smsList);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(smsAdapter);
        
        fabCompose.setOnClickListener(v -> {
            Intent intent = new Intent(this, ComposeActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupSmsRefreshReceiver() {
        smsRefreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "SMS refresh broadcast received");
                loadSmsMessages();
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.defaultsmsapp.SMS_RECEIVED");
        filter.addAction("com.example.defaultsmsapp.MMS_RECEIVED");
        registerReceiver(smsRefreshReceiver, filter);
    }
    
    private void checkPermissionsAndSetupApp() {
        if (!hasAllPermissions()) {
            requestPermissions();
        } else {
            checkDefaultSmsApp();
        }
    }
    
    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                checkDefaultSmsApp();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }
    
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs SMS and Contacts permissions to function properly.")
            .setPositiveButton("Grant", (dialog, which) -> requestPermissions())
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }
    
    private void checkDefaultSmsApp() {
        if (!isDefaultSmsApp()) {
            showSetDefaultSmsDialog();
        } else {
            loadSmsMessages();
        }
    }
    
    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }
    
    private void showSetDefaultSmsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Set as Default SMS App")
            .setMessage("To receive and send SMS messages, this app needs to be set as your default SMS app.")
            .setPositiveButton("Set Default", (dialog, which) -> requestDefaultSmsApp())
            .setNegativeButton("Cancel", (dialog, which) -> {
                Toast.makeText(this, "App requires default SMS permission to function", Toast.LENGTH_LONG).show();
                loadSmsMessages(); // Still allow reading existing messages
            })
            .setCancelable(false)
            .show();
    }
    
    private void requestDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                startActivityForResult(roleRequestIntent, DEFAULT_SMS_REQUEST_CODE);
            }
        } else {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivityForResult(intent, DEFAULT_SMS_REQUEST_CODE);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == DEFAULT_SMS_REQUEST_CODE) {
            if (isDefaultSmsApp()) {
                Toast.makeText(this, "App is now the default SMS app", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "App was not set as default SMS app", Toast.LENGTH_SHORT).show();
            }
            loadSmsMessages();
        }
    }
    
    private void loadSmsMessages() {
        smsList.clear();
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Uri smsUri = Uri.parse("content://sms/");
        String[] projection = {
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        };
        
        try (Cursor cursor = getContentResolver().query(
            smsUri, 
            projection, 
            null, 
            null, 
            Telephony.Sms.DATE + " DESC"
        )) {
            
            if (cursor != null && cursor.moveToFirst()) {
                Map<String, List<SmsModel>> conversations = new HashMap<>();
                
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));
                    boolean isRead = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1;
                    
                    String contactName = getContactName(address);
                    
                    SmsModel sms = new SmsModel(id, address, body, date, type, isRead, contactName);
                    
                    // Group by address for conversations
                    conversations.computeIfAbsent(address, k -> new ArrayList<>()).add(sms);
                    
                } while (cursor.moveToNext());
                
                // Add the latest message from each conversation
                for (List<SmsModel> conversation : conversations.values()) {
                    if (!conversation.isEmpty()) {
                        // Sort by date and take the latest
                        Collections.sort(conversation, (a, b) -> Long.compare(b.getDate(), a.getDate()));
                        smsList.add(conversation.get(0));
                    }
                }
                
                // Sort all conversations by latest message date
                Collections.sort(smsList, (a, b) -> Long.compare(b.getDate(), a.getDate()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading SMS messages", e);
            Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
        }
        
        smsAdapter.notifyDataSetChanged();
        Log.d(TAG, "Loaded " + smsList.size() + " SMS conversations");
    }
    
    private String getContactName(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return phoneNumber;
        }
        
        Uri uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
            Uri.encode(phoneNumber));
        String[] projection = {android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME};
        
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name", e);
        }
        
        return phoneNumber;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (hasAllPermissions()) {
            loadSmsMessages();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (smsRefreshReceiver != null) {
            unregisterReceiver(smsRefreshReceiver);
        }
    }
    
    public void refreshMessages() {
        loadSmsMessages();
    }
}