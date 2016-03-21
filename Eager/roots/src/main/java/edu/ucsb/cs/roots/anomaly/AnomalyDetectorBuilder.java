package edu.ucsb.cs.roots.anomaly;

import edu.ucsb.cs.roots.data.DataStore;

import java.util.concurrent.TimeUnit;

public abstract class AnomalyDetectorBuilder<T extends AnomalyDetector, B extends AnomalyDetectorBuilder<T,B>> {

    protected String application;
    protected int periodInSeconds = 60;
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

    public final B setPeriodInSeconds(int periodInSeconds) {
        this.periodInSeconds = periodInSeconds;
        return thisObj;
    }

    public final B setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        return thisObj;
    }
}
