package com.example.defaultsmsapp;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.telephony.TelephonyManager;


public class HeadlessSmsSendService extends Service {
    private static final String TAG = "HeadlessSmsSendService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "HeadlessSmsSendService started");

        if (intent != null && TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(intent.getAction()))
        {
            handleRespondViaMessage(intent);
        }

        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void handleRespondViaMessage(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            Log.e(TAG, "No URI in respond via message intent");
            return;
        }

        String phoneNumber = uri.getSchemeSpecificPart();
        String message = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(message)) {
            Log.e(TAG, "Missing phone number or message in respond via message intent");
            return;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d(TAG, "Quick response SMS sent to: " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error sending quick response SMS", e);
        }
    }
}
