package psych.sensorlab.usagelogger2;

import java.util.Set;

class CONSTANTS {

    static int RETURN_CONTEXT_TYPE(Set input){
        if(input.contains("installed")){
            if(input.contains("permission")){
                if(input.contains("response")){
                    return CONSTANTS.INSTALLED_AND_PERMISSION_AND_RESPONSE;
                } else {
                    return CONSTANTS.INSTALLED_AND_PERMISSION;
                }
            }else{
                if(input.contains("response")){
                    return CONSTANTS.INSTALLED_AND_RESPONSE;
                } else {
                    return CONSTANTS.ONLY_INSTALLED;
                }
            }
        } else if(input.contains("permission")){
            if(input.contains("response")){
                return CONSTANTS.RESPONSE_AND_PERMISSION;
            } else {
                return CONSTANTS.ONLY_PERMISSION;
            }
        } else if(input.contains("response")){
            return CONSTANTS.ONLY_RESPONSE;
        } else {
            return CONSTANTS.NO_DATA_COLLECTED;
        }
    }

    static final String
    CONTEXT_FILE = "contextual.pdf",
    USAGE_FILE = "past_usage.pdf",
    CONTINUOUS_FILE = "continuous.pdf";

    static final int
            QR_CODE_ACTIVITY = 1,
            ALERT_DIALOG_CAMERA_PERMISSION = 2,
            ALERT_DIALOG_USAGE_PERMISSION = 3,
            ALERT_DIALOG_NOTIFICATION_PERMISSION = 4,
            GENERAL_USAGE_PERMISSION_REQUEST = 5,
            ALL_PERMISSIONS_GRANTED = 6,
            SEND_EMAIL = 7,

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
            PUTTING_CONTINUOUS_DATA_IN_PDF = 4;
}
