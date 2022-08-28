package psych.sensorlab.usagelogger2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.google.gson.Gson;

import static android.content.Context.MODE_PRIVATE;

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

            //if (qrInput == null) {
                Gson gson = new Gson();
                qrInput = gson.fromJson(sharedPreferences.getString("qrcode_info",
                        "instructions not initialized"), QRInput.class);
           // }

            Timber.i("Restarted service after booting");

            //check if anything needs to be restarted
            if (qrInput.dataSources.containsKey("continuous")) {
                Intent startServiceIntent;

                Bundle bundle = new Bundle();
                bundle.putBoolean("restart", true);
                bundle.putBoolean("screenLog", qrInput.continuousDataSource.contains("screen"));
                bundle.putBoolean("appLog", qrInput.continuousDataSource.contains("app"));
                bundle.putBoolean("appChanges", qrInput.continuousDataSource.contains("installed"));

                if (qrInput.continuousDataSource.contains("notification")) {
                    //start service for notification listening
                    //(e.g., continuous logging with notifications was selected in configuration)
                    startServiceIntent = new Intent(context, LoggerWithNotesService.class);
                } else {
                    //start service without notification listening
                    startServiceIntent = new Intent(context, LoggerService.class);
                }
                startServiceIntent.putExtras(bundle);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(startServiceIntent);
                } else {
                    context.startForegroundService(startServiceIntent);
                }
            }
        }
    }

    private void initializeError() {
        if (Timber.treeCount() == 0) {
            if (BuildConfig.DEBUG) {
                Timber.plant(new Timber.DebugTree() {
                    @NonNull
                    @Override
                    protected String createStackElementTag(@NotNull StackTraceElement element) {
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
