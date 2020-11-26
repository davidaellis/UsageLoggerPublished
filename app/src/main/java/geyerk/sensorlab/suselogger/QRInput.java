package geyerk.sensorlab.suselogger;

import java.util.HashMap;
import java.util.Set;

class QRInput {

    final HashMap<String, Integer> dataSources;
    final Set<String> contextualDataSources, prospectiveDataSource;
    final int daysToMonitor;

    QRInput(HashMap<String, Integer> dataSources, Set<String> contextualDataSources, int daysToMonitor, Set<String> prospectiveDataSources) {
        this.dataSources = dataSources;
        this.contextualDataSources = contextualDataSources;
        this.prospectiveDataSource = prospectiveDataSources;
        this.daysToMonitor = daysToMonitor;
    }
}
