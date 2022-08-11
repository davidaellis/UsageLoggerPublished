package psych.sensorlab.usagelogger2;

class UsageRecord {

     final Long UNIXTime;
     final String app;
     final int event;

    UsageRecord(Long UNIXTime, String app, int event){
        this.UNIXTime = UNIXTime;
        this.app = app;
        this.event = event;
    }

}
