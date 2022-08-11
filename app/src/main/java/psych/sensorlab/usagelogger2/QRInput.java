package psych.sensorlab.usagelogger2;

import java.util.HashMap;
import java.util.Set;

class QRInput {

    final HashMap<String, Integer> dataSources;
    final Set<String> contextualDataSources, continuousDataSource;
    final int daysToMonitor;

    QRInput(HashMap<String, Integer> dataSources, Set<String> contextualDataSources, int daysToMonitor, Set<String> continuousDataSources) {
        this.dataSources = dataSources;
        this.contextualDataSources = contextualDataSources;
        this.continuousDataSource = continuousDataSources;
        this.daysToMonitor = daysToMonitor;
    }
}
