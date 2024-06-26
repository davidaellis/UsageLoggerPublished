package psych.sensorlab.usagelogger2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.favre.lib.armadillo.Armadillo;
import timber.log.Timber;

public class Logger extends NotificationListenerService {

    private static final String TAG = NotificationListenerService.class.getSimpleName();
    private BroadcastReceiver screenReceiver, appReceiver;
    private IdentifyAppInForeground identifyAppInForeground;
    private LoggingDirection loggingDirection;
    private PackageManager packageManager;
    private Handler handler;
    private String currentlyRunningApp;
    private SQLiteDatabase database;
    int serviceType;
    boolean restart;
    String notificationText;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        try {
            if (sbn != null && packageManager != null) {
                storeData(packageManager.getApplicationLabel(packageManager.
                        getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA))
                        + " notification posted");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(TAG, "Couldn't find package name: %s", e.getMessage());
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        try {
            if (sbn != null && packageManager != null) {
                storeData(packageManager.getApplicationLabel(packageManager.
                        getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA))
                        + " notification removed");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(TAG, "Couldn't find package name: %s", e.getMessage());
        }
    }

    //Classes
    private static class LoggingDirection {
        final boolean screenLog, appLog, appChanges;
        LoggingDirection(boolean screenLog, boolean appLog, boolean appChanges) {
            this.screenLog = screenLog;
            this.appLog = appLog;
            this.appChanges = appChanges;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction() != null && intent.getAction().equals(CONSTANTS.STARTFOREGROUND_ACTION)) {

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                serviceType = bundle.getInt("serviceType");
                restart = bundle.getBoolean("restart");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1087384, DeclareInForeground(serviceType),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(1087384, DeclareInForeground(serviceType));
            }

            if (intent.getExtras() == null) {
                return START_STICKY;
            }

            try {
                initializeComponents(bundle);
                initializeBroadcastReceivers();

                if (serviceType == 1) {
                    onListenerConnected();
                }

                if (restart) {
                    Handler restartHandler = new Handler();
                    Runnable documentRestart = () -> storeData(getString(R.string.phone_restart));
                    restartHandler.postDelayed(documentRestart, 10 * CONSTANTS.LOGGING_INTERVAL_MS);
                    storeData(getString(R.string.phone_restart));
                }
            } catch (Exception e) {
                Timber.e(TAG, "Could not initialize components: %s", e.getMessage());
            }
        }

        if (intent.getAction() != null && intent.getAction().equals(CONSTANTS.STOPFOREGROUND_ACTION)) {
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private Notification DeclareInForeground(int serviceType) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("ul2",
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            channel.setShowBadge(true);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        if (serviceType == 1) {
            notificationText = getString(R.string.notification_text_notes);
        } else {
            notificationText = getString(R.string.notification_text);
        }

        NotificationCompat.Builder nfc = new NotificationCompat.Builder(
                getApplicationContext(),"ul2")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(notificationText)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis());

        //link back to app when user clicks on notification
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }
        nfc.setContentIntent(pendingIntent);
        nfc.setDefaults(Notification.DEFAULT_ALL);

        return nfc.build();
    }

    private void initializeComponents(Bundle bundle) throws Exception {

        final String password;
        StoreInSQL storeInSQL;

        if (bundle == null) {
            throw new Exception("Bundle is null");
        }

        SharedPreferences securePreferences = Armadillo.create(this,
                        "service_no_note")
                .enableKitKatSupport(true).encryptionFingerprint(this).build();

        if (!bundle.getBoolean("restart")) {
            password = bundle.getString("password", "no_pwd");
            securePreferences.edit().putString("password", password).apply();
        } else {
            password = securePreferences.getString("password", "no_pwd");
        }

        for (String key: bundle.keySet()) {
            Timber.d(TAG,"Bundle logger: %s", key);
        }

        loggingDirection = new LoggingDirection(
                bundle.getBoolean("screenLog"),
                bundle.getBoolean("appLog"),
                bundle.getBoolean("appChanges")
        );

        Timber.d(TAG,"LoggingDirection: %s", loggingDirection);

        if (password == null || password.isEmpty() || password.equals("no_pwd")) {
            throw new Exception("Could not retrieve password");
        }

        storeInSQL = new StoreInSQL(this, CONSTANTS.CONTINUOUS_DB_NAME, CONSTANTS.DB_VERSION,
                CONSTANTS.CONTINUOUS_DB_TABLE);
        SQLiteDatabase.loadLibs(this);
        if (database == null) { //only if its not already open
            database = storeInSQL.getWritableDatabase(password);
        }

        handler = new Handler();
        if (serviceType == 1) {
            packageManager = getPackageManager();
        }
        currentlyRunningApp = getPackageName();

        identifyAppInForeground = new IdentifyAppInForeground();
    }

    /**
     * HANDLING BROADCAST RECEIVERS
     */
    private void initializeBroadcastReceivers() {

        //start logging use of apps in foreground
        if (loggingDirection.appLog) {
            handler.postDelayed(callIdentifyAppInForeground, CONSTANTS.LOGGING_INTERVAL_MS); //post delayed
        }

        if (loggingDirection.screenLog) {
            screenReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() != null) {
                        String action = intent.getAction();
                        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                            storeData(getString(R.string.screen_off));
                        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                            storeData(getString(R.string.screen_on));
                        } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                            storeData(getString(R.string.user_present));
                        }
                    }
                }
            };

            IntentFilter screenReceiverFilter = new IntentFilter();
            screenReceiverFilter.addAction(Intent.ACTION_SCREEN_OFF);
            screenReceiverFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenReceiverFilter.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(screenReceiver, screenReceiverFilter);
        }

        if (loggingDirection.appChanges) {
            SharedPreferences sharedPreferences = getSharedPreferences("appPrefs", MODE_PRIVATE);
            if (!sharedPreferences.getBoolean("initial_app_scan_done", false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    sharedPreferences.edit().putStringSet("installed_apps", getInstalledApps())
                            .putBoolean("initial_app_scan_done",true).apply();
                } else {
                    sharedPreferences.edit().putStringSet("installed_apps", getInstalledAppsWorkAround())
                            .putBoolean("initial_app_scan_done",true).apply();
                }
            }

            appReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() != null) {
                        String action = intent.getAction();
                        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                            Set<String> oldAppListAdd = sharedPreferences.getStringSet("installed_apps", new HashSet<>());
                            Set<String> newAppListAdd;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                newAppListAdd = getInstalledApps();
                            } else {
                                newAppListAdd = getInstalledAppsWorkAround();
                            }
                            if (newAppListAdd.containsAll(oldAppListAdd)) {
                                Set<String> newApps = identifyNewApp(oldAppListAdd, newAppListAdd);
                                for (String newApp : newApps) {
                                    Timber.d(TAG,"New installed app: %s", newApp);
                                    storeData(getString(R.string.app_installed, newApp));
                                }
                                sharedPreferences.edit().putStringSet("installed_apps", newAppListAdd).apply();
                            } else {
                                Timber.e(TAG,"Issue with package added broadcast receiver");
                            }
                        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                            Set<String> oldAppListRemoved = sharedPreferences.getStringSet("installed_apps", new HashSet<>());
                            Set<String> newAppListRemoved;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                newAppListRemoved = getInstalledApps();
                            } else {
                                newAppListRemoved = getInstalledAppsWorkAround();
                            }
                            if (oldAppListRemoved.containsAll(newAppListRemoved)) {
                                Set<String> removedApps = identifyNewApp(newAppListRemoved, oldAppListRemoved);
                                for (String removedApp : removedApps) {
                                    storeData(getString(R.string.app_removed, removedApp));
                                }
                                sharedPreferences.edit().putStringSet("installed_apps", newAppListRemoved).apply();
                            } else {
                                Timber.e(TAG, "Issue with package added broadcast receiver");
                            }
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
                PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS);

        Set<String> installedApps = new HashSet<>();
        for (PackageInfo packageInfo:appInstall){
            installedApps.add((String) packageInfo.applicationInfo.loadLabel(pm));
        }
        return installedApps;
    }

    //This is a workaround for the bug that exists in Android SDK 22 and lower that sometimes stops
    //installed apps from being listed. Funnily it works in StoreInPDF but not here! Here, only some
    //are listed before TransactionTooLarge exception is triggered.
    //https://stackoverflow.com/questions/13235793/transactiontoolargeeception-when-trying-to-get-a-list-of-applications-installed
    @SuppressLint("QueryPermissionsNeeded")
    private Set<String> getInstalledAppsWorkAround() {
        Process process;
        String line;
        PackageManager pm = getPackageManager();
        Set<String> installedApps = new HashSet<>();
        BufferedReader bufferedReader = null;
        try {
            process = Runtime.getRuntime().exec("pm list packages");
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line=bufferedReader.readLine())!=null) {
                final String packageName = line.substring(line.indexOf(':')+1);
                final PackageInfo packageInfo = pm.getPackageInfo(packageName,
                        PackageManager.GET_PERMISSIONS | PackageManager.GET_RECEIVERS |
                        PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS);
                String installedApp = String.valueOf(packageInfo);
                String[] split = installedApp.split(" ");
                String cleanAppString = split[1].substring(0, split[1].length() - 1);
                installedApps.add(cleanAppString);
            }
            process.waitFor();
        }
        catch(Exception e) {
            Timber.e(TAG, "Could not get installed apps: %s", e.getMessage());
        } finally {
            if (bufferedReader!=null)
                try {
                    bufferedReader.close();
                } catch(IOException e) {
                    Timber.e(TAG, "Could not close bufferedReader: %s", e.getMessage());
                }
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
            String appRunningInForeground = identifyAppInForeground.identifyForegroundTask(getApplicationContext());

            if (!appRunningInForeground.equals(currentlyRunningApp) &&
                    !appRunningInForeground.equals("not_real_app")) {
                storeData(getString(R.string.app, appRunningInForeground));
                currentlyRunningApp = appRunningInForeground;
                Timber.d(TAG,"App running in foreground: %s", appRunningInForeground);
            }

            handler.postDelayed(callIdentifyAppInForeground, CONSTANTS.LOGGING_INTERVAL_MS);
        }
    };

    void storeData(String data){
        ContentValues values = new ContentValues();
        values.put("time", System.currentTimeMillis());
        values.put("event", data);
        if (database.isOpen()) {
            Timber.d(TAG,"inserting into db: %s in %s", values, CONSTANTS.CONTINUOUS_DB_TABLE);
            database.insert(CONSTANTS.CONTINUOUS_DB_TABLE,null, values);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (database.isOpen()) database.close();
        if (screenReceiver != null && loggingDirection.screenLog) unregisterReceiver(screenReceiver);
        if (appReceiver != null && loggingDirection.appChanges) unregisterReceiver(appReceiver);
    }
}
