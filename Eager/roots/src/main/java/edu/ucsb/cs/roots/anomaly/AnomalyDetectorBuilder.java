package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.data.DataStore;

import java.util.concurrent.TimeUnit;

public abstract class AnomalyDetectorBuilder<T extends AnomalyDetector, B extends AnomalyDetectorBuilder<T,B>> {

    protected String application;
    protected int period = 60;
    protected TimeUnit timeUnit = TimeUnit.SECONDS;
    protected DataStore dataStore;

    private final B thisObj;

    public abstract T build();
    protected abstract B getThisObj();

    public AnomalyDetectorBuilder() {
        this.thisObj = getThisObj();
    }

    public final B setApplication(String application) {
        this.application = application;
        return thisObj;
    }

    public final B setPeriod(int period) {
        this.period = period;
        return thisObj;
    }

    public final B setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return thisObj;
    }

    public final B setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        return thisObj;
    }
}
