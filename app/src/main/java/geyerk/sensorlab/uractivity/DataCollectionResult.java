package geyerk.sensorlab.uractivity;

class DataCollectionResult {
    final boolean success;
    final int dataRetrieved, dataSource, task;
    DataCollectionResult(boolean success, int dataRetrieved, int dataSource, int task){
        this.success = success;
        this.dataRetrieved = dataRetrieved;
        this.dataSource = dataSource;
        this.task = task;
    }
}
