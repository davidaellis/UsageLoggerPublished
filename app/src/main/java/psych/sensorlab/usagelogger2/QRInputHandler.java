package psych.sensorlab.usagelogger2;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

class QRInputHandler {

    private final String input;

    QRInputHandler(String input, Context context) throws Exception {
        this.input = input;
        Gson gson = new Gson();
        String json  = gson.toJson(analyseInput());
        SharedPreferences sharedPreferences = context.getSharedPreferences("QRInput", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("instructions from QR", json).apply();
    }

    private QRInput analyseInput() throws Exception {
        Timber.i("The input: %s", this.input);
        String[] rows = this.input.split("\n");
        HashMap<String, Integer> dataSources = new HashMap<>();
        Set<String> contextualDataSources = new HashSet<>();
        Set<String> continuousDataSources = new HashSet<>();
        int daysToMonitor = 0;
        if (rows.length < 1){
            throw new Exception("No data detected in QR");
        }
        if(rows[1].charAt(3) == 'T'){
            dataSources.put("contextual", returnOrder(rows[1]));
            if(rows[1].charAt(8) == 'T'){
                contextualDataSources.add("installed");
            }
            if(rows[1].charAt(12) == 'T'){
                contextualDataSources.add("permission");
            }
            if(rows[1].charAt(16) == 'T'){
                contextualDataSources.add("response");
            }

        }
        if(rows[2].charAt(3) == 'T'){
            dataSources.put("usage", returnOrder(rows[2]));
            String suspectDaysToMonitor = String.valueOf(rows[2].charAt(8));
            if(onlyDigits(suspectDaysToMonitor)){
                daysToMonitor = Integer.parseInt(suspectDaysToMonitor);
            }
        }
        if(rows[3].charAt(3) == 'T'){
            dataSources.put("continuous", returnOrder(rows[3]));
            if(rows[3].charAt(8) == 'T'){
                continuousDataSources.add("screen");
            }
            if(rows[3].charAt(12) == 'T'){
                continuousDataSources.add("app");
            }
            if(rows[3].charAt(16) == 'T'){
                continuousDataSources.add("notification");
            }
            if(rows[3].charAt(20) == 'T'){
                continuousDataSources.add("installed");
            }
        }
        dataSources.put("finish", dataSources.size());

        return new QRInput(dataSources, contextualDataSources, daysToMonitor, continuousDataSources);
    }

    private int returnOrder(final String row){
        int position = 4;
        final String characterOfInterest = String.valueOf(row.charAt(row.length()-2));
        if(onlyDigits(characterOfInterest)){
            position = Integer.parseInt(characterOfInterest);
        }
        return position;
    }

    private boolean onlyDigits (final String toTest){
        return toTest.matches("[0-9]+");
    }
}
