package geyerk.sensorlab.uractivity;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashMap;

import timber.log.Timber;

class DealWithPermission {

    private HashMap<String, Integer> essentialPermissions;
    private final Context context;
    private final PostAlert postAlert;

    DealWithPermission(Context context, PostAlert postAlert) {
        this.context = context;
        this.postAlert = postAlert;
        this.essentialPermissions = new HashMap<>();

        BroadcastReceiver localReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    final String permissionRequested = bundle.getString("permissionToRequest");
                    if (permissionRequested != null) {
                        if(bundle.getBoolean("positiveResponse")){
                            Timber.i("Camera position now can be requested");
                            String permissionToRequest = bundle.getString("permissionToRequest");
                            if(permissionToRequest!= null){
                                requestNextPermission(permissionToRequest);
                            }
                        }
                    }
                }
                Timber.i("message was received");
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(localReceiver, new IntentFilter("alertDialogPermissionResponse"));

    }

    void determinePermissionThatAreEssential(String[] essentialPermissions){
        for(String essentialPermission: essentialPermissions){
            this.essentialPermissions.put(essentialPermission, CONSTANTS.PERMISSION_NOT_ASSESSED);
        }
        assessApprovalOfPermissions();
        requestNextPermission();
    }

    private void requestNextPermission(){
        Timber.i("request next permission");
        for(String permission: essentialPermissions.keySet()){

            Integer stateOfPermission = essentialPermissions.get(permission);
            Timber.i("Permissions being assessed: %s, state: %d ", permission, stateOfPermission);
            if(stateOfPermission!=null && !permission.equals("NA")) {
                if (stateOfPermission != PackageManager.PERMISSION_GRANTED) {
                    postRationaleForRequestingPermission(permission);
                    return;
                }
            }
        }
        confirmAllPermissionsGranted();
    }

    private void postRationaleForRequestingPermission(String permission){
        switch (permission){
            case Manifest.permission.CAMERA:
                postAlert.customiseMessage(CONSTANTS.ALERT_DIALOG_CAMERA_PERMISSION, permission, "Requesting camera permission", "To calibrate this app with the scientific study which you are participating in, this app will scan a QRcode. In order to do this, it requires you to allow for this app to access the camera. Please allow this permissions in a moment" , "alertDialogPermissionResponse");
                break;
            case "usage":
                postAlert.customiseMessage(CONSTANTS.ALERT_DIALOG_USAGE_PERMISSION, permission, "Requesting usage permission", "The experiment that you are involved in requires you to provide usage permission. Usage permission will allow the app to monitor what you have used your phone for (what apps you've used and when the screen has been on/off up to the last 5 days). This feature allows to see what apps you continue to use as well." , "alertDialogResponse");
                break;
            case "notification":
                postAlert.customiseMessage(CONSTANTS.ALERT_DIALOG_NOTIFICATION_PERMISSION , permission, "Requesting notification permission", "The experiment that you are involved in requires you to provide Notification permission. Notification permission means that the app will know when you have received notifications and when you have deleted it." , "alertDialogResponse");
                break;
        }
    }

    private void assessApprovalOfPermissions() {
        Timber.i("Assessing permission");
        for(String essentialPermission: essentialPermissions.keySet()){
            if(!essentialPermission.equals("usage") &&  !essentialPermission.equals("notification")){
                essentialPermissions.put(essentialPermission, permissionRequired(essentialPermission));

            }else{
                if(essentialPermission.equals("usage")){
                    if(establishStateOfUsageStatisticsPermission()){
                        essentialPermissions.put(essentialPermission, PackageManager.PERMISSION_DENIED);
                        Timber.i("Usage permission required");
                    }else{
                        essentialPermissions.put(essentialPermission, PackageManager.PERMISSION_GRANTED);
                        Timber.i("Usage permission not required");
                    }
                }
                if(essentialPermission.equals("notification")){
                    if(establishStateOfNotificationListenerPermission()){
                        Timber.i("Notification permission required");
                        essentialPermissions.put(essentialPermission, PackageManager.PERMISSION_DENIED);
                    }else{
                        Timber.i("Notification permission not required");
                        essentialPermissions.put(essentialPermission, PackageManager.PERMISSION_GRANTED);
                    }
                }
            }
        }
    }

    private int permissionRequired(String permission) {
        return ContextCompat.checkSelfPermission(context, permission);
    }

    private void requestNextPermission(String permissionToRequest) {
        ActivityCompat.requestPermissions((Activity) context, new String[]{permissionToRequest},1);
    }

    private Boolean establishStateOfUsageStatisticsPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int mode = 2;
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    mode = appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), String.valueOf(context.getPackageName()));
                } else {
                    mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), String.valueOf(context.getPackageName()));
                }
            }
            return (mode != AppOpsManager.MODE_ALLOWED);
        } else {
            return false;
        }
    }

    private boolean establishStateOfNotificationListenerPermission() {
        String notificationListenerString = Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");
        return notificationListenerString != null && !notificationListenerString.contains(context.getPackageName());
    }

    private void confirmAllPermissionsGranted() {
        Timber.i("confirming all permission is granted");
        postAlert.customiseMessage(CONSTANTS.ALL_PERMISSIONS_GRANTED, "NA", "All permissions given", "All essential permissions needed", "alertDialogResponse");
    }
}
