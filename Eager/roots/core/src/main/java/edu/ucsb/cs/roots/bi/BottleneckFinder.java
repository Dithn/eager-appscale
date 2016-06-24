package edu.ucsb.cs.roots.bi;

import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;
import edu.ucsb.cs.roots.anomaly.AnomalyLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BottleneckFinder {

    protected static final String LOCAL = "LOCAL";

    protected final RootsEnvironment environment;
    protected final Anomaly anomaly;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final AnomalyLog anomalyLog = new AnomalyLog(log);

    public BottleneckFinder(RootsEnvironment environment, Anomaly anomaly) {
        this.environment = environment;
        this.anomaly = anomaly;
    }

    abstract void analyze();

}
