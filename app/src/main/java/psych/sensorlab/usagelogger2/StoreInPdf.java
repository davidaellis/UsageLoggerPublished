package psych.sensorlab.usagelogger2;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.os.AsyncTask;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.itextpdf.text.Chunk;
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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class StoreInPdf extends AsyncTask<Object, Integer, Object> {

    StoreInPdf(MainActivity delegate) { this.delegate = delegate; }

    private final AsyncResult delegate;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    protected Object doInBackground(Object... objects) {
        final Context context = (Context) objects[0];
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
        final String password = (String) objects[1];
        final int dataDirection = (int) objects[2];
        final int generalData = (int) objects[3];
        File file;

        switch (generalData) {
            case CONSTANTS.COLLECTING_CONTEXTUAL_DATA:
                file = new File(context.getFilesDir(), CONSTANTS.CONTEXT_FILE);
                boolean appropriateLength = false;
                try {
                    appropriateLength = storeContextualData(file, context, dataDirection, password);
                } catch (Exception e) {
                    Timber.e(e.getLocalizedMessage());
                }
                Timber.d("File exists: %s, permissions detected", file.exists());
                return new DataCollectionResult(appropriateLength && file.exists(),
                        dataDirection, generalData, CONSTANTS.PUTTING_CONTEXTUAL_DATA_IN_PDF);

            case CONSTANTS.COLLECTING_PAST_USAGE:
                file = new File(context.getFilesDir(), CONSTANTS.USAGE_FILE);
                boolean appropriateUsageLength = false;
                try {
                    appropriateUsageLength = storeUsageData(file,
                            recordUsageEvent(dataDirection, context), password, context);
                } catch (Exception e) {
                    Timber.e(e.getLocalizedMessage());
                }
                Timber.d("File exists: %s - appropriateUsageLength: %s",
                        appropriateUsageLength, file.exists());
                return new DataCollectionResult(appropriateUsageLength && file.exists(),
                        dataDirection, generalData, CONSTANTS.PUTTING_USAGE_DATA_IN_PDF);

            case CONSTANTS.COLLECTING_CONTINUOUS_DATA:
                Timber.d("Begin of packaging of continuous data");
                file = new File(context.getFilesDir(), CONSTANTS.CONTINUOUS_FILE); //create PDF
                boolean appropriateContinuousLength = false;
                try {
                    appropriateContinuousLength = storeContinuousData(file, password, context);
                } catch (Exception e) {
                    Timber.e(e.getLocalizedMessage());
                }
                return new DataCollectionResult(appropriateContinuousLength && file.exists(),
                        dataDirection, generalData, CONSTANTS.PUTTING_CONTINUOUS_DATA_IN_PDF);

            default:
                Timber.d("General data not detected");
                return new DataCollectionResult(false, dataDirection,
                        CONSTANTS.COLLECTING_CONTEXTUAL_DATA,
                        CONSTANTS.PUTTING_CONTINUOUS_DATA_IN_PDF);
        }

    }

    /**
     * CONTEXT METHODS
     */
    private boolean storeContextualData (File file, Context context, int dataDirection,
        String password) throws DocumentException, FileNotFoundException {

        String versionRelease = Build.VERSION.RELEASE;
        String versionSDK = String.valueOf(android.os.Build.VERSION.SDK_INT);

        Document document = new Document();
        PdfWriter writer;
        //noinspection IOStreamConstructor
        writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setEncryption(password.getBytes(),null, PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_256);
        document.open();
        document.setPageSize(PageSize.A4);
        document.addTitle(context.getString(R.string.contextual_data));
        document.addAuthor(context.getString(R.string.app_name));
        document.addSubject(context.getString(R.string.pdf_subject, versionSDK, versionRelease));
        document.add(new Chunk("")); //in case pdf is empty
        PdfPTable table;

        CompleteContextData contextualData = gatherContextualData(context);
        int allToEnter = contextualData.permissionNumber;
        int count = 1;

        if(document.isOpen()){
            switch (dataDirection){
                case CONSTANTS.INSTALLED_AND_PERMISSION_AND_RESPONSE: //c123
                    table = new PdfPTable(3);
                    for (App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator<Map.Entry<String, Boolean>> it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter / count++);
                            Map.Entry<String, Boolean> pair = it.next();
                            table.addCell(app_to_permission_to_response.app); //app installed
                            table.addCell(pair.getKey()); //permission req. by app
                            table.addCell(String.valueOf(pair.getValue())); //response by user
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.INSTALLED_AND_PERMISSION: //c12
                    table = new PdfPTable(2);
                    for (App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator<Map.Entry<String, Boolean>> it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry<String, Boolean> pair = it.next();
                            table.addCell(app_to_permission_to_response.app);
                            table.addCell(pair.getKey());
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.RESPONSE_AND_PERMISSION: //c23
                    table = new PdfPTable(2);
                    for (App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator<Map.Entry<String, Boolean>> it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry<String, Boolean> pair = it.next();
                            table.addCell(pair.getKey());
                            table.addCell(String.valueOf(pair.getValue()));
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.INSTALLED_AND_RESPONSE: //c13
                    table = new PdfPTable(2);
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        Iterator<Map.Entry<String, Boolean>> it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry<String, Boolean> pair = it.next();
                            table.addCell(app_to_permission_to_response.app);
                            table.addCell(String.valueOf(pair.getValue()));
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.ONLY_INSTALLED: //c1
                    table = new PdfPTable(1);
                    allToEnter = contextualData.appNumber;
                    for(App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses){
                        publishProgress(count++/allToEnter);
                        table.addCell(app_to_permission_to_response.app);
                    }
                    break;
                case CONSTANTS.ONLY_PERMISSION: //c2
                    table = new PdfPTable(1);
                    for (App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses) {
                        Iterator<Map.Entry<String, Boolean>> it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry<String, Boolean> pair = it.next();
                            table.addCell(pair.getKey());
                            it.remove();
                        }
                    }
                    break;
                case CONSTANTS.ONLY_RESPONSE: //c3
                    table = new PdfPTable(1);
                    for (App_to_permission_to_response app_to_permission_to_response: contextualData.app_to_permission_to_responses) {
                        Iterator<Map.Entry<String, Boolean>> it = app_to_permission_to_response.permissions_and_response.entrySet().iterator();
                        while (it.hasNext()) {
                            publishProgress(allToEnter /count++);
                            Map.Entry<String, Boolean> pair = it.next();
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

            if (dataDirection != CONSTANTS.ONLY_INSTALLED){
                if (contextualData.permissionNumber == table.size()) {
                    return true;
                } else {
                    Timber.d("Permissions number: %d - table size: %d", contextualData.permissionNumber, table.size());
                    return false;
                }
            } else {
                if (contextualData.appNumber == table.size()) {
                    return true;
                } else {
                    Timber.d("App number: %d - table size: %d", contextualData.appNumber, table.size());
                    return false;
                }
            }
        } else {
            Timber.e("Document not open when querying database");
            return false;
        }
    }

    static class CompleteContextData {
        final ArrayList<App_to_permission_to_response> app_to_permission_to_responses;
        final int permissionNumber, appNumber;
        CompleteContextData(ArrayList<App_to_permission_to_response> app_to_permission_to_responses, int permissionNumber){
            this.app_to_permission_to_responses = app_to_permission_to_responses;
            this.permissionNumber = permissionNumber;
            this.appNumber = app_to_permission_to_responses.size();
        }

    }

    static class App_to_permission_to_response {
        final String app;
        final HashMap<String,Boolean> permissions_and_response;
        App_to_permission_to_response(String app, HashMap<String, Boolean> permissions_and_response){
            this.app = app;
            this.permissions_and_response = permissions_and_response;
        }
    }

    private CompleteContextData gatherContextualData(Context context) {

        PackageManager pm = context.getPackageManager();
        ArrayList<App_to_permission_to_response> allData = new ArrayList<>();
        int permissionNumber = 0;

        //@SuppressLint("QueryPermissionsNeeded")
        final List<PackageInfo> appInstall= pm.getInstalledPackages(
                PackageManager.GET_PERMISSIONS | PackageManager.GET_RECEIVERS |
                PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS);

        for (PackageInfo pInfo:appInstall) {
            HashMap<String, Boolean> permissionsResponse = new HashMap<>(); //don't move
            final String[] permissions = pInfo.requestedPermissions;
            final int[] reaction = pInfo.requestedPermissionsFlags;
            Timber.d("installed app:  %s", pInfo.applicationInfo.loadLabel(pm));

            if (permissions != null) {
                Timber.d("number of permissions: %d, size of reaction: %d",
                        permissions.length, reaction.length);
                for (int i = 0; i <permissions.length; i++) {
                    permissionsResponse.put(permissions[i], reaction[i] == 3);
                    permissionNumber++;
                }
            } else {
                Timber.e("Permissions equal null");
            }

            allData.add(new App_to_permission_to_response("" +
                    pInfo.applicationInfo.loadLabel(pm), permissionsResponse));
        }

        return new CompleteContextData(allData, permissionNumber);
    }

    /**
     * USAGE METHODS
     */
    @SuppressLint("WrongConstant")
    private ArrayList<UsageRecord> recordUsageEvent(int daysToGoBack, Context context) throws Exception {
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

        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event e = new UsageEvents.Event();
            usageEvents.getNextEvent(e);

            String appName;
            try {
                appName = (String) packageManager.getApplicationLabel(packageManager.
                        getApplicationInfo(e.getPackageName(), PackageManager.GET_META_DATA));
            } catch (PackageManager.NameNotFoundException nameNotFound){
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

    private boolean storeUsageData(File file, ArrayList<UsageRecord> recordedUsageEvent,
        String password, Context context) throws FileNotFoundException, DocumentException {

        Timber.d("Collecting past usage records");
        boolean appropriateSize = false;
        String versionRelease = android.os.Build.VERSION.RELEASE;
        String versionSDK = String.valueOf(android.os.Build.VERSION.SDK_INT);

        Document document = new Document();
        PdfWriter writer;
        //noinspection IOStreamConstructor
        writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setEncryption(password.getBytes(),null, PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_256);
        document.open();
        document.setPageSize(PageSize.A4);
        document.addTitle(context.getString(R.string.usage_data));
        document.addAuthor(context.getString(R.string.app_name));
        document.addSubject(context.getString(R.string.pdf_subject, versionSDK, versionRelease));
        document.add(new Chunk("")); //in case pdf is empty
        PdfPTable table;

        if(document.isOpen()){
            table = new PdfPTable(3);
            int count = 1;

            for(UsageRecord usageRecord :recordedUsageEvent){
                publishProgress(recordedUsageEvent.size()/count);
                table.addCell(String.valueOf(usageRecord.UNIXTime));
                table.addCell(usageRecord.app);
                table.addCell(String.valueOf(usageRecord.event));
            }
            document.add(table);
            Timber.d("table size: %d - recordedUsageEvent size %d", table.size(), recordedUsageEvent.size());
            if (table.size() == recordedUsageEvent.size()) {
                appropriateSize = true;
            }
        } else {
            Timber.e("Document would not open");
        }
        document.close();
        return appropriateSize;
    }

    /**
     * CONTINUOUS DATA METHODS
     */
    private boolean storeContinuousData (File file, String password, Context context) throws
            FileNotFoundException, DocumentException {

        Timber.d("Collecting continuous records");

        boolean appropriateSize = false;
        StoreInSQL storeInSQL;
        String versionRelease = android.os.Build.VERSION.RELEASE;
        String versionSDK = String.valueOf(android.os.Build.VERSION.SDK_INT);

        storeInSQL = new StoreInSQL(context, CONSTANTS.CONTINUOUS_DB_NAME, CONSTANTS.DB_VERSION,
                CONSTANTS.CONTINUOUS_DB_TABLE);
        SQLiteDatabase.loadLibs(context);
        SQLiteDatabase database = storeInSQL.getReadableDatabase(password);

        Cursor cursor = database.rawQuery("SELECT * FROM continuous_table",null);
        int event = cursor.getColumnIndex("event");
        int time = cursor.getColumnIndex("time");

        Document document = new Document();
        PdfWriter writer;
        //noinspection IOStreamConstructor
        writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setEncryption(password.getBytes(), null, PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_256);
        document.open();
        document.setPageSize(PageSize.A4);
        document.addTitle(context.getString(R.string.continuous_data));
        document.addAuthor(context.getString(R.string.app_name));
        document.addSubject(context.getString(R.string.pdf_subject, versionSDK, versionRelease));
        document.add(new Chunk("")); //in case pdf is empty
        PdfPTable table = new PdfPTable(2);

        if (document.isOpen()) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                table.addCell(String.valueOf(cursor.getLong(time)));
                table.addCell(cursor.getString(event));
                Timber.d("dbinsert: %s", cursor.getString(event));
            }
            document.add(table);
            Timber.d("Size of table: %d - Size of SQLTable: %d", table.size(), cursor.getCount());
            if (table.size() == cursor.getCount()) {
                appropriateSize = true;
            }
            cursor.close();
            document.close();
        }
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
