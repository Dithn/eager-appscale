package edu.ucsb.cs.roots.bi;

import com.google.common.collect.ImmutableList;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.data.ApiCall;
import edu.ucsb.cs.roots.data.ApplicationRequest;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.DataStoreException;
import edu.ucsb.cs.roots.utils.ImmutableCollectors;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;

public class PercentileBasedFinder extends BottleneckFinder {

    private final double percentile;

    public PercentileBasedFinder(RootsEnvironment environment, double percentile) {
        super(environment);
        checkArgument(percentile > 0 && percentile < 100, "Percentile must be in the interval (0,100)");
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
        ImmutableList<ApplicationRequest> oldRequests = requests.stream()
                .filter(r -> r.getTimestamp() < anomaly.getStart())
                .collect(ImmutableCollectors.toList());
        if (oldRequests.isEmpty()) {
            log.warn("Insufficient data to compute percentiles");
            return;
        }
        double[] percentiles = computePercentiles(oldRequests);
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

    private double[] computePercentiles(List<ApplicationRequest> requests) {
        ImmutableList<ApiCall> apiCalls = requests.get(0).getApiCalls();
        ImmutableList<DescriptiveStatistics> stats = initStatistics(apiCalls);
        requests.forEach(r -> {
            int[] timeValues = new int[apiCalls.size() + 1];
            for (int i = 0; i < apiCalls.size(); i++) {
                timeValues[i] = r.getApiCalls().get(i).getTimeElapsed();
            }
            timeValues[apiCalls.size()] = r.getResponseTime() - IntStream.of(timeValues).sum();
            if (log.isDebugEnabled()) {
                log.debug("Response time vector: {}", Arrays.toString(timeValues));
            }

            for (int i = 0; i < timeValues.length; i++) {
                stats.get(i).addValue(timeValues[i]);
            }
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
            DescriptiveStatistics statistics = new DescriptiveStatistics();
            statistics.setPercentileImpl(new Percentile()
                    .withEstimationType(Percentile.EstimationType.R_7));
            stats.add(statistics);
        }
        return stats.build();
    }
}
