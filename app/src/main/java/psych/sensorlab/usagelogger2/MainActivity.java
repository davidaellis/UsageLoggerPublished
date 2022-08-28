package psych.sensorlab.usagelogger2;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

import at.favre.lib.armadillo.Armadillo;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AsyncResult {

    DealWithPermission dealWithPermission;
    QRInput qrInput;
    PostAlert postAlert;
    SharedPreferences sharedPreferences, securePreferences;
    TextView reportScreen;
    ProgressBar progressBar;
    ActivityResultLauncher<Intent> startUsageNotePermissionsIntent, qrCodeScannerIntent;
    Intent chooserIntent;

    //initialize app
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeSharedPreferences();
        initializeUI();
        initializeError();
        initializeClasses();
        initializeLocalReceiver();

        startUsageNotePermissionsIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> dealWithPermission.determineEssentialPerms(permissionsToRequest()));

        qrCodeScannerIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (data != null) {
                        String config_data = data.getStringExtra("result");
                        try {
                            commitKsonToFile(config_data);
                        } catch (Exception e) {
                            sendMessage(5); //show dialogue about problem reading QR-code
                            Timber.e(e.getLocalizedMessage());
                        }
                        updateUI();
                    }
                });

        if(!sharedPreferences.getBoolean("informed_user", false)){
            postAlert.customiseMessage(0, "NA", getString(R.string.title_app_info),
                    getString(R.string.app_info_short),
                    "alertDialogResponse");
        }

        //lets check if the background service should be running and restart if needed
        if (QRCodeProvided() && sharedPreferences.getInt("current_status", 0) != CONSTANTS.FILE_SENT) {
            if (qrInput == null) {
                Gson gson = new Gson();
                qrInput = gson.fromJson(sharedPreferences.getString("qrcode_info",
                        "instructions not initialized"), QRInput.class);
            }
            startBackgroundLogging();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.close_app) {
            finishAndRemoveTask();
            return true;
        } else if (item.getItemId() == R.id.reset_app) {
            //delete configuration and restart app, perhaps show warning!
            stopServices();
            clearSharedPreferences();
            clearFiles();
            if (doesDatabaseExist(this, CONSTANTS.CONTINUOUS_DB_NAME)) {
                deleteDatabase(CONSTANTS.CONTINUOUS_DB_NAME);
            }
            triggerRebirth(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeSharedPreferences() {
        sharedPreferences = getSharedPreferences("QRInput", MODE_PRIVATE);
        securePreferences = Armadillo.create(this, "initialization_secure")
                .enableKitKatSupport(true)
                .encryptionFingerprint(this)
                .build();
    }

    private void initializeUI() {
        progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.INVISIBLE);
        reportScreen = findViewById(R.id.tvReport);
        int current_status = sharedPreferences.getInt("current_status", 0);
        Timber.i("current status %s", current_status);

        switch (current_status) {
            case CONSTANTS.COLLECTING_CONTEXTUAL_DATA:
                reportScreen.setText(R.string.coll_context_data);
                break;
            case CONSTANTS.COLLECTING_PAST_USAGE:
                reportScreen.setText(R.string.coll_past_usage_data);
                break;
            case CONSTANTS.COLLECTING_CONTINUOUS_DATA:
                reportScreen.setText(R.string.coll_continuous_data);
                break;
            case CONSTANTS.FILE_SENT:
                reportScreen.setText(R.string.study_fin);
                break;
        }

        findViewById(R.id.btnReadQR).setOnClickListener(this);
        findViewById(R.id.btnSeePassword).setOnClickListener(this);
        findViewById(R.id.btnNextStep).setOnClickListener(this);
        updateUI();
    }

    private void updateUI(){
        //controls if READ-QR Code button should be shown, or see password button
        Button passwordBtn = findViewById(R.id.btnSeePassword);
        Button nextStep = findViewById(R.id.btnNextStep);
        Button QRReadBtn = findViewById(R.id.btnReadQR);

        if (QRCodeProvided()) {
            nextStep.setVisibility(View.VISIBLE);
            QRReadBtn.setVisibility(View.GONE);
            passwordBtn.setVisibility(View.VISIBLE);
        } else {
            reportScreen.setText(R.string.click_read_qr);
            passwordBtn.setVisibility(View.GONE);
        }
    }

    private static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

    @Override
    public void onClick(@NonNull View view) {
        if (view.getId() == R.id.btnNextStep) {
            if (sharedPreferences.getBoolean("permissions_provided", false)) {
                collectFromDataSource();
            } else {
                if (QRCodeProvided()) {
                    Timber.i("QR code has already been scanned, continue with setup");
                    continueWithSetUp();
                } else {
                    if (checkCallingOrSelfPermission("android.permission.CAMERA")!=PackageManager.PERMISSION_GRANTED) {
                        Timber.i("Camera permission has NOT been given, asking for it now");
                        dealWithPermission.determineEssentialPerms(new String[]{Manifest.permission.CAMERA});
                    } else {
                        Timber.i("Okay, camera permission already given");
                        postAlert.customiseMessage(99, "NA", getString(R.string.title_next),
                                getString(R.string.pls_press_qr_code), "NA");
                    }
                }
            }
        } else if (view.getId() == R.id.btnReadQR) {
            Intent chooserIntent = new Intent(this, QRScanner.class);
            qrCodeScannerIntent.launch(chooserIntent);
        } else if (view.getId() == R.id.btnSeePassword) {
            informUserOnPassword();
        } else {
            throw new IllegalStateException("Unexpected value: " + view.getId());
        }
    }

    private void initializeError() {
        if (Timber.treeCount() == 0) { //make sure its not already initialized
            if (BuildConfig.DEBUG) {
                Timber.plant(new Timber.DebugTree() {
                    @NonNull
                    @Override
                    protected String createStackElementTag(@NotNull StackTraceElement element) {
                        return String.format("C:%s:%s", super.createStackElementTag(element),
                                element.getLineNumber());
                    }
                });
                Timber.i("Phone restarted previously: %s",
                        sharedPreferences.getBoolean("restarted", false));
            } else {
                //release mode
                Timber.plant(new ReleaseTree());
            }
        }
    }

    private void initializeClasses() {
        postAlert = new PostAlert(this);
        dealWithPermission = new DealWithPermission(this, postAlert);
    }

    private void initializeLocalReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver,
                new IntentFilter("alertDialogResponse"));
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver,
                new IntentFilter("progressUpdate"));
    }

    private boolean QRCodeProvided(){
        return !sharedPreferences.getString("qrcode_info",
                "instructions not initialized").equals("instructions not initialized");
    }

    private void continueWithSetUp() {
        if (checkCallingOrSelfPermission("android.permission.CAMERA") ==
                PackageManager.PERMISSION_GRANTED){
            if (QRCodeProvided()) {
                Gson gson = new Gson();
                qrInput = gson.fromJson(sharedPreferences.getString("qrcode_info",
                        "instructions not initialized"), QRInput.class);
                if (!securePreferences.getBoolean("password_generated", false)) {
                    sendMessage(1);
                } else {
                    dealWithPermission.determineEssentialPerms(permissionsToRequest());
                }
            }
        } else {
            dealWithPermission.determineEssentialPerms(new String[]{Manifest.permission.CAMERA});
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (QRCodeProvided()) {
            dealWithPermission.determineEssentialPerms(permissionsToRequest());
        } else {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                postAlert.customiseMessage(99, "NA", getString(R.string.title_next),
                        getString(R.string.pls_press_qr_code), "NA");
            } else {
                continueWithSetUp();
            }
        }
    }

    private void commitKsonToFile(String input) throws Exception {
        new QRInputHandler(input,this);
        final String messageToReport = sharedPreferences.getString("qrcode_info",
                "instructions not initialized");
        if (!messageToReport.equals("instructions not initialized")) {
            Timber.i("input from QR: %s", messageToReport);
            Gson gson = new Gson();
            qrInput = gson.fromJson(messageToReport, QRInput.class);
            sendMessage(1);
        } else {
            Timber.e("Issue deciphering QR configuration");
        }
    }

    private void sendMessage(int message) {
        Timber.i("showing message alert id: %s", message);
        switch (message){
            case CONSTANTS.ALERT_ATTENTION: //1
                // Attention!
                postAlert.customiseMessage(1, "NA", getString(R.string.title_attention),
                        getString(R.string.app_info),
                        "alertDialogResponse");
                break;
            case CONSTANTS.ALERT_INFO_COLLECTED: //2
                // What information will be collected?
                StringBuilder whatTheAppDoes = new StringBuilder();
                if (qrInput.dataSources.containsKey("contextual")) {
                    whatTheAppDoes.append("\n\n").append(getString(R.string.func_context1));
                    if (qrInput.contextualDataSources.contains("installed")) {
                        whatTheAppDoes.append("\n").append(getString(R.string.func_context2));
                    }
                    if (qrInput.contextualDataSources.contains("permission")) {
                        whatTheAppDoes.append("\n").append(getString(R.string.func_context3));
                    }
                    if (qrInput.contextualDataSources.contains("response")) {
                        whatTheAppDoes.append("\n").append(getString(R.string.func_context4));
                    }
                }
                if (qrInput.dataSources.containsKey("continuous")) {
                    whatTheAppDoes.append("\n\n").append(getString(R.string.func_continuous1));
                    whatTheAppDoes.append("\n").append(getString(R.string.func_continuous2));
                    if (qrInput.continuousDataSource.contains("screen")) {
                        whatTheAppDoes.append("\n").append(getString(R.string.func_continuous3));
                    }
                    if (qrInput.continuousDataSource.contains("app")) {
                        whatTheAppDoes.append("\n").append(getString(R.string.func_continuous4));
                    }
                    if (qrInput.continuousDataSource.contains("notification")) {
                        whatTheAppDoes.append("\n").append(getString(R.string.func_continuous5));
                    }
                    if (qrInput.continuousDataSource.contains("installed")) {
                        whatTheAppDoes.append("\n").append(getString(R.string.func_continuous6));
                    }
                }
                if (qrInput.dataSources.containsKey("usage")) {
                    whatTheAppDoes.append("\n\n").append(getString(R.string.func_past1)).
                            append(" ").append(qrInput.daysToMonitor).append(" ").
                            append(getString(R.string.func_past2)).append("\n").
                            append(getString(R.string.func_past3)).append("\n").
                            append(getString(R.string.func_past4));
                }
                postAlert.customiseMessage(2, "NA", getString(R.string.title_log_request),
                        getString(R.string.func_start) + whatTheAppDoes + "\n\n"
                                + getString(R.string.func_end),
                        "alertDialogResponse");
                break;
            case CONSTANTS.ALERT_PHONE_ROOTED: //3
                //Data security alert (e.g. if phone is rooted)
                postAlert.customiseMessage(3, "NA", getString(R.string.title_data_sec),
                        getString(R.string.data_security),
                        "alertDialogResponse");
                break;
            case CONSTANTS.ALERT_PERMISSIONS_NEEDED: //4
                //list of required permissions
                StringBuilder permissionsToBeRequested = new StringBuilder();
                if (qrInput.dataSources.containsKey("usage") || qrInput.continuousDataSource.contains("app")) {
                    permissionsToBeRequested.append("\n").append(getString(R.string.usage_perm));
                }
                if (qrInput.continuousDataSource.contains("notification")) {
                    permissionsToBeRequested.append("\n").append(getString(R.string.note_list_perm));
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    permissionsToBeRequested.append("\n").append(getString(R.string.show_note_list_perm));
                }
                if (permissionsToBeRequested.length() == 0) {
                    permissionsToBeRequested.append("\n").append(getString(R.string.no_perm_req));
                }
                //show alert listing what permissions will be asked for
                postAlert.customiseMessage(4, "NA", getString(R.string.title_perm_req),
                        getString(R.string.give_perms) + permissionsToBeRequested,
                        "alertDialogResponse");
                break;
            case CONSTANTS.ALERT_PROBLEM_QRCODE: //5
                postAlert.customiseMessage(5, "NA", getString(R.string.title_problem_qr),
                        getString(R.string.problem_read_qr),"NA");
        }
    }

    private String[] permissionsToRequest() {
        String[] permissionsNeeded = {"NA","NA"};
        if (qrInput.dataSources.containsKey("usage") || qrInput.continuousDataSource.contains("app")) {
            permissionsNeeded[0] ="usage";
        }
        if (qrInput.continuousDataSource.contains("notification")) {
            permissionsNeeded[1] = "notification";
        }
        return permissionsNeeded;
    }

    private void requestUsageNotePermissions(String permission){
        if (permission.equals("usage")) {
            chooserIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startUsageNotePermissionsIntent.launch(chooserIntent);
        } else if (permission.equals("notification")) {
            //request notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                chooserIntent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            } else {
                chooserIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            }
            startUsageNotePermissionsIntent.launch(chooserIntent);
        } else {
            Timber.i("All permission granted");
        }
    }

    /**
     * HANDLE RESPONSE TO ALERT DIALOGS
     */
    private final BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                int message = bundle.getInt("messageID", 100);
                String permission = bundle.getString("permissionToRequest", "NA");

                if (permission.equals("NA")) {
                    if (!securePreferences.getBoolean("password_generated", false)) {
                        securePreferences.edit().putLong("message" + message,
                                System.currentTimeMillis()).apply();
                    }
                    switch (message) {
                        case CONSTANTS.INFORM_USER: //0
                            sharedPreferences.edit().putBoolean("informed_user",true).apply();
                            continueWithSetUp();
                            break;
                        case CONSTANTS.QR_CODE_ACTIVITY: //1
                        case CONSTANTS.ALERT_DIALOG_CAMERA_PERMISSION: //2
                        case CONSTANTS.ALERT_DIALOG_USAGE_PERMISSION: //3
                            sendMessage(++message);
                            break;
                        case CONSTANTS.ALERT_DIALOG_NOTIFICATION_PERMISSION: //4
                            //lists which permissions are required, e.g. usage, notifications
                            dealWithPermission.determineEssentialPerms(permissionsToRequest());
                            break;
                        case CONSTANTS.GENERAL_USAGE_PERMISSION_REQUEST: //5
                            break;
                        case CONSTANTS.ALL_PERMISSIONS_GRANTED: //6
                            sharedPreferences.edit().putBoolean("permissions_provided", true).apply();
                            try {
                                generate_password();
                            } catch (Exception e) {
                                Timber.e(e);
                            }
                            updateUI();
                            generatePlanForDataCollection();
                            break;
                        case CONSTANTS.SEND_EMAIL: //7
                            sendEmail();
                            break;
                    }

                } else {
                    //request permissions
                    Timber.i("permission request: %s", permission);
                    requestUsageNotePermissions(permission);
                }
            }
        }
    };

    public void generate_password() {
        String pass = GeneratePassword.randomString(12);
        Timber.i("generated password: %s", pass);
        securePreferences.edit()
               .putString("password", pass)
               .putBoolean("password_generated", true)
               .apply();
    }

    final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if(bundle != null) {
            int update = bundle.getInt("progress");
            progressBar.setProgress(update);
        }
        }
    };

    private void informUserOnPassword() {
        //show password info alert
        Button password = findViewById(R.id.btnSeePassword);
        password.setVisibility(View.VISIBLE);
        postAlert.customiseMessage(5, "NA", getString(R.string.title_pwd),
                securePreferences.getString("password", "not generated yet"), "");
    }

    private void generatePlanForDataCollection() {
        int numberOfDataLoggingActivities = qrInput.dataSources.size();
        Timber.i("number of data logging activities: %d", numberOfDataLoggingActivities);
        sharedPreferences.edit().putInt("numberOfActivities", numberOfDataLoggingActivities).apply();
        collectFromDataSource();
    }

    /**
     * INITIATE DATA COLLECTION
     */
    private void collectFromDataSource() {

        updateUI();
        String nextDataSource = "not established";
        StoreInPdf storeInPdf;
        Resources resources = getResources();
        final int currentActivityTarget = sharedPreferences.getInt("current_activity", 0);

        if (qrInput == null) {
            Gson gson = new Gson();
            qrInput = gson.fromJson(sharedPreferences.getString("qrcode_info",
                    "instructions not initialized"), QRInput.class);
        }

        for (String dataSource: qrInput.dataSources.keySet()) {
            Integer currentDataSource = qrInput.dataSources.get(dataSource);
            Timber.i("data source: %s - currentDataSource: %d - currentActivityTarget: %d",
                    dataSource, currentDataSource, currentActivityTarget);
            if (currentDataSource!= null) {
                if (currentDataSource == currentActivityTarget) {
                    Timber.i("next data source: %s", dataSource);
                    nextDataSource = dataSource;
                    break;
                }
            }
        }

        switch (nextDataSource){
            case "contextual":
                progressBar.setVisibility(View.VISIBLE);
                reportScreen.setText(resources.getString(R.string.coll_context_data));
                sharedPreferences.edit().putInt("current_status",
                        CONSTANTS.COLLECTING_CONTEXTUAL_DATA).apply();
                storeInPdf = new StoreInPdf(this);
                storeInPdf.execute(this, securePreferences.getString("password", "not real password"),
                        CONSTANTS.RETURN_CONTEXT_TYPE(qrInput.contextualDataSources),
                        CONSTANTS.COLLECTING_CONTEXTUAL_DATA);
                break;
            case "continuous":
                progressBar.setVisibility(View.INVISIBLE);
                reportScreen.setText(resources.getString(R.string.coll_continuous_data));
                sharedPreferences.edit().putInt("current_status",
                        CONSTANTS.COLLECTING_CONTINUOUS_DATA).apply();
                if (sharedPreferences.getBoolean("doing_background_logging", false)){
                    storeInPdf = new StoreInPdf(this);
                    storeInPdf.execute(this,
                            securePreferences.getString("password","not real password"),
                            CONSTANTS.READY_FOR_EMAIL, CONSTANTS.COLLECTING_CONTINUOUS_DATA);
                } else {
                    startBackgroundLogging();
                }
                break;
            case "usage": //past
                progressBar.setVisibility(View.VISIBLE);
                reportScreen.setText(resources.getString(R.string.coll_past_usage_data));
                sharedPreferences.edit().putInt("current_status",
                        CONSTANTS.COLLECTING_PAST_USAGE).apply();
                storeInPdf = new StoreInPdf(this);
                storeInPdf.execute(this, securePreferences.getString("password", "not real password"),
                        qrInput.daysToMonitor, CONSTANTS.COLLECTING_PAST_USAGE);
                break;
            case "finish":
                progressBar.setVisibility(View.GONE);
                postAlert.customiseMessage(CONSTANTS.SEND_EMAIL, "NA", getString(R.string.title_send_email),
                        getString(R.string.send_data_away),
                        "alertDialogResponse");
                break;
            default:
                Timber.i("Next data source not established");
                break;
        }
    }

    private void startBackgroundLogging() {

        Intent toStartService;

        if (qrInput == null) {
            Gson gson = new Gson();
            qrInput = gson.fromJson(sharedPreferences.getString("qrcode_info",
                    "Instructions not initialized"), QRInput.class);
        }

        sharedPreferences.edit().putInt("current_status", CONSTANTS.COLLECTING_CONTINUOUS_DATA).
                putBoolean("doing_background_logging", true).apply();

        //start logging (either with or without notification logging)
        if (!qrInput.continuousDataSource.contains("notification")) {
            toStartService = new Intent(this, LoggerService.class);
        } else {
            //start logger with notification data collection (LoggerWithNotesService)
            toStartService = new Intent(this, LoggerWithNotesService.class);
        }

        Bundle bundle = new Bundle();
        bundle.putString("password", securePreferences.getString("password", "not real password"));
        bundle.putBoolean("restart", false);
        bundle.putBoolean("screenLog", qrInput.continuousDataSource.contains("screen"));
        bundle.putBoolean("appLog", qrInput.continuousDataSource.contains("app"));
        bundle.putBoolean("appChanges", qrInput.continuousDataSource.contains("installed"));
        toStartService.putExtras(bundle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(toStartService);
        } else {
            startService(toStartService);
        }
    }

    /**
     * RESPOND TO DATA COLLECTION INITIATIVES
     */
    @Override
    public void processFinish(@NonNull DataCollectionResult output) {
        Timber.i("data collection result. DataRetrieved: %d - Success: %s",
                output.dataRetrieved, output.success);
        switch (output.dataSource){
            case CONSTANTS.COLLECTING_CONTEXTUAL_DATA:
                if (output.task == CONSTANTS.PUTTING_CONTEXTUAL_DATA_IN_PDF){
                    sharedPreferences.edit().putInt("current_activity",
                            (1+sharedPreferences.getInt("current_activity", 0))).apply();
                    collectFromDataSource();
                }
                break;
            case CONSTANTS.COLLECTING_PAST_USAGE:
            case CONSTANTS.COLLECTING_CONTINUOUS_DATA:
                sharedPreferences.edit().putInt("current_activity",
                        (1+sharedPreferences.getInt("current_activity", 0))).apply();
                collectFromDataSource();
                break;
        }
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("text/plain");

        String dir = (getFilesDir() + File.separator);
        String authority = BuildConfig.APPLICATION_ID + ".fileprovider";
        ArrayList<Uri> files = new ArrayList<>();

        File
                contextFile = new File(dir + File.separator + CONSTANTS.CONTEXT_FILE),
                usageEvents = new File(dir + File.separator + CONSTANTS.USAGE_FILE),
                continuous = new File(dir + File.separator + CONSTANTS.CONTINUOUS_FILE);

        try {
            if (contextFile.exists()) {
                files.add(FileProvider.getUriForFile(this, authority, contextFile));
            }

            if (usageEvents.exists()) {
                files.add(FileProvider.getUriForFile(this, authority, usageEvents));
            }

            if (continuous.exists()) {
                files.add(FileProvider.getUriForFile(this, authority, continuous));
            }

            if (files.size() > 0) {
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                sharedPreferences.edit().putInt("current_status", CONSTANTS.FILE_SENT).apply();
                TextView view = findViewById(R.id.tvReport);
                view.setText(R.string.study_fin);
                stopServices();

                startActivity(intent);
            } else {
                Timber.e("No files to upload");
            }
        }
        catch (Exception e){
            Timber.e(e.getLocalizedMessage());
        }
    }

    private void stopServices() {
        Intent loggingService;
        Timber.i("gee im here: %s", QRCodeProvided());
        if (QRCodeProvided()) {
            if (!qrInput.continuousDataSource.contains("notification")) {
                loggingService = new Intent(MainActivity.this, LoggerService.class);
            } else {
                Timber.i("gee ime here 2");
                loggingService = new Intent(MainActivity.this, LoggerWithNotesService.class);
            }
            stopService(loggingService);
        }
    }

    public void clearSharedPreferences(){
        File dir = new File(getApplicationInfo().dataDir, "shared_prefs");
        Timber.i("files shared: %s", dir);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    getSharedPreferences(child.replace(".xml", ""),
                            Context.MODE_PRIVATE).edit().clear().apply();
                    new File(dir, child).delete();
                }
            }
        }
    }

    public void clearFiles(){
        //File dir = new File(getFilesDir().getParent() + "/files/");
        File dir = new File(getApplicationInfo().dataDir, "files");
        Timber.i("files: %s", dir);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    new File(dir, child).delete();
                }
            }
        }
    }

    public static void triggerRebirth(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //close alert dialog if open when app is closed
        if (postAlert.showingDialog()) {
            postAlert.dismissDialog();
        }
    }
}
