package psych.sensorlab.usagelogger2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.favre.lib.armadillo.Armadillo;
import timber.log.Timber;

public class LoggerService extends Service {

    //Todo - Remove redundancy between LoggerServices
    private IdentifyAppInForeground identifyAppInForeground;
    private BroadcastReceiver screenReceiver, appReceiver;
    private LoggingDirection loggingDirection;
    private Handler handler;
    private String currentlyRunningApp;
    private SQLiteDatabase database;

    //Classes
    private static class LoggingDirection {
        final boolean screenLog, appChanges;
        LoggingDirection(boolean screenLog, boolean appChanges){
            this.screenLog = screenLog;
            this.appChanges = appChanges;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(1087384, DeclareInForeground());
        Bundle bundle  = intent.getExtras();

        try {
            initializeComponents(bundle);
            initializeBroadcastReceivers();
            if (bundle.getBoolean("restart")) {
                Handler restartHandler = new Handler();
                Runnable documentRestart = () -> storeData("Phone restarted");
                restartHandler.postDelayed(documentRestart, 10 * CONSTANTS.LOGGING_INTERVAL_MS);
                storeData("Phone restarted");
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return START_STICKY;
    }

    private Notification DeclareInForeground() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("ul2",
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null,null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            channel.setShowBadge(true);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder nfc = new NotificationCompat.Builder(
                getApplicationContext(),"ul2")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setWhen(System.currentTimeMillis());
        return nfc.build();
    }

    private void initializeComponents(Bundle bundle) throws Exception {

        final String password;
        StoreInSQL storeInSQL;

        SharedPreferences securePreferences = Armadillo.create(this, "service_no_note")
                .enableKitKatSupport(true).encryptionFingerprint(this).build();

        if (bundle == null) {
            throw new Exception("Bundle is null");
        }

        if (!bundle.getBoolean("restart")) {
            password = bundle.getString("password", "no_pwd");
            securePreferences.edit().putString("password", password).apply();
        } else {
            password = securePreferences.getString("password", "no_pwd");
        }

        loggingDirection = new LoggingDirection(
                bundle.getBoolean("screenLog"),
                bundle.getBoolean("appChanges")
        );

        if (password == null || password.isEmpty() || password.equals("no_pwd")) {
            throw new Exception("Could not retrieve password");
        }

       storeInSQL = new StoreInSQL(this, CONSTANTS.CONTINUOUS_DB_TABLE, CONSTANTS.DB_VERSION,
                CONSTANTS.CONTINUOUS_DB_TABLE);
       SQLiteDatabase.loadLibs(this);

       database = storeInSQL.getWritableDatabase(password);
       handler = new Handler();
       currentlyRunningApp = getPackageName();

       identifyAppInForeground = new IdentifyAppInForeground();
    }

    /**
     * HANDLING BROADCAST RECEIVERS
     */
    private void initializeBroadcastReceivers() {

        if (loggingDirection.screenLog) {
            screenReceiver = new BroadcastReceiver() {
             @Override
             public void onReceive(Context context, Intent intent) {
                 if (intent.getAction() != null) {
                     String action = intent.getAction();
                     if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                            storeData("screen off");
                        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                            storeData("screen on");
                        }
                    }
                }
            };

            IntentFilter screenReceiverFilter = new IntentFilter();
            screenReceiverFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenReceiverFilter.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(screenReceiver, screenReceiverFilter);
        }

        if (loggingDirection.appChanges) {
            SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
            if (!sharedPreferences.getBoolean("initial_app_scan_done", false)) {
                sharedPreferences.edit().putStringSet("installed_apps", getInstalledApps())
                    .putBoolean("initial_app_scan_done", true).apply();
            }

            appReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null) {
                    switch (intent.getAction()) {
                        case Intent.ACTION_PACKAGE_ADDED:
                            Set<String> oldAppListAdd = sharedPreferences.
                                    getStringSet("installed_apps", new HashSet<>());
                            Set<String> newAppListAdd = getInstalledApps();
                            if (newAppListAdd.containsAll(oldAppListAdd)) {
                                Set<String> newApps = identifyNewApp(oldAppListAdd, newAppListAdd);
                                for(String newApp: newApps){
                                    storeData("installed: " + newApp);
                                }
                                sharedPreferences.edit().putStringSet("installed_apps",
                                        newAppListAdd).apply();
                            } else {
                                Timber.e("Issue with package added broadcast receiver");
                            }
                            break;
                        case Intent.ACTION_PACKAGE_REMOVED:
                            Set<String> oldAppListRemoved = sharedPreferences.
                                    getStringSet("installed_apps", new HashSet<>());
                            Set<String> newAppListRemoved = getInstalledApps();
                            if (oldAppListRemoved.containsAll(newAppListRemoved)) {
                                Set<String> removedApps = identifyNewApp(newAppListRemoved, oldAppListRemoved);
                                for(String removedApp: removedApps){
                                    storeData("uninstalled: " + removedApp);
                                }
                                sharedPreferences.edit().putStringSet("installed_apps",
                                        newAppListRemoved).apply();
                            } else {
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
        PackageManager pm = getPackageManager();
        //getInstalledPackages no longer works for Android 11 and above. Extra permissions are
        //required (QUERY_ALL_PACKAGES), which need approval from Google for Play Store listing
        //see https://support.google.com/googleplay/android-developer/answer/10158779?hl=en and
        //https://developer.android.com/training/package-visibility
        @SuppressLint("QueryPermissionsNeeded")
        final List<PackageInfo> appInstall = pm.getInstalledPackages(
                PackageManager.GET_PERMISSIONS | PackageManager.GET_RECEIVERS |
                        PackageManager.GET_SERVICES|PackageManager.GET_PROVIDERS);

        Set<String> installedApps = new HashSet<>();
        for (PackageInfo packageInfo:appInstall){
            installedApps.add((String) packageInfo.applicationInfo.loadLabel(pm));
        }
        return installedApps;
    }

    private Set<String> identifyNewApp(Set<String> shorterAppList, Set<String> largerAppList) {
        Set<String> newApp = new HashSet<>();
        for (String app: largerAppList) {
            if(!shorterAppList.contains(app)){
                newApp.add(app);
            }
        }
        return newApp;
    }

    final Runnable callIdentifyAppInForeground = new Runnable() {
        @Override
        public void run() {
            String appRunningInForeground = identifyAppInForeground.
                    identifyForegroundTask(getApplicationContext());
            if (!appRunningInForeground.equals(currentlyRunningApp) &&
                    !appRunningInForeground.equals("not_real_app")) {
                storeData("App: " + appRunningInForeground);
                currentlyRunningApp = appRunningInForeground;
                Timber.i("App running in foreground: %s", appRunningInForeground);
            }

            handler.postDelayed(callIdentifyAppInForeground, CONSTANTS.LOGGING_INTERVAL_MS);
        }
    };

    void storeData(String data){
        ContentValues values = new ContentValues();
        values.put("time", System.currentTimeMillis());
        values.put("event", data);
        Timber.i("data: %d - %s", System.currentTimeMillis(), data);
        if (database.isOpen()) {
            database.insert(CONSTANTS.CONTINUOUS_DB_TABLE, null, values);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (database!=null) database.close();
        if (loggingDirection.screenLog) {
            unregisterReceiver(screenReceiver);
        }
        if (loggingDirection.appChanges) {
            unregisterReceiver(appReceiver);
        }
    }
}
