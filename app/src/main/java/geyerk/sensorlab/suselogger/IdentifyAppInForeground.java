package geyerk.sensorlab.suselogger;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

class IdentifyAppInForeground {

    final private ActivityManager activityManager;

    IdentifyAppInForeground(ActivityManager activityManager){
        this.activityManager = activityManager;
    }

    @SuppressLint("WrongConstant")
    String identifyForegroundTaskLollipop(Context context) {

        String currentApp = "THIS IS NOT A REAL APP";
        long time = System.currentTimeMillis();
        final List<UsageStats> appList;

        UsageStatsManager usm;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            usm = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
        } else {
            usm = (UsageStatsManager)context.getSystemService("usagestats");
        }

        appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000, time);

        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {

                currentApp = Objects.requireNonNull(mySortedMap.get(mySortedMap.lastKey())).
                        getPackageName();
                PackageManager packageManager = context.getPackageManager();

                try {
                    currentApp = (String) packageManager.getApplicationLabel(packageManager.
                            getApplicationInfo(currentApp, PackageManager.GET_META_DATA));
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
