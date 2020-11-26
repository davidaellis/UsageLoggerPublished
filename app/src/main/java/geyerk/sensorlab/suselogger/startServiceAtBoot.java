package geyerk.sensorlab.suselogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.google.gson.Gson;

import static android.content.Context.MODE_PRIVATE;

public class startServiceAtBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            SharedPreferences sharedPreferences = context.getSharedPreferences("QRInput", MODE_PRIVATE);
            Gson gson = new Gson();
            QRInput qrInput = gson.fromJson(sharedPreferences.getString("instructions from QR", "instructions not initialized"), QRInput.class);
            sharedPreferences.edit().putBoolean("restarted", true).apply();
            //check if anything needs to be restarted
            if(qrInput.dataSources.keySet().contains("prospective")){
                Intent startServiceIntent;

                Bundle bundle = new Bundle();
                bundle.putBoolean("restart", true);
                bundle.putBoolean("screenLog", qrInput.prospectiveDataSource.contains("screen"));
                bundle.putBoolean("appLog", qrInput.prospectiveDataSource.contains("app"));
                bundle.putBoolean("appChanges", qrInput.prospectiveDataSource.contains("installed"));
                if (qrInput.prospectiveDataSource.contains("notification")){
                    //start service for notification listening
                    startServiceIntent = new Intent(context, ProspectiveLoggerWithNotes.class);
                }else{
                    //start service without notification listening
                    startServiceIntent = new Intent(context, ProspectiveLogger.class);
                }
                startServiceIntent.putExtras(bundle);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    context.startService(startServiceIntent);
                }else {
                    context.startForegroundService(startServiceIntent);
                }
            }
        }
    }
}
