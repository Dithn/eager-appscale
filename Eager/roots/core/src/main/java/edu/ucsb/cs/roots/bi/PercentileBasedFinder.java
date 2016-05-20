package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class PercentileBasedFinder extends BottleneckFinder {

    private final double percentile;

    public PercentileBasedFinder(RootsEnvironment environment, double percentile) {
        super(environment);
        checkArgument(percentile > 0 && percentile < 1, "Percentile must be in the interval (0,1)");
        this.percentile = percentile;
    }

    @Override
    void analyze(Anomaly anomaly) {
        long history = anomaly.getEnd() - anomaly.getStart();
        long start = anomaly.getEnd() - 3 * history;
        DataStore ds = environment.getDataStoreService().get(anomaly.getDataStore());
        try {
            ImmutableList<ApplicationRequest> requests = ds.getRequestInfo(
                    anomaly.getApplication(), anomaly.getOperation(), start, anomaly.getEnd());
            log.debug("Received {} requests for analysis", requests.size());
            Map<String,List<ApplicationRequest>> perPathRequests = requests.stream().collect(
                    Collectors.groupingBy(ApplicationRequest::getPathAsString));
            perPathRequests.forEach((path,list) -> analyze(anomaly, path, list));
        } catch (DataStoreException e) {
            anomalyLog.error(anomaly, "Error while retrieving API call data", e);
        }
    }

    private void analyze(Anomaly anomaly, String path, List<ApplicationRequest> requests) {
        ImmutableList<ApiCall> apiCalls = requests.get(0).getApiCalls();
        double[] percentiles = computePercentiles(anomaly, requests);
        if (log.isDebugEnabled()) {
            log.debug("Percentiles: {}", Arrays.toString(percentiles));
        }
        requests.stream().filter(r -> r.getTimestamp() >= anomaly.getStart())
                .forEach(r -> checkForAnomalies(r, apiCalls.size(), percentiles, anomaly, path));
    }

    private void checkForAnomalies(ApplicationRequest r, int apiCalls, double[] percentiles,
                                   Anomaly anomaly, String path) {
        int total = 0;
        for (int i = 0; i < apiCalls; i++) {
            int timeElapsed = r.getApiCalls().get(i).getTimeElapsed();
            total += timeElapsed;
            if (timeElapsed > percentiles[i]) {
                anomalyLog.info(
                        anomaly, "Anomalous API call execution in path {} at {}: {} [> {} ({}p)]",
                        path, r.getApiCalls().get(i).name(), timeElapsed, percentiles[i],
                        percentile);
            }
        }

        int localExecTime = r.getResponseTime() - total;
        if (localExecTime > percentiles[apiCalls]) {
            anomalyLog.info(
                    anomaly, "Anomalous local execution in path {} at LOCAL: {} [> {} ({}p)]",
                    path, localExecTime, percentiles[apiCalls], percentile);
        }
    }

    private double[] computePercentiles(Anomaly anomaly, List<ApplicationRequest> requests) {
        ImmutableList<ApiCall> apiCalls = requests.get(0).getApiCalls();
        ImmutableList<DescriptiveStatistics> stats = initStatistics(apiCalls);
        requests.stream().filter(r -> r.getTimestamp() < anomaly.getStart()).forEach(r -> {
            int total = 0;
            for (int i = 0; i < apiCalls.size(); i++) {
                int timeElapsed = r.getApiCalls().get(i).getTimeElapsed();
                stats.get(i).addValue(timeElapsed);
                total += timeElapsed;
            }
            stats.get(apiCalls.size()).addValue(r.getResponseTime() - total);
        });

        if (log.isDebugEnabled()) {
            log.debug("Percentiles computed using {} data points", stats.get(0).getN());
        }
        return stats.stream().mapToDouble(s -> s.getPercentile(percentile)).toArray();
    }

    private ImmutableList<DescriptiveStatistics> initStatistics(List<ApiCall> apiCalls) {
        int size = apiCalls.size() + 1;
        ImmutableList.Builder<DescriptiveStatistics> stats = ImmutableList.builder();
        for (int i = 0; i < size; i++) {
            stats.add(new DescriptiveStatistics());
        }
        return stats.build();
    }
}
