package geyerk.sensorlab.suselogger;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

class IdentifyAppInForeground {

    private UsageStatsManager usageStatsManager;
    private Context context;
    private ActivityManager activityManager;

    IdentifyAppInForeground(Context context, UsageStatsManager usageStatsManager){
        this.usageStatsManager = usageStatsManager;
        this.context = context;
    }

    IdentifyAppInForeground(ActivityManager activityManager){
        this.activityManager = activityManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("WrongConstant")
     String identifyForegroundTaskLollipop() {

        String currentApp = "THIS IS NOT A REAL APP";

        long time = System.currentTimeMillis();
        List<UsageStats> appList;


        appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000, time);


        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {

                currentApp = Objects.requireNonNull(mySortedMap.get(mySortedMap.lastKey())).getPackageName();
                PackageManager packageManager = context.getPackageManager();

                try {
                    currentApp = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(currentApp, PackageManager.GET_META_DATA));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }
        return currentApp;
    }


     String identifyForegroundTaskUnderLollipop() {
        List<ActivityManager.RunningAppProcessInfo> tasks;

        tasks = activityManager.getRunningAppProcesses();

        return tasks.get(0).processName;
    }
}
