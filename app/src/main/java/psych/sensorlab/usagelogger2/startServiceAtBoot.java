package psych.sensorlab.usagelogger2;

import static android.content.Context.MODE_PRIVATE;

import static timber.log.Timber.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.google.gson.Gson;
import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class startServiceAtBoot extends BroadcastReceiver {

    QRInput qrInput;
    SharedPreferences sharedPreferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            initializeError();
            sharedPreferences = context.getSharedPreferences("QRInput", MODE_PRIVATE);
            sharedPreferences.edit().putBoolean("restarted", true).apply();
            int serviceType = 0;

            if (qrInput == null) {
                Gson gson = new Gson();
                qrInput = gson.fromJson(sharedPreferences.getString("qrcode_info",
                        "instructions not initialized"), QRInput.class);
            }

            d("Restarted service after booting");

            //check if anything needs to be restarted
            if (qrInput.dataSources.containsKey("continuous")) {

                //start logging (either with or without notification logging)
                if (qrInput.continuousDataSource.contains("notification")) {
                    //start logger with notification data collection
                    serviceType = 1;
                }

                Intent startIntent = new Intent(context, Logger.class);
                startIntent.setAction(CONSTANTS.STARTFOREGROUND_ACTION);

                Bundle bundle = new Bundle();
                bundle.putBoolean("restart", true);
                bundle.putBoolean("screenLog", qrInput.continuousDataSource.contains("screen"));
                bundle.putBoolean("appLog", qrInput.continuousDataSource.contains("app"));
                bundle.putBoolean("appChanges", qrInput.continuousDataSource.contains("installed"));
                bundle.putInt("serviceType", serviceType);
                startIntent.putExtras(bundle);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(startIntent);
                } else {
                    context.startForegroundService(startIntent);
                }
            }
        }
    }

    private void initializeError() {
        if (Timber.treeCount() == 0) {
            if (BuildConfig.DEBUG) {
                Timber.plant(new DebugTree() {
                    @NonNull
                    @Override
                    public String createStackElementTag(@NotNull StackTraceElement element) {
                        return String.format("C:%s:%s", super.createStackElementTag(element),
                                element.getLineNumber());
                    }
                });
            } else {
                //release mode
                Timber.plant(new ReleaseTree());
            }
        }
    }
}
