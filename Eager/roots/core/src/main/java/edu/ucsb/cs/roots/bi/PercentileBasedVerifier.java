package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.anomaly.AnomalyLog;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class PercentileBasedVerifier {

    private final Map<Long,List<ApplicationRequest>> requests;
    private final Bottleneck bottleneck;
    private final AnomalyLog anomalyLog;
    private final ImmutableList<ApiCall> apiCalls;
    private final double percentile;

    public PercentileBasedVerifier(Map<Long, List<ApplicationRequest>> requests,
                                   Bottleneck bottleneck, AnomalyLog anomalyLog,
                                   ImmutableList<ApiCall> apiCalls, double percentile) {
        this.requests = requests;
        this.bottleneck = bottleneck;
        this.anomalyLog = anomalyLog;
        this.apiCalls = apiCalls;
        this.percentile = percentile;
    }

    public void verify() {
        Anomaly anomaly = bottleneck.getAnomaly();
        Date onsetTime = bottleneck.getOnsetTime();
        if (onsetTime == null) {
            anomalyLog.info(anomaly, "Onset time not available for verification");
            return;
        }

        ImmutableList<DescriptiveStatistics> stats = PercentileBasedFinder.initStatistics(apiCalls.size());
        for (long timestamp : requests.keySet()) {
            if (timestamp > onsetTime.getTime()) {
                break;
            }
            List<ApplicationRequest> batch = requests.get(timestamp);
            batch.forEach(r -> {
                int[] timeValues = PercentileBasedFinder.getResponseTimeVector(r);
                for (int i = 0; i < timeValues.length; i++) {
                    stats.get(i).addValue(timeValues[i]);
                }
            });
        }

        for (int i = 0; i < stats.size(); i++) {
            double value = stats.get(i).getPercentile(percentile);
            String apiCall;
            if (i != stats.size() - 1) {
                apiCall = apiCalls.get(i).name();
            } else {
                apiCall = "LOCAL";
            }
            anomalyLog.info(anomaly, "{}p for {}: {}", percentile, apiCall, value);
        }
    }

}
