package psych.sensorlab.usagelogger2;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import timber.log.Timber;

class IdentifyAppInForeground {

    String identifyForegroundTask(Context context) {

        String currentApp = "not_real_app";
        long time = System.currentTimeMillis();
        final List<UsageStats> appList;
         UsageStatsManager usm;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            usm = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
        } else {
            //noinspection ResourceType
            usm = (UsageStatsManager)context.getSystemService("usagestats");
        }

        appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                time - CONSTANTS.LOGGING_INTERVAL_MS, time);

        if (appList != null && !appList.isEmpty()) {
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
                    Timber.e("Unable to get app name: %s", e.getLocalizedMessage());
                }

            }
        }
        return currentApp;
    }
}
