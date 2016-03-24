package edu.ucsb.cs.roots;

import edu.ucsb.cs.roots.anomaly.AnomalyDetectorService;
import edu.ucsb.cs.roots.config.DataStoreService;
import org.quartz.SchedulerException;

import static com.google.common.base.Preconditions.checkState;

public class RootsEnvironment {

    private final String id;
    private final DataStoreService dataStoreService;
    private final AnomalyDetectorService anomalyDetectorService;

    private State state;

    public RootsEnvironment(String id) throws SchedulerException {
        this.id = id;
        this.dataStoreService = new DataStoreService(this);
        this.anomalyDetectorService = new AnomalyDetectorService(this);
        this.state = State.STANDBY;
    }

    public synchronized void init() {
        checkState(state == State.STANDBY);
        dataStoreService.init();
        anomalyDetectorService.init();
        state = State.INITIALIZED;
    }

    public synchronized void destroy() {
        checkState(state == State.INITIALIZED);
        try {
            anomalyDetectorService.destroy();
            dataStoreService.destroy();
        } finally {
            state = State.DESTROYED;
            this.notifyAll();
        }
    }

    public String getId() {
        return id;
    }

    public DataStoreService getDataStoreService() {
        checkState(state == State.INITIALIZED);
        return dataStoreService;
    }

    public AnomalyDetectorService getAnomalyDetectorService() {
        checkState(state == State.INITIALIZED);
        return anomalyDetectorService;
    }

    public synchronized void waitFor() {
        while (state == State.INITIALIZED) {
            try {
                this.wait(10000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static void main(String[] args) throws SchedulerException {
        RootsEnvironment environment = new RootsEnvironment("Roots");
        environment.init();

        Runtime.getRuntime().addShutdownHook(new Thread("RootsShutdownHook") {
            @Override
            public void run() {
                environment.destroy();
            }
        });
        environment.waitFor();
    }
}
