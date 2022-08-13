package psych.sensorlab.usagelogger2;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    /**
     * GLOBAL VARIABLES
     */
    //CLASSES
    DealWithPermission dealWithPermission;
    QRInput qrInput;
    PostAlert postAlert;

    //COMPONENTS
    SharedPreferences sharedPreferences, securePreferences;
    TextView reportScreen;
    ProgressBar progressBar;

    /**
     * DIRECTLY RELATED TO INITIALIZING APP
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeSharedPreferences();
        initializeUI();
        initializeError();
        initializeClasses();
        initializeLocalReceiver();

        if(!sharedPreferences.getBoolean("informed user", false)){
            postAlert.customiseMessage(0, "NA", getString(R.string.title_app_info),
                    getString(R.string.app_info_short), "alertDialogResponse");
        }

    }

    private void initializeSharedPreferences() {
        sharedPreferences = getSharedPreferences("QRInput", MODE_PRIVATE);
        securePreferences = Armadillo.create(this, "Initialization secure")
                .enableKitKatSupport(true)
                .encryptionFingerprint(this)
                .build();
    }

    private void initializeUI() {
        progressBar = findViewById(R.id.pb);
        progressBar.setVisibility(View.INVISIBLE);
        reportScreen = findViewById(R.id.tvReport);
        switch (sharedPreferences.getInt("data collected info", 0)){
            case CONSTANTS.COLLECTING_CONTEXTUAL_DATA:
                reportScreen.setText(R.string.coll_context_data);
                break;
            case CONSTANTS.COLLECTING_PAST_USAGE:
                reportScreen.setText(R.string.coll_past_data);
                break;
            case CONSTANTS.COLLECTING_CONTINUOUS_DATA:
                reportScreen.setText(R.string.coll_active_phone_data);
                break;
            case CONSTANTS.FILE_SENT:
                reportScreen.setText(R.string.study_fin);
                break;
        }
        findViewById(R.id.btnReadQR).setOnClickListener(this);
        findViewById(R.id.btnSeePassword).setOnClickListener(this);
        findViewById(R.id.btnEmail).setOnClickListener(this);
        updateUI();
    }

    private void updateUI(){
        Button passwordBtn = findViewById(R.id.btnSeePassword);

        if(QRCodeProvided()){
            Button email = findViewById(R.id.btnEmail);
            email.setVisibility(View.VISIBLE);
            Button QRReadBtn = findViewById(R.id.btnReadQR);
            QRReadBtn.setVisibility(View.GONE);
            passwordBtn.setVisibility(View.VISIBLE);
        }else{
            passwordBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnEmail) {
            if (sharedPreferences.getBoolean("permissions provided", false)) {
                collectFromDataSource();
            } else {
                if (QRCodeProvided()) {
                    Timber.i("QR code has already been scanned, continue with setup");
                    continueWithSetUp();
                } else {
                    if (this.checkCallingOrSelfPermission("android.permission.CAMERA")!=PackageManager.PERMISSION_GRANTED) {
                        Timber.i("Camera permission has NOT been given, asking for it now");
                        dealWithPermission.determinePermissionThatAreEssential(new String[]{Manifest.permission.CAMERA});
                    } else {
                        Timber.i("Okay, camera permission already given");
                        postAlert.customiseMessage(99, "NA", getString(R.string.title_next),
                                getString(R.string.pls_press_qr_code), "NA");
                    }
                }
            }
        } else if (view.getId() == R.id.btnReadQR) {
            startActivityForResult(new Intent(this,QRScanner.class),
                    CONSTANTS.QR_CODE_ACTIVITY);
        } else if (view.getId() == R.id.btnSeePassword) {
            informUserOnPassword("noWhere");
        } else {
            throw new IllegalStateException("Unexpected value: " + view.getId());
        }
    }

    private void initializeError() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree(){
                @NonNull
                @Override
                protected String createStackElementTag(@NotNull StackTraceElement element) {
                    return String.format("C:%s:%s",super.createStackElementTag(element),
                            element.getLineNumber());
                }
            });
            Timber.i("check if phone restarted: ");
            Timber.i("Phone restarted previously: %s",
                    sharedPreferences.getBoolean("restarted", false));
        }
    }

    private void initializeClasses() {
        postAlert = new PostAlert(this);
        dealWithPermission = new DealWithPermission(this, postAlert);
    }

    private void initializeLocalReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, new IntentFilter("alertDialogResponse"));
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("progressUpdate"));
    }

    private boolean QRCodeProvided(){
        return !sharedPreferences.getString("instructions from QR", "instructions not initialized").equals("instructions not initialized");
    }

    private void continueWithSetUp() {
        Timber.i("Continuing with setup");
        if (this.checkCallingOrSelfPermission("android.permission.CAMERA") ==
                PackageManager.PERMISSION_GRANTED){
            Gson gson = new Gson();
            qrInput = gson.fromJson(sharedPreferences.getString("instructions from QR",
                    "instructions not initialized"), QRInput.class);
            if(!securePreferences.getBoolean("password generated", false)){
                    sendMessage(1);
            } else {
                Timber.i("call to determine position");
                dealWithPermission.determinePermissionThatAreEssential(establishPermissionsToRequest());
            }
        } else {
            Timber.i("requesting approval of camera permission");
            dealWithPermission.determinePermissionThatAreEssential(new String[]{Manifest.permission.CAMERA});
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(QRCodeProvided()){
            dealWithPermission.determinePermissionThatAreEssential(establishPermissionsToRequest());
        } else {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                postAlert.customiseMessage(99, "NA", getString(R.string.title_next),
                        getString(R.string.pls_press_qr_code), "NA");
            } else {
                continueWithSetUp();
            }
        }
    }

    /**
     * DIRECTLY RELATED TO INTERPRETING QR-CODE FROM RESEARCHER
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String result;
        resultCode = 0;
        if(data!=null){
            switch (resultCode){
                case 0: //todo - check logic, as resultCode is always 0?
                    result = data.getStringExtra("result");
                    if(result!=null){
                        try {
                            commitKsonToFile(result);
                        } catch (Exception e) {
                            sendMessage(5);
                        }
                        updateUI();
                    }else{
                        Timber.i( "QR is Kson, but result returned is null");
                    }
                    break;
                case 1:
                    result = data.getStringExtra("result");
                    if (result!=null) {
                        Timber.i(" result is not Kson, string is: %s", result);
                    }else{
                        Timber.i(" result is not Kson, but result returned is null");
                    }
                    break;
            }
        } else {
            if (requestCode == CONSTANTS.GENERAL_USAGE_PERMISSION_REQUEST){
                Timber.i("requesting usage permission");
                dealWithPermission.determinePermissionThatAreEssential(establishPermissionsToRequest());
            }
            Timber.i("QR result: %s", resultCode);
        }
    }

    private void commitKsonToFile(String input) throws Exception {
        new QRInputHandler(input, this);
        final String messageToReport =  sharedPreferences.getString("instructions from QR", "instructions not initialized");
        if(!messageToReport.equals("instructions not initialized")){
            Timber.i("input from QR: %s", messageToReport);
            Gson gson = new Gson();
            qrInput = gson.fromJson(messageToReport, QRInput.class);
            sendMessage(1);
        }else{
            Timber.e("Issue deciphering QR configuration");
        }
    }

    private void sendMessage(int message) {
        switch (message){
            case 1:
                //last warning before collecting data
                postAlert.customiseMessage(1, "NA", getString(R.string.title_attention),
                        getString(R.string.app_info), "alertDialogResponse");
                break;
            case 2:
                StringBuilder whatTheAppDoes = new StringBuilder();

                if(qrInput.dataSources.containsKey("contextual")){
                    whatTheAppDoes.append("\n\n").append(getString(R.string.func_context1));
                    if(qrInput.contextualDataSources.contains("installed")){
                        whatTheAppDoes.append("\n").append(getString(R.string.func_context2));
                    }
                    if(qrInput.contextualDataSources.contains("permission")){
                        whatTheAppDoes.append("\n").append(getString(R.string.func_context3));
                    }
                    if(qrInput.contextualDataSources.contains("response")){
                        whatTheAppDoes.append("\n").append(getString(R.string.func_context4));
                    }
                }
                if(qrInput.dataSources.containsKey("continuous")){
                    whatTheAppDoes.append("\n\n").append(getString(R.string.func_continuous1));
                    whatTheAppDoes.append("\n").append(getString(R.string.func_continuous2));
                    if(qrInput.continuousDataSource.contains("screen")){
                        whatTheAppDoes.append("\n").append(getString(R.string.func_continuous3));
                    }
                    if(qrInput.continuousDataSource.contains("app")){
                        whatTheAppDoes.append("\n").append(getString(R.string.func_continuous4));
                    }
                    if(qrInput.continuousDataSource.contains("notification")){
                        whatTheAppDoes.append("\n").append(getString(R.string.func_continuous5));
                    }
                    if(qrInput.continuousDataSource.contains("installed")){
                        whatTheAppDoes.append("\n").append(getString(R.string.func_continuous6));
                    }
                }
                if(qrInput.dataSources.containsKey("usage")){
                    whatTheAppDoes.append("\n\n").append(getString(R.string.func_past1)).
                            append(" ").append(qrInput.daysToMonitor).append(" ").
                            append(getString(R.string.func_past2)).append("\n").
                            append(getString(R.string.func_past3)).append("\n").
                            append(getString(R.string.func_past4));
                }
                postAlert.customiseMessage(2, "NA", getString(R.string.title_log_request),
                        getString(R.string.func_start) + whatTheAppDoes + "\n" +  "\n"
                                + getString(R.string.func_end), "alertDialogResponse");
                break;
            case 3:
                postAlert.customiseMessage(3, "NA", getString(R.string.title_data_sec),
                        getString(R.string.data_security),  "alertDialogResponse");
                break;
            case 4:
                StringBuilder permissionsToBeRequested = new StringBuilder();
                if(qrInput.dataSources.containsKey("usage") || qrInput.continuousDataSource.contains("app")){
                    permissionsToBeRequested.append("\n\n").append(getString(R.string.usage_perm));
                }
                if(qrInput.continuousDataSource.contains("notification")){
                    permissionsToBeRequested.append("\n").append(getString(R.string.note_list_perm));
                }
                if(permissionsToBeRequested.length() == 0){
                    permissionsToBeRequested.append("\n").append(getString(R.string.no_perm_req));
                }
                postAlert.customiseMessage(4, "NA", getString(R.string.title_perm_req),
                        getString(R.string.give_perms) + permissionsToBeRequested,
                        "alertDialogResponse");
                break;
            case 5:
                postAlert.customiseMessage(5, "NA", getString(R.string.title_problem_qr),
                        getString(R.string.problem_read_qr),""); //should this not also be alertDialogResponse?
        }
    }

    private String[] establishPermissionsToRequest() {
        String[] permissionsNeeded = {"NA","NA"};
        if(qrInput.dataSources.containsKey("usage") || qrInput.continuousDataSource.contains("app")){
                permissionsNeeded[0] ="usage";
        }

        if(qrInput.continuousDataSource.contains("notification")){
            permissionsNeeded[1] = "notification";
        }
        return permissionsNeeded;
    }

    private void requestUsagePermissions(String permission){
        if(permission.equals("usage")){
            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                    CONSTANTS.GENERAL_USAGE_PERMISSION_REQUEST);
        } else if (permission.equals("notification")) {
            //request notification permission
            startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
                   CONSTANTS.GENERAL_USAGE_PERMISSION_REQUEST);
        } else {
            Timber.i("All permission granted");
        }
    }

    /**
     * DIRECTLY RELATED TO HANDLING RESPONSE TO ALERT DIALOGS
     */
    private final BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                int message = bundle.getInt("messageID", 100);
                String permission = bundle.getString("permissionToRequest", "NA");
                if (permission.equals("NA")) {
                    if (!securePreferences.getBoolean("password generated", false)) {
                        securePreferences.edit().putLong("message" + message, System.currentTimeMillis()).apply();
                        Timber.i("result from securePref message%d: %d", message,
                                securePreferences.getLong("message" + message, 1L));
                    }
                    switch (message) {
                        case 0:
                            sharedPreferences.edit().putBoolean("informed user", true).apply();
                            continueWithSetUp();
                            break;
                        case 1:
                        case 2:
                        case 3:
                            sendMessage(++message);
                            break;
                        case 4:
                            try {
                                generate_password();
                                informUserOnPassword("alertDialogResponse");
                            } catch (Exception e) {
                                Timber.e(e);
                            }
                            break;
                        case 5:
                            dealWithPermission.determinePermissionThatAreEssential(establishPermissionsToRequest());
                            break;
                        case CONSTANTS.ALL_PERMISSIONS_GRANTED:
                            sharedPreferences.edit().putBoolean("permissions provided", true).apply();
                            updateUI();
                            generatePlanForDataCollection();
                            break;
                        case CONSTANTS.SEND_EMAIL:
                            sendEmail();
                            break;
                    }

                } else {
                    requestUsagePermissions(permission);
                }
            }
        }
    };

    public void generate_password() {
        String pass = GeneratePassword.randomString(12);
        Timber.i("generated password: %s", pass);

        securePreferences.edit()
               .putString("password", pass)
               .putBoolean("password generated", true)
               .apply();
    }

    final BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                Timber.i("Progress update received");
                if(bundle != null) {
                    int update = bundle.getInt("progress");
                    Timber.i("Progress update received: progress: %d", update);
                    progressBar.setProgress(update);
                }
        }
    };

    private void informUserOnPassword(String whereToSend) {
        Button password = findViewById(R.id.btnSeePassword);
        password.setVisibility(View.VISIBLE);
        postAlert.customiseMessage(5, "NA", getString(R.string.pwd),
                "Your password is " + securePreferences.getString("password",
                        "not generated yet"), whereToSend);
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
        final int currentActivityTarget = sharedPreferences.getInt("currentActivity", 0);
        if(qrInput == null){
            Gson gson = new Gson();
            qrInput = gson.fromJson(sharedPreferences.getString("instructions from QR", "instructions not initialized"), QRInput.class);
        }
        for(String dataSource: qrInput.dataSources.keySet()){
            Integer currentDataSource = qrInput.dataSources.get(dataSource);
            Timber.i("data source: %s - currentDataSource: %d - currentActivityTarget: %d", dataSource, currentDataSource, currentActivityTarget);
            if(currentDataSource!= null){
                if(currentDataSource == currentActivityTarget){
                    Timber.i("next data source: %s", dataSource);
                    nextDataSource = dataSource;
                    break;
                }
            }
        }
        StoreInPdf storeInPdf;
        Resources resources = getResources();
        switch (nextDataSource){
            case "contextual":
                progressBar.setVisibility(View.VISIBLE);
                reportScreen.setText(resources.getString(R.string.context_data_collection));
                Timber.i(resources.getString(R.string.context_data_collection));
                sharedPreferences.edit().putInt("data being collected", CONSTANTS.COLLECTING_CONTEXTUAL_DATA).apply();
                storeInPdf = new StoreInPdf(this);
                storeInPdf.execute(this, securePreferences.getString("password", "not real password"),
                        CONSTANTS.RETURN_CONTEXT_TYPE(qrInput.contextualDataSources), CONSTANTS.COLLECTING_CONTEXTUAL_DATA);
                break;
            case "usage":
                progressBar.setVisibility(View.VISIBLE);
                reportScreen.setText(resources.getString(R.string.usage_data_collection));
                Timber.i(resources.getString(R.string.usage_data_collection));
                sharedPreferences.edit().putInt("data being collected", CONSTANTS.COLLECTING_PAST_USAGE).apply();
                storeInPdf = new StoreInPdf(this);
                storeInPdf.execute(this, securePreferences.getString("password", "not real password"),
                        qrInput.daysToMonitor, CONSTANTS.COLLECTING_PAST_USAGE);
                break;
            case "continuous":
                progressBar.setVisibility(View.INVISIBLE);
                reportScreen.setText(resources.getString(R.string.continuous_data_collection));
                Timber.i(resources.getString(R.string.continuous_data_collection));
                if(sharedPreferences.getBoolean("background logging underway", false)){
                    Timber.i("attempting to package continuous logger");
                    storeInPdf = new StoreInPdf(this);
                    storeInPdf.execute(this, securePreferences.getString("password", "not real password"),
                            CONSTANTS.READY_FOR_EMAIL, CONSTANTS.COLLECTING_CONTINUOUS_DATA);
                }else{
                    startBackgroundLogging();
                }
                break;
            case "finish":
                progressBar.setVisibility(View.GONE);
                postAlert.customiseMessage(CONSTANTS.SEND_EMAIL, "NA", getString(R.string.title_send_email),
                        getString(R.string.send_data_away), "alertDialogResponse");
                break;
            default:
                Timber.i("next data source not established");
                break;
        }
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LoggerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startBackgroundLogging() {
        Timber.i("continuous is the next data source");
        sharedPreferences.edit().putInt("data being collected", CONSTANTS.COLLECTING_CONTINUOUS_DATA).putBoolean("background logging underway", true).apply();
        Intent toStartService;
        Bundle bundle = new Bundle();
        bundle.putString("password", securePreferences.getString("password", "not actual password"));
        bundle.putBoolean("restart", false);
        bundle.putBoolean("screenLog", qrInput.continuousDataSource.contains("screen"));
        bundle.putBoolean("appLog", qrInput.continuousDataSource.contains("app"));
        bundle.putBoolean("appChanges", qrInput.continuousDataSource.contains("installed"));
        if(!qrInput.continuousDataSource.contains("notification")){
            toStartService = new Intent(this, LoggerService.class);
            toStartService.putExtras(bundle);
            if(!isMyServiceRunning()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(toStartService);
                } else {
                    startService(toStartService);
                }
            }
        } else {
            toStartService = new Intent(this, LoggerWithNotesService.class);
            toStartService.putExtras(bundle);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(toStartService);
            } else {
                startService(toStartService);
            }
        }
    }

    /**
     * RESPOND TO DATA COLLECTION INITIATIVES
     */
    @Override
    public void processFinish(DataCollectionResult output) {
        Timber.i("data collection result. DataRetrieved: %d - Success: %s", output.dataRetrieved, output.success);
        switch (output.dataSource){
            case CONSTANTS.COLLECTING_CONTEXTUAL_DATA:
                if (output.task == CONSTANTS.PUTTING_CONTEXTUAL_DATA_IN_PDF){
                    Timber.i("Completed storage of contextual data in Pdf");
                    sharedPreferences.edit().putInt("currentActivity", (1+sharedPreferences.getInt("currentActivity", 0))).apply();
                    collectFromDataSource();
                }
                break;
            case CONSTANTS.COLLECTING_PAST_USAGE:
                Timber.i("Finished collecting past usage info");
                sharedPreferences.edit().putInt("currentActivity", (1+sharedPreferences.getInt("currentActivity", 0))).apply();
                collectFromDataSource();
                break;
            case CONSTANTS.COLLECTING_CONTINUOUS_DATA:
                Timber.i("Finished collecting continuous usage info");
                sharedPreferences.edit().putInt("currentActivity", (1+sharedPreferences.getInt("currentActivity", 0))).apply();
                collectFromDataSource();
                break;
        }
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("text/plain");

        //getting directory for internal files
        String directory = (this.getFilesDir() + File.separator);

        //initializing files reference
        File
                contextFile = new File(directory + File.separator + CONSTANTS.CONTEXT_FILE),
                usageEvents = new File(directory + File.separator + CONSTANTS.USAGE_FILE),
                continuous = new File(directory + File.separator + CONSTANTS.CONTINUOUS_FILE);

        //list of files to be uploaded
        ArrayList<Uri> files = new ArrayList<>();

        //if target files are identified to exist then they are packages into the attachments of the email
        try {
            if (contextFile.exists()) {
                files.add(FileProvider.getUriForFile(this,
                        "psych.sensorlab.usagelogger2.fileprovider", contextFile));
            }

            if (usageEvents.exists()) {
                files.add(FileProvider.getUriForFile(this,
                        "psych.sensorlab.usagelogger2.fileprovider", usageEvents));
            }

            if (continuous.exists()) {
                files.add(FileProvider.getUriForFile(this,
                        "psych.sensorlab.usagelogger2.fileprovider", continuous));
            }

            if (files.size() > 0) {
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                sharedPreferences.edit().putInt("data collected info", CONSTANTS.FILE_SENT).apply();
                startActivity(intent);

            } else {
                Timber.e("No files to upload");
            }
        }
        catch (Exception e){
            Timber.e(e);
        }
    }
}
