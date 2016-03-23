package edu.ucsb.cs.roots.anomaly;

public abstract class AnomalyDetectorBuilder<T extends AnomalyDetector, B extends AnomalyDetectorBuilder<T,B>> {

    protected String application;
    protected int periodInSeconds = 60;
    protected String dataStore = "default";

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

    public final B setDataStore(String dataStore) {
        this.dataStore = dataStore;
        return thisObj;
    }
}
