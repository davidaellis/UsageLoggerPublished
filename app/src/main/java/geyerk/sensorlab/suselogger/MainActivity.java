package geyerk.sensorlab.suselogger;

import android.Manifest;
import android.app.Activity;
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
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

import at.favre.lib.armadillo.Armadillo;
import timber.log.Timber;

public class MainActivity extends Activity implements View.OnClickListener, AsyncResult {

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
            postAlert.customiseMessage(0, "NA", "What is Usage logger", "This app is for researching for scientific research, if you are not participating in scientific research please uninstall this app." , "alertDialogResponse");
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
        switch (sharedPreferences.getInt("data being collected", 0)){
            case CONSTANTS.COLLECTING_CONTEXTUAL_DATA:
                reportScreen.setText("Currently collecting context data");
                break;
            case CONSTANTS.COLLECTING_PAST_USAGE:
                reportScreen.setText("Currently collecting past usage data");
                break;
            case CONSTANTS.COLLECTING_PROSPECTIVE_DATA:
                reportScreen.setText("Collecting active phone usage data");
                break;
            case CONSTANTS.FILE_SENT:
                reportScreen.setText("Study is complete");
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
        switch (view.getId()){
            case R.id.btnReadQR:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1){
                    startActivityForResult(new Intent(this, GoogleQRScanner.class), CONSTANTS.QR_CODE_ACTIVITY);
                }else{
                    startActivityForResult(new Intent(this, GoogleQRScanner.class), CONSTANTS.QR_CODE_ACTIVITY);
                }

                break;
            case R.id.btnSeePassword:
                informUserOnPassword("noWhere");
                break;
            case R.id.btnEmail:
                if(sharedPreferences.getBoolean("permissions provided", false)){
                    collectFromDataSource();
                }else{
                    if(this.checkCallingOrSelfPermission("android.permission.CAMERA")!= PackageManager.PERMISSION_GRANTED){
                        continueWithSetUp();
                    }else {
                        postAlert.customiseMessage(99, "NA", "Next step", "Thank you for providing the permission. Now please press on Read QR code and use your camera to scan a QR code provided to you as part of the study.", "NA");
                    }
                }
                break;
        }
    }

    private void initializeError() {
        if(BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree(){
                @Override
                protected @org.jetbrains.annotations.Nullable String createStackElementTag(@NotNull StackTraceElement element) {
                    return String.format("C:%s:%s",super.createStackElementTag(element), element.getLineNumber());
                }
            });
            Timber.i("check if phone restarted: ");
            Timber.i("Phone restarted previously: %s", sharedPreferences.getBoolean("restarted", false));
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
        Timber.i("Continuing with set up");
        if(this.checkCallingOrSelfPermission("android.permission.CAMERA")== PackageManager.PERMISSION_GRANTED){
            Gson gson = new Gson();
            qrInput = gson.fromJson(sharedPreferences.getString("instructions from QR", "instructions not initialized"), QRInput.class);
            if(!securePreferences.getBoolean("password generated", false)){
                    sendMessage(1);
            }else{
                Timber.i("call to determine position");
                dealWithPermission.determinePermissionThatAreEssential(establishPermissionsToRequest());
            }
        }else{
            Timber.i("requesting approval of camera permission");
            dealWithPermission.determinePermissionThatAreEssential(new String[]{Manifest.permission.CAMERA});
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(QRCodeProvided()){
            dealWithPermission.determinePermissionThatAreEssential(establishPermissionsToRequest());
        }else{
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                postAlert.customiseMessage(99, "NA", "Next step", "Please press on Read QR code and use your camera to scan a QR code", "NA");
            }else{
                continueWithSetUp();
            }

        }
    }

    /**
     * DIRECTLY RELATED TO INTERPRETING QR CODE FROM RESEARCHER
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String result;
        resultCode = 0;
        if(data!=null){
            switch (resultCode){
                case 0:
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
                    if(result!=null){
                        Timber.i( " result is not Kson, string is: %s", result);
                    }else{
                        Timber.i("result is not Kson, but result returned is null");
                    }
                    break;
            }
        }else{
            if(requestCode == CONSTANTS.GENERAL_USAGE_PERMISSION_REQUEST){
                Timber.i("requesting usage permission");
                dealWithPermission.determinePermissionThatAreEssential(establishPermissionsToRequest());
            }
            Timber.i("QR result: %s", resultCode);
        }
    }

    private void commitKsonToFile(String input) throws Exception {
        new QrInputHandler(input, this);
        final String messageToReport =  sharedPreferences.getString("instructions from QR", "instructions not initialized");
        if(!messageToReport.equals("instructions not initialized")){
            Timber.i("input from QR: %s", messageToReport);
            Gson gson = new Gson();
            qrInput = gson.fromJson(messageToReport, QRInput.class);
            sendMessage(1);
        }else{
            Timber.e("Issue deciphering QR instructions");
        }
    }

    private void sendMessage(int message) {
        switch (message){
            case 1:
                postAlert.customiseMessage(1, "NA", "What is Usage Logger", "Usage logger is an app that helps scientists identify what you are using your phone for. Please do not use this app for any other purpose, if you have been asked to use this app for purposes unrelated to scientific research then please uninstall it now. At this point no data has been collected.",  "alertDialogResponse");
                break;
            case 2:
                StringBuilder whatTheAppDoes = new StringBuilder();


                if(qrInput.dataSources.keySet().contains("contextual")){
                    if(qrInput.contextualDataSources.contains("installed")){
                        whatTheAppDoes.append("\n").append("- What apps are installed on your phone.");
                    }
                    if(qrInput.contextualDataSources.contains("permission")){
                        whatTheAppDoes.append("\n").append("- What permissions that the installed apps on your phone request.");
                    }
                    if(qrInput.contextualDataSources.contains("response")){
                        whatTheAppDoes.append("\n").append("- How you have previously responded to app's request for permission requests");
                    }
                }
                if(qrInput.dataSources.keySet().contains("usage")){
                    whatTheAppDoes.append("\n").append("- What you have previously used your phone for across the past ").append(qrInput.daysToMonitor).append("days This will include: when the screen was used and when apps were used. No information will be collected on what you did with the apps. However, inferences may be made regarding what you could have done with these apps");
                }
                if(qrInput.dataSources.keySet().contains("prospective")){
                    whatTheAppDoes.append("\n").append("- How you will use your phone in the future. This will include: ");
                    whatTheAppDoes.append("\n").append("- When the phone is off or on");
                    if(qrInput.prospectiveDataSource.contains("screen")){
                        whatTheAppDoes.append("\n").append("- When the screen is off or on");
                    }
                    if(qrInput.prospectiveDataSource.contains("app")){
                        whatTheAppDoes.append("\n").append("- What apps you use and when you use them");
                    }
                    if(qrInput.prospectiveDataSource.contains("notification")){
                        whatTheAppDoes.append("\n").append("- When an app sends you a notification and when you delete the notification");
                    }
                    if(qrInput.prospectiveDataSource.contains("installed")){
                        whatTheAppDoes.append("\n").append("- When you change the apps installed");
                    }
                }
                postAlert.customiseMessage(2, "NA", "What Usage logger has been requested to do", "The QR code that you previously scanned has informed this app to collect particular types of data. Including: " + whatTheAppDoes + "\n" +  "\n" + "Please press 'ok' if you are happy with this data being collected. If not contact the research and please be prepared to withdraw from the study if you feel it is necessary.", "alertDialogResponse");
                break;
            case 3:
                postAlert.customiseMessage(3, "NA", "Data security", "The developer of this app has done his upmost to secure the data (which has previously been described) that this app collects. This was done by encrypting the data, securely storing the encrypted data, restricting any access to the data, devising a complex security key to protect the encrypted and inaccessible data, securing this security key among other measures. However, it is important that you do not participate in this study if your phone is rooted (jailbroken) or that you believe that the normal Android security on your phone is compromised. Additionally, your data can not and will not be exported without you manually emailing/exporting the data yourself. I have made every effort to give you complete control of your data.",  "alertDialogResponse");
                break;
            case 4:
                StringBuilder permissionsToBeRequested = new StringBuilder();
                if(qrInput.dataSources.keySet().contains("usage") || qrInput.prospectiveDataSource.contains("app")){
                    permissionsToBeRequested.append("\n").append("Usage permission");
                }
                if(qrInput.prospectiveDataSource.contains("notification")){
                    permissionsToBeRequested.append("\n").append("Notification listing permission");
                }
                if(permissionsToBeRequested.length() == 0){
                    permissionsToBeRequested.append("\n").append("No permissions are required");
                }

                postAlert.customiseMessage(4, "NA", "Permissions required", "In order to participate you will have to provide the following permissions: " + permissionsToBeRequested, "alertDialogResponse");
                break;
            case 5:
                postAlert.customiseMessage(5, "NA", "Issue Reading QR code", "There was a problem reading the QR code. Can you please scan the code again","");
        }
    }

    private String[] establishPermissionsToRequest() {
        String[] permissionsNeeded = {"NA","NA"};
        if(qrInput.dataSources.keySet().contains("usage") || qrInput.prospectiveDataSource.contains("app")){
                permissionsNeeded[0] ="usage";
        }

        if(qrInput.prospectiveDataSource.contains("notification")){
            permissionsNeeded[1] = "notification";
        }
        return permissionsNeeded;
    }

    private void requestUsagePermissions(String permission){
        if(permission.equals("usage")){
            //request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), CONSTANTS.GENERAL_USAGE_PERMISSION_REQUEST);
            }
        }
        else if(permission.equals("notification")){
            //request notification permission
            startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), CONSTANTS.GENERAL_USAGE_PERMISSION_REQUEST);
        }else{
            Timber.i("All permission granted");
        }
    }

    /**
     * DIRECTLY RELATED TO HANDLING RESPONSE TO ALERT DIALOGS
     */

    private BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if(bundle!=null){
                int message = bundle.getInt("messageID",100);
                String permission = bundle.getString("permissionToRequest", "NA");
                if(permission.equals("NA")){
                    if(!securePreferences.getBoolean("password generated", false)){
                        securePreferences.edit().putLong("message" + message, System.currentTimeMillis()).apply();
                        Timber.i("result from securePref message%d: %d", message, securePreferences.getLong("message"+message, 1L));
                    }
                    switch (message){
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
                                generatePassword();
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

                }else{
                    requestUsagePermissions(permission);
                }
            }
        }
        private void generatePassword() throws Exception {

            Long[] randomValues = new Long[4];
            randomValues[0] = securePreferences.getLong("message1", 1L);
            randomValues[1] = securePreferences.getLong("message2", 1L);
            randomValues[2] = securePreferences.getLong("message3", 1L);
            randomValues[3] = securePreferences.getLong("message4", 1L);

            Timber.i("randomValues[0]: %d - randomValues[1]: %d - randomValues[2]: %d - randomValues[3]: %d", randomValues[0],randomValues[1],randomValues[2],randomValues[3]);
            if(
                    randomValues[0] == 1||
                            randomValues[1] == 1||
                            randomValues[2] == 1||
                            randomValues[3] == 1){
                throw new Exception("Did not appropriately generate messages from the securePreferences");
            }else{
                Ascii ascii = new Ascii();
                StringBuilder password = new StringBuilder();
                Random userRandomInitial = new Random(randomValues[0]);
                while (password.length() < 12){
                    Random userRandom ;
                    userRandom = new Random(randomValues[userRandomInitial.nextInt(4)]);

                    Random random = new Random(System.currentTimeMillis()* randomValues[userRandomInitial.nextInt(4)]);
                    Thread.sleep(userRandom.nextInt(40) +1);

                    int randomAsciiInt =  random.nextInt(93) + 33;
                    Character randomAsciiChar = ascii.returnAscii(randomAsciiInt);
                    if(randomAsciiChar != ' '){
                        Timber.i("returned from Ascii: %s", randomAsciiChar);
                        password.append(randomAsciiChar);
                    }
                }
                Arrays.fill(randomValues,null);
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 1);
                c.set(Calendar.MINUTE, 1);
                c.set(Calendar.MILLISECOND, 1);
                c.set(Calendar.SECOND, 1);

                securePreferences.edit()
                        .putString("password", String.valueOf(password))
                        .putBoolean("password generated", true)
                        .putLong("message1", c.getTimeInMillis())
                        .putLong("message2", c.getTimeInMillis())
                        .putLong("message3", c.getTimeInMillis())
                        .putLong("message4", c.getTimeInMillis())

                        .apply();
            }
        }
    };

    BroadcastReceiver progressReceiver = new BroadcastReceiver() {
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
        postAlert.customiseMessage(5, "NA", "Password", "Your password is " + securePreferences.getString("password", "password not generated yet"), whereToSend);
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
                storeInPdf.execute(this, securePreferences.getString("password", "not real password"), CONSTANTS.RETURN_CONTEXT_TYPE(qrInput.contextualDataSources), CONSTANTS.COLLECTING_CONTEXTUAL_DATA);
                break;
            case "usage":
                progressBar.setVisibility(View.VISIBLE);
                reportScreen.setText(resources.getString(R.string.past_data_collection));
                Timber.i(resources.getString(R.string.past_data_collection));
                sharedPreferences.edit().putInt("data being collected", CONSTANTS.COLLECTING_PAST_USAGE).apply();
                storeInPdf = new StoreInPdf(this);
                storeInPdf.execute(this, securePreferences.getString("password", "not real password"),  qrInput.daysToMonitor, CONSTANTS.COLLECTING_PAST_USAGE);
                break;
            case "prospective":
                progressBar.setVisibility(View.INVISIBLE);
                reportScreen.setText(resources.getString(R.string.prospective_Data_collection));
                Timber.i(resources.getString(R.string.prospective_Data_collection));
                if(sharedPreferences.getBoolean("background logging underway", false)){
                    Timber.i("attempting to package prospective logger");
                    storeInPdf = new StoreInPdf(this);
                    storeInPdf.execute(this, securePreferences.getString("password", "not real password"),  CONSTANTS.READY_FOR_EMAIL, CONSTANTS.COLLECTING_PROSPECTIVE_DATA);
                }else{
                    startBackgroundLogging();
                }
                break;
            case "finish":
                progressBar.setVisibility(View.GONE);
                postAlert.customiseMessage(CONSTANTS.SEND_EMAIL, "NA", "Send email?", "The only remaining step is to send off the data to the researcher via email. Do you wish to send the data at this point?", "alertDialogResponse");
                break;
            default:
                Timber.i("next data source not established");
                break;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startBackgroundLogging() {
        Timber.i("prospective is the next data source");
        sharedPreferences.edit().putInt("data being collected", CONSTANTS.COLLECTING_PROSPECTIVE_DATA).putBoolean("background logging underway", true).apply();
        Intent toStartService;
        Bundle bundle = new Bundle();
        bundle.putString("password", securePreferences.getString("password", "not actual password"));
        bundle.putBoolean("restart", false);
        bundle.putBoolean("screenLog", qrInput.prospectiveDataSource.contains("screen"));
        bundle.putBoolean("appLog", qrInput.prospectiveDataSource.contains("app"));
        bundle.putBoolean("appChanges", qrInput.prospectiveDataSource.contains("installed"));
        if(!qrInput.prospectiveDataSource.contains("notification")){
            toStartService = new Intent(this, ProspectiveLogger.class);
            toStartService.putExtras(bundle);
            if(!isMyServiceRunning(ProspectiveLogger.class)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(toStartService);
                } else {
                    startService(toStartService);
                }
            }
        }else{
            toStartService = new Intent(this, ProspectiveLoggerWithNotes.class);
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
                Timber.i("Finished collecting past usage");
                sharedPreferences.edit().putInt("currentActivity", (1+sharedPreferences.getInt("currentActivity", 0))).apply();
                collectFromDataSource();
                break;
            case CONSTANTS.COLLECTING_PROSPECTIVE_DATA:
                Timber.i("Finished collecting prospective usage");
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
                prospective = new File(directory + File.separator + CONSTANTS.PROSPECTIVE_FILE);

        //list of files to be uploaded
        ArrayList<Uri> files = new ArrayList<>();

        //if target files are identified to exist then they are packages into the attachments of the email
        try {
            if(contextFile.exists()){
                files.add(FileProvider.getUriForFile(this, "geyerk.sensorlab.suselogger.fileprovider", contextFile));
            }

            if(usageEvents.exists()){
                files.add(FileProvider.getUriForFile(this, "geyerk.sensorlab.suselogger.fileprovider", usageEvents));
            }

            if(prospective.exists()){
                files.add(FileProvider.getUriForFile(this, "geyerk.sensorlab.suselogger.fileprovider", prospective));
            }

            if(files.size()>0){
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                sharedPreferences.edit().putInt("data being collected", CONSTANTS.FILE_SENT).apply();
                startActivity(intent);

            }else{
                Timber.e("no files to upload");
            }
        }
        catch (Exception e){
            Timber.e(e);
        }
    }

}
