package psych.sensorlab.usagelogger2;

import androidx.annotation.NonNull;

import java.util.Set;

class CONSTANTS {

    static int RETURN_CONTEXT_TYPE(@NonNull Set<String> input){
        if (input.contains("installed")) {
            if (input.contains("permission")) {
                if (input.contains("response")) {
                    return CONSTANTS.INSTALLED_AND_PERMISSION_AND_RESPONSE;
                } else {
                    return CONSTANTS.INSTALLED_AND_PERMISSION;
                }
            } else {
                if (input.contains("response")) {
                    return CONSTANTS.INSTALLED_AND_RESPONSE;
                } else {
                    return CONSTANTS.ONLY_INSTALLED;
                }
            }
        } else if (input.contains("permission")) {
            if (input.contains("response")) {
                return CONSTANTS.RESPONSE_AND_PERMISSION;
            } else {
                return CONSTANTS.ONLY_PERMISSION;
            }
        } else if (input.contains("response")) {
            return CONSTANTS.ONLY_RESPONSE;
        } else {
            return CONSTANTS.NO_DATA_COLLECTED;
        }
    }

    public static String STARTFOREGROUND_ACTION = "psych.sensorlab.usagelogger2.action.startforeground";
    public static String STOPFOREGROUND_ACTION = "psych.sensorlab.usagelogger2.action.stopforeground";

    static final String
            CONTEXT_FILE = "context.pdf",
            USAGE_FILE = "usage.pdf",
            CONTINUOUS_FILE = "continuous.pdf",
            CONTINUOUS_DB_TABLE = "continuous_table",
            CONTINUOUS_DB_NAME = "continuous.db";

    static final int
            //broadcast receiver message IDs
            INFORM_USER = 0,
            QR_CODE_ACTIVITY = 1,
            ALERT_DIALOG_CAMERA_PERMISSION = 2,
            ALERT_DIALOG_USAGE_PERMISSION = 3,
            ALERT_DIALOG_NOTIFICATION_PERMISSION = 4,
            GENERAL_USAGE_PERMISSION_REQUEST = 5,
            ALL_PERMISSIONS_GRANTED = 6,
            SEND_EMAIL = 7,

            ALERT_ATTENTION = 1,
            ALERT_INFO_COLLECTED = 2,
            ALERT_PHONE_ROOTED = 3,
            ALERT_PERMISSIONS_NEEDED = 4,
            ALERT_PROBLEM_QRCODE = 5,

            PERMISSION_NOT_ASSESSED = 1,
            COLLECTING_CONTEXTUAL_DATA = 1,
            COLLECTING_PAST_USAGE = 2,
            COLLECTING_CONTINUOUS_DATA = 3,
            READY_FOR_EMAIL = 4,
            FILE_SENT = 5,

            NO_DATA_COLLECTED = 1,
            ONLY_RESPONSE = 2,
            ONLY_PERMISSION = 3,
            RESPONSE_AND_PERMISSION = 4,
            ONLY_INSTALLED = 5,
            INSTALLED_AND_RESPONSE = 6,
            INSTALLED_AND_PERMISSION = 7,
            INSTALLED_AND_PERMISSION_AND_RESPONSE = 8,

            PUTTING_CONTEXTUAL_DATA_IN_PDF = 1,
            PUTTING_USAGE_DATA_IN_PDF = 2,
            ERROR_EXPERIENCED_IN_ASYNC = 3,
            PUTTING_CONTINUOUS_DATA_IN_PDF = 4,

            LOGGING_INTERVAL_MS = 1000,
            DB_VERSION = 1;
}
