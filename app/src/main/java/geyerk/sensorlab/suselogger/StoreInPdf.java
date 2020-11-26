package geyerk.sensorlab.suselogger;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class StoreInPdf extends AsyncTask<Object, Integer, Object> {

    StoreInPdf(MainActivity delegate) { this.delegate = delegate;}

    private AsyncResult delegate;
    private LocalBroadcastManager localBroadcastManager;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected Object doInBackground(Object... objects) {
        final Context context = (Context) objects[0];
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        final String password = (String) objects[1];
        final int dataDirection = (int) objects[2];
        final int generalData = (int) objects[3];
        final File path = context.getFilesDir();
        File file;
        try {
        switch (generalData){
            case CONSTANTS.COLLECTING_CONTEXTUAL_DATA:
                file = new File(path, CONSTANTS.CONTEXT_FILE);
                boolean appropriateLength;

                appropriateLength = storeContextualData(file, context,dataDirection, password);

                if(!appropriateLength){
                    Timber.i("file exists: %s, permissions detected", file.exists());
                }

                return new DataCollectionResult(appropriateLength && file.exists() ,dataDirection,generalData, CONSTANTS.PUTTING_CONTEXTUAL_DATA_IN_PDF);
            case CONSTANTS.COLLECTING_PAST_USAGE:
                file = new File(path, CONSTANTS.USAGE_FILE);
                boolean appropriateUsageLength = false;
                try {
                    appropriateUsageLength =  storeUsageData(file, recordUsageEvent(dataDirection, context), password);
                } catch (Exception e) {
                    Timber.e(e);
                }
                Timber.i("File exists: %s - appropriateUsageLength: %s", appropriateUsageLength, file.exists());
                return new DataCollectionResult(appropriateUsageLength && file.exists() ,dataDirection,generalData, CONSTANTS.PUTTING_USAGE_DATA_IN_PDF);
            case CONSTANTS.COLLECTING_PROSPECTIVE_DATA:
                Timber.i("begging of packaging of prospective data");
                file = new File(path, CONSTANTS.PROSPECTIVE_FILE);
                boolean appropriateProspectiveLength = false;
                try{
                    appropriateProspectiveLength = storeProspectiveData(file, password, context);
                }catch (Exception e){
                    Timber.e(e);
                }
                return new DataCollectionResult(appropriateProspectiveLength && file.exists(), dataDirection, generalData, CONSTANTS.PUTTING_PROSPECTIVE_DATA_IN_PDF);
            default:
                Timber.i("general data not detected");
                return new DataCollectionResult(false,dataDirection, CONSTANTS.COLLECTING_CONTEXTUAL_DATA, CONSTANTS.PUTTING_PROSPECTIVE_DATA_IN_PDF);
        }
        } catch (DocumentException e) {
            Timber.e(e);
        } catch (FileNotFoundException e) {
            Timber.e(e);
        }

        return new DataCollectionResult(false, dataDirection, generalData, CONSTANTS.ERROR_EXPERIENCED_IN_ASYNC);
    }

    /**
     * CONTEXT METHODS
     */

    private boolean storeContextualData(File file, Context context, int dataDirection, String password) throws DocumentException, FileNotFoundException {
        Document document = new Document();
        PdfWriter writer;
        writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setEncryption(password.getBytes(), null, PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_256);
        document.open();
        document.setPageSize(PageSize.A4);
        PdfPTable table;

        CompleteContextData contextualData = gatherContextualData(context);
        int allToEnter = contextualData.permissionNumber;
        int count = 1;

        if(document.isOpen()){
            switch (dataDirection){
                case CONSTANTS.INSTALLED_AND_PERMISSION_AND_RESPONSE:

                    table = new PdfPTable(3);
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry pair = (Map.Entry)it.next();
                            table.addCell(app_to_permission_to_response.app);
                            table.addCell((String) pair.getKey());
                            table.addCell(String.valueOf(pair.getValue()));
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.INSTALLED_AND_PERMISSION:
                    table = new PdfPTable(2);
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry pair = (Map.Entry)it.next();
                            table.addCell((String) pair.getKey());
                            table.addCell(String.valueOf(pair.getValue()));
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.INSTALLED_AND_RESPONSE:
                    table = new PdfPTable(2);
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry pair = (Map.Entry)it.next();
                            table.addCell(app_to_permission_to_response.app);
                            table.addCell(String.valueOf(pair.getValue()));
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.ONLY_INSTALLED:
                    table = new PdfPTable(1);
                    allToEnter = contextualData.appNumber;
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        publishProgress(count++/allToEnter);
                        table.addCell(app_to_permission_to_response.app);
                    }
                    break;
                case CONSTANTS.RESPONSE_AND_PERMISSION:
                    table = new PdfPTable(2);
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        ;
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry pair = (Map.Entry)it.next();
                            table.addCell((String) pair.getKey());
                            table.addCell(String.valueOf(pair.getValue()));
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.ONLY_PERMISSION:
                    table = new PdfPTable(1);
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        ;
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry pair = (Map.Entry)it.next();
                            table.addCell((String) pair.getKey());
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.ONLY_RESPONSE:
                    table = new PdfPTable(1);
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        ;
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry pair = (Map.Entry)it.next();
                            table.addCell(String.valueOf(pair.getValue()));
                            it.remove();
                        }
                    }
                    break;

                default:
                    table = new PdfPTable(1);
                    table.addCell("No DATA");
                    break;
            }
            document.add(table);
            document.close();

            if(dataDirection != CONSTANTS.ONLY_INSTALLED){
                if(contextualData.permissionNumber == table.size()){
                    return true;
                }else{
                    Timber.i("permissions number: %d - table size: %d", contextualData.permissionNumber, table.size());
                    return false;
                }
            }else {
                if(contextualData.appNumber == table.size()){
                    return true;
                }else{
                    Timber.i("app number: %d - table size: %d", contextualData.appNumber, table.size());
                    return false;
                }
            }
        }else{
            Timber.e("Document was not open when it came to querying the database");
            return false;
        }
    }

    class CompleteContextData {

        final ArrayList<App_to_permission_to_response> app_to_permission_to_responses;
        final int permissionNumber, appNumber;
        CompleteContextData(ArrayList<App_to_permission_to_response> app_to_permission_to_responses, int permissionNumber){
            this.app_to_permission_to_responses = app_to_permission_to_responses;
            this.permissionNumber = permissionNumber;
            this.appNumber = app_to_permission_to_responses.size();
        }

    }
    class App_to_permission_to_response {

        final String app;
        final HashMap<String,Boolean> permissions_and_response;
        App_to_permission_to_response(String app, HashMap<String, Boolean> permissions_and_response){
            this.app = app;
            this.permissions_and_response = permissions_and_response;
        }


    }
    private CompleteContextData gatherContextualData(Context context) {
        ArrayList<App_to_permission_to_response> allData = new ArrayList<>();
        int permissionNumber = 0;


        PackageManager pm = context.getPackageManager();
        final List<PackageInfo> appInstall= pm.getInstalledPackages(PackageManager.GET_PERMISSIONS|PackageManager.GET_RECEIVERS|
                PackageManager.GET_SERVICES|PackageManager.GET_PROVIDERS);

        for(PackageInfo pInfo:appInstall) {
            HashMap<String, Boolean> permissionsResponse = new HashMap<>();
            final String[] permissions = pInfo.requestedPermissions;
            final int[] reaction = pInfo.requestedPermissionsFlags;
            Timber.i("app being viewed:  %s", pInfo.applicationInfo.loadLabel(pm));
            if(permissions != null){
                Timber.i("size of permissions: %d, size of reaction: %d", permissions.length, reaction.length);

                for(int i = 0; i <permissions.length; i++){
                    permissionsResponse.put(permissions[i], reaction[i] == 3);
                    permissionNumber++;
                }
            }else{
                Timber.e("Permissions equal null");
            }


            allData.add(new App_to_permission_to_response(""+pInfo.applicationInfo.loadLabel(pm),permissionsResponse));

        }

        return new CompleteContextData(allData, permissionNumber);
    }


    /**
     * USAGE METHODS
     */

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("WrongConstant")
    private  ArrayList<UsageRecord> recordUsageEvent(int daysToGoBack, Context context) throws Exception {
        ArrayList<UsageRecord> allData = new ArrayList<>();

        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService("usagestats");
        PackageManager packageManager = context.getPackageManager();
        if (usageStatsManager == null) {
            throw new Exception("usm is null");
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, - daysToGoBack);
        long start = calendar.getTimeInMillis();

        UsageEvents usageEvents = usageStatsManager.queryEvents(start, System.currentTimeMillis());

        if (usageEvents == null) {
            throw new Exception("UsageEvents is null");
        }

        while(usageEvents.hasNextEvent()){
            UsageEvents.Event e = new UsageEvents.Event();
            usageEvents.getNextEvent(e);

            String appName;
            try{
                appName = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(e.getPackageName(), PackageManager.GET_META_DATA));
            }catch (PackageManager.NameNotFoundException nameNotFound){
                appName = e.getPackageName();
            }

            allData.add(new UsageRecord(
                    e.getTimeStamp(),
                    appName,
                    e.getEventType()
            )) ;

        }
        return allData;
    }

    private boolean storeUsageData(File file, ArrayList<UsageRecord> recordedUsageEvent, String password) throws FileNotFoundException, DocumentException {
        Timber.i("Collecting past usage records");
        boolean appropriateSize = false;
        Document document = new Document();
        PdfWriter writer;
        writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setEncryption(password.getBytes(), null, PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_256);
        document.open();
        document.setPageSize(PageSize.A4);
        PdfPTable table;

        if(document.isOpen()){
            Timber.i("Document is open, usage");
            table = new PdfPTable(3);
            int count = 1;
            for(UsageRecord usageRecord :recordedUsageEvent){
                publishProgress(recordedUsageEvent.size()/count);
                table.addCell(String.valueOf(usageRecord.UNIXTime));
                table.addCell(usageRecord.app);
                table.addCell(String.valueOf(usageRecord.event));
            }
            document.add(table);
            Timber.i("table size: %d - recordedUsageEvent size %d", table.size(), recordedUsageEvent.size());
            if(table.size() == recordedUsageEvent.size()){
                appropriateSize = true;
            }
        }else{
            Timber.e("Document would not open");
        }
        document.close();
        return appropriateSize;
    }

    /**
     * PROSPECTIVE DATA METHODS
     */

    private boolean storeProspectiveData(File file, String password, Context context) throws FileNotFoundException, DocumentException {
        boolean appropriateSize = false;
        StoreInSQL storeInSQL = new StoreInSQL(context, "prospective.db",1, "prospective_table", "(time INTEGER, event TEXT)");
        SQLiteDatabase.loadLibs(context);
        SQLiteDatabase database = storeInSQL.getReadableDatabase(password);
        Cursor cursor = database.rawQuery("SELECT * FROM prospective_table",null);
        int event = cursor.getColumnIndex("event");
        int time= cursor.getColumnIndex("time");

        Document document = new Document();
        PdfWriter writer;
        writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setEncryption(password.getBytes(), null, PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_256);
        document.open();
        document.setPageSize(PageSize.A4);
        PdfPTable table = new PdfPTable(2);

        if(document.isOpen()){
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                table.addCell(String.valueOf(cursor.getLong(time)));
                table.addCell(cursor.getString(event));
            }
            document.add(table);
            Timber.i("Size of table: %d - Size of SQLTable: %d", table.size(), cursor.getCount());
            if(table.size() == cursor.getCount()){
                appropriateSize = true;
            }
        }
        document.close();
        database.close();
        return appropriateSize;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        delegate.processFinish((DataCollectionResult)o);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        Intent intent = new Intent("progressUpdate");
        intent.putExtra("progress", values[0]);
        localBroadcastManager.sendBroadcast(intent);
    }
}
