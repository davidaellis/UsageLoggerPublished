package psych.sensorlab.usagelogger2;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import net.sqlcipher.database.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import at.favre.lib.armadillo.Armadillo;
import at.favre.lib.armadillo.BuildConfig;
import timber.log.Timber;

public class LoggerWithNotesService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        try {
            if(sbn!= null && packageManager != null) {
                storeData(packageManager.getApplicationLabel(packageManager.
                        getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA))
                        + " posted note");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        super.onNotificationRemoved(sbn, rankingMap, reason);
        try {
            if(sbn!= null && packageManager != null) {
                storeData(packageManager.getApplicationLabel(packageManager.
                        getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA))
                        + " note removed");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Classes
    private static class LoggingDirection {
        final boolean screenLog, appLog, appChanges;
        LoggingDirection(boolean screenLog, boolean appLog, boolean appChanges){
            this.screenLog = screenLog;
            this.appLog = appLog;
            this.appChanges = appChanges;
        }
    }
    private IdentifyAppInForeground identifyAppInForeground;

    //receivers
    private BroadcastReceiver screenReceiver, appReceiver;

    //Components
    private LoggingDirection loggingDirection;
    private PackageManager packageManager;
    private Handler handler;
    private String currentlyRunningApp;
    private SQLiteDatabase database;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1087384, DeclareInForeground());

        if(intent.getExtras() == null){
            return START_STICKY;
        }

        Bundle bundle  = intent.getExtras();
        try {
            initializeComponents(bundle);
            if(loggingDirection.screenLog || loggingDirection.appLog || loggingDirection.appChanges){
                initializeBroadcastReceivers();
                onListenerConnected();
                if (bundle.getBoolean("restart")) {
                    Handler restartHandler = new Handler();
                    Runnable documentRestart = () -> storeData("Phone restarted");
                    restartHandler.postDelayed(documentRestart, 10*1000);
                    storeData("Phone restarted");
                    Timber.v("--- restarting ---");
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return START_STICKY;
    }

    private Notification DeclareInForeground() {

        final String contentText = "Currently collecting data in background";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel("usage logger", getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null,null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            channel.setShowBadge(true);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder nfc = new NotificationCompat.Builder(getApplicationContext(),
                "usage logger")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_stat_name))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET) //This hides the notification from lock screen
                .setContentTitle(this.getApplication().getPackageName())
                .setContentText("Usage Logger 2 is collecting data")
                .setOngoing(true);

        nfc.setContentTitle(this.getApplication().getPackageName());
        nfc.setContentText(contentText);
        nfc.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText).
                setBigContentTitle(this.getApplication().getPackageName()));
        nfc.setWhen(System.currentTimeMillis());

        return nfc.build();
    }

    private void initializeError() {
        if(BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree(){
                @NonNull
                @Override
                protected String createStackElementTag(@NotNull StackTraceElement element) {
                    return String.format("C:%s:%s",super.createStackElementTag(element),
                            element.getLineNumber());
                }
            });
        }
    }

    private void initializeComponents(Bundle bundle) throws Exception {
        if(bundle==null){
            throw new Exception("Bundle is null");
        }
        SharedPreferences securePreferences = Armadillo.create(this,
                        "service without note")
                .enableKitKatSupport(true)
                .encryptionFingerprint(this)
                .build();
        final String password;

        if (!bundle.getBoolean("restart")){
            password = bundle.getString("password", "not password");
            securePreferences.edit().putString("password", password).apply();
        }else{
            initializeError();
            password = securePreferences.getString("password", "not password");
        }

        if(password.equals("not password")){
            throw new Exception("Could not retrieve password");
        }
        loggingDirection = new LoggingDirection(
                bundle.getBoolean("screenLog"),
                bundle.getBoolean("appLog"),
                bundle.getBoolean("appChanges")
        );

        StoreInSQL storeInSQL = new StoreInSQL(this, "continuous.db",1,
                "continuous_table", "(time INTEGER, event TEXT)");
        SQLiteDatabase.loadLibs(this);
        database = storeInSQL.getWritableDatabase(password);
        handler = new Handler();
        packageManager = this.getPackageManager();
        currentlyRunningApp = this.getPackageName();

        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        identifyAppInForeground = new IdentifyAppInForeground(activityManager);
    }

    /**
     * HANDLING BROADCAST RECEIVERS
     */

    private void initializeBroadcastReceivers() {
        if(loggingDirection.screenLog || loggingDirection.appLog){
            if(loggingDirection.appLog) {
                screenReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction() != null) {
                            switch (intent.getAction()) {
                                case Intent.ACTION_SCREEN_OFF:
                                    storeData("screen off");
                                    handler.removeCallbacks(callIdentifyAppInForegroundOld);
                                    break;
                                case Intent.ACTION_SCREEN_ON:
                                    storeData("screen on");
                                    handler.postDelayed(callIdentifyAppInForegroundOld, 100);
                                    break;
                                case Intent.ACTION_USER_PRESENT:
                                    storeData("user present");
                                    break;
                            }
                        }
                    }
                };
            } else {
                screenReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction() != null) {
                            switch (intent.getAction()) {
                                case Intent.ACTION_SCREEN_OFF:
                                    storeData("screen off");
                                    break;
                                case Intent.ACTION_SCREEN_ON:
                                    storeData("screen on");
                                    break;
                                case Intent.ACTION_USER_PRESENT:
                                    storeData("user present");
                                    break;
                            }
                        }
                    }
                };
            }

            IntentFilter screenReceiverFilter = new IntentFilter();
            screenReceiverFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenReceiverFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenReceiverFilter.addAction(Intent.ACTION_USER_PRESENT);

            registerReceiver(screenReceiver, screenReceiverFilter);
        }

        if(loggingDirection.appChanges) {
            SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
            if(!sharedPreferences.getBoolean("initial app survey conducted", false)){
                sharedPreferences.edit()
                        .putStringSet("installed apps", getInstalledApps())
                        .putBoolean("initial app survey conducted", true)
                        .apply();
            }

            appReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() != null) {
                        switch (intent.getAction()) {
                            case Intent.ACTION_PACKAGE_ADDED:
                                Set<String> oldAppListAdd = sharedPreferences.
                                        getStringSet("installed apps", new HashSet<>());
                                Set<String> newAppListAdd = getInstalledApps();
                                if(newAppListAdd.containsAll(oldAppListAdd)){
                                    Set<String> newApps = identifyNewApp(oldAppListAdd, newAppListAdd);
                                    for(String newApp: newApps){
                                        storeData("installed: "+newApp);
                                    }
                                    sharedPreferences.edit().putStringSet("installed apps",
                                            newAppListAdd).apply();
                                } else {
                                    Timber.e("Issue with package added broadcast receiver");
                                }
                                break;
                            case Intent.ACTION_PACKAGE_REMOVED:
                                Set<String> oldAppListRemoved = sharedPreferences.getStringSet("installed apps", new HashSet<>());
                                Set<String> newAppListRemoved = getInstalledApps();
                                if(oldAppListRemoved.containsAll(newAppListRemoved)){
                                    Set<String> removedApps = identifyNewApp(newAppListRemoved, oldAppListRemoved);
                                    for(String removedApp: removedApps){
                                        storeData("uninstalled: "+removedApp);
                                    }
                                    sharedPreferences.edit().putStringSet("installed apps", newAppListRemoved).apply();
                                }else{
                                    Timber.e("Issue with package added broadcast receiver");
                                }
                                break;
                        }
                    }
                }
            };

            IntentFilter appReceiverFilter = new IntentFilter();
            appReceiverFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
            appReceiverFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            appReceiverFilter.addDataScheme("package");
            registerReceiver(appReceiver, appReceiverFilter);
        }
    }

    private Set<String> getInstalledApps() {
        PackageManager pm = this.getPackageManager();
        //tk 09.06.22 getInstalledPackages no longer works for Android 11 and above
        //extra permissions are required QUERY_ALL_PACKAGES, which need a approval from Google for Play Store listing
        //see https://support.google.com/googleplay/android-developer/answer/10158779?hl=en and
        //https://developer.android.com/training/package-visibility
        @SuppressLint("QueryPermissionsNeeded")
        final List<PackageInfo> appInstall= pm.getInstalledPackages(PackageManager.GET_PERMISSIONS|PackageManager.GET_RECEIVERS|
                PackageManager.GET_SERVICES|PackageManager.GET_PROVIDERS);

        Set<String> installedApps = new HashSet<>();
        for (PackageInfo packageInfo:appInstall){
            installedApps.add((String) packageInfo.applicationInfo.loadLabel(pm));
        }
        return installedApps;
    }

    private Set<String> identifyNewApp(Set<String> shorterAppList, Set<String> largerAppList) {
        Set<String> newApp = new HashSet<>();
        for(String app: largerAppList){
            if(!shorterAppList.contains(app)){
                newApp.add(app);
            }
        }
        return newApp;
    }

    final Runnable callIdentifyAppInForegroundNew = new Runnable() {
        @Override
        public void run() {
         
            String appRunningInForeground = identifyAppInForeground.identifyForegroundTaskLollipop(getApplicationContext());
            if (!appRunningInForeground.equals(currentlyRunningApp) &&
                    !appRunningInForeground.equals("THIS IS NOT A REAL APP")) {
                storeData("App: " + appRunningInForeground);
                currentlyRunningApp = appRunningInForeground;
            }

            handler.postDelayed(callIdentifyAppInForegroundNew, 1000);
        }
    };

    final Runnable callIdentifyAppInForegroundOld = new Runnable() {
        @Override
        public void run() {
            String appRunningInForeground = identifyAppInForeground.identifyForegroundTaskUnderLollipop();
            if (Objects.equals(appRunningInForeground, "UR activity")) {
                Timber.v("App is running in foreground");
            }
            if (!appRunningInForeground.equals(currentlyRunningApp) &&
                    !appRunningInForeground.equals("THIS IS NOT A REAL APP")) {
                storeData("App: " + appRunningInForeground);
                currentlyRunningApp = appRunningInForeground;
            }
            handler.postDelayed(callIdentifyAppInForegroundNew, 1000);
        }
    };

    void storeData(String data){
        ContentValues values = new ContentValues();
        values.put("time", System.currentTimeMillis());
        values.put("event", data);
        Timber.i("notes data: %d - %s", System.currentTimeMillis(), data);
        database.insert("continuous_table",null, values);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (database!=null) database.close();

        if(loggingDirection.screenLog){
            unregisterReceiver(screenReceiver);
        }
        if(loggingDirection.appChanges){
            unregisterReceiver(appReceiver);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            onListenerDisconnected();
        }
    }
}
