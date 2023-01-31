package psych.sensorlab.usagelogger2;

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

    private final HashMap<String, Integer> essentialPermissions;
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
                    Timber.v("permissions requested: %s", permissionRequested);
                    if (permissionRequested != null) {
                        if (bundle.getBoolean("positiveResponse")) {
                            Timber.i("Camera position can now be requested");
                            String permissionToRequest = bundle.getString("permissionToRequest");
                            if (permissionToRequest != null) {
                                requestNextPermission(permissionToRequest);
                            }
                        }
                    }
                }
                Timber.i("message was received");
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(localReceiver,
                new IntentFilter("alertDialogPermissionResponse"));
    }

    void determineEssentialPerms(String[] essentialPermissions){
        for (String essentialPermission: essentialPermissions) {
            this.essentialPermissions.put(essentialPermission, CONSTANTS.PERMISSION_NOT_ASSESSED);
        }
        assessApprovalOfPermissions();
        requestNextPermission();
    }

    private void requestNextPermission(){
        Timber.i("request next permission");
        for (String permission: essentialPermissions.keySet()) {
            Integer stateOfPermission = essentialPermissions.get(permission);
            Timber.i("Next permissions being assessed: %s, state: %d ", permission, stateOfPermission);
            if(stateOfPermission != null && !permission.equals("NA")) {
                if (stateOfPermission != PackageManager.PERMISSION_GRANTED) {
                    postRationaleForRequestingPermission(permission, context);
                    return;
                }
            }
        }

        //only ask for notification permissions in Android 13 and above
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    confirmAllPermissionsGranted(context);
            } else {
                postAlert.customiseMessage(123, Manifest.permission.POST_NOTIFICATIONS,
                        context.getString(R.string.title_show_note_perm),
                        context.getString(R.string.show_note_perm),
                        "alertDialogPermissionResponse");
            }
        } else {
                confirmAllPermissionsGranted(context);
        }
    }

    private void postRationaleForRequestingPermission(String permission, Context context){
        switch (permission) {
            case Manifest.permission.CAMERA:
                postAlert.customiseMessage(CONSTANTS.ALERT_DIALOG_CAMERA_PERMISSION, permission,
                        context.getString(R.string.title_cam_perm), context.getString(R.string.cam_perm),
                        "alertDialogPermissionResponse");
                break;
            case "usage":
                postAlert.customiseMessage(CONSTANTS.ALERT_DIALOG_USAGE_PERMISSION, permission,
                        context.getString(R.string.title_usage_perm), context.getString(R.string.usage_perm_info),
                        "alertDialogResponse");
                break;
            case "notification":
                postAlert.customiseMessage(CONSTANTS.ALERT_DIALOG_NOTIFICATION_PERMISSION, permission,
                        context.getString(R.string.title_req_note_perm), context.getString(R.string.note_perm_info),
                        "alertDialogResponse");
                break;
        }
    }

    private void assessApprovalOfPermissions() {
        Timber.i("Assessing permission%s", essentialPermissions.keySet());
        for (String essentialPermission: essentialPermissions.keySet()) {
            if (!essentialPermission.equals("usage")
                    && !essentialPermission.equals("notification")) {
                essentialPermissions.put(essentialPermission, permissionRequired(essentialPermission));
            } else {
                if (essentialPermission.equals("usage")) {
                    if (establishStateOfUsageStatisticsPermission()) {
                        essentialPermissions.put(essentialPermission, PackageManager.PERMISSION_DENIED);
                        Timber.i("essential: usage permission required");
                    } else {
                        essentialPermissions.put(essentialPermission, PackageManager.PERMISSION_GRANTED);
                        Timber.i("essential: usage permission not required");
                    }
                }
                if (essentialPermission.equals("notification")) {
                    if(establishStateOfNotificationListenerPermission()) {
                        Timber.i("essential: notification permission required");
                        essentialPermissions.put(essentialPermission, PackageManager.PERMISSION_DENIED);
                    } else {
                        Timber.i("essential: notification permission not required");
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
        ActivityCompat.requestPermissions((Activity) context,
                new String[]{permissionToRequest},1);
    }

    private Boolean establishStateOfUsageStatisticsPermission() {
        int mode = 2;
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOpsManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mode = appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), String.valueOf(context.getPackageName()));
            } else {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), String.valueOf(context.getPackageName()));
            }
        }
        return (mode != AppOpsManager.MODE_ALLOWED);
    }

    private boolean establishStateOfNotificationListenerPermission() {
        String notificationListenerString = Settings.Secure.getString(
                context.getContentResolver(),"enabled_notification_listeners");
        return notificationListenerString != null && !notificationListenerString.contains(
                context.getPackageName());
    }

    private void confirmAllPermissionsGranted(Context context) {
            postAlert.customiseMessage(CONSTANTS.ALL_PERMISSIONS_GRANTED, "NA",
                    context.getString(R.string.title_setup_fin), context.getString(R.string.all_perms_given),
                    "alertDialogResponse");
    }
}
