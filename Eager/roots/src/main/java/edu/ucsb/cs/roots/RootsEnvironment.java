package edu.ucsb.cs.roots;

import edu.ucsb.cs.roots.anomaly.AnomalyDetectorService;
import edu.ucsb.cs.roots.data.DataStoreService;

import java.util.Stack;

import static com.google.common.base.Preconditions.checkState;

public class RootsEnvironment {

    private final String id;
    private final DataStoreService dataStoreService;
    private final AnomalyDetectorService anomalyDetectorService;
    private final Stack<ManagedService> activeServices;

    private State state;

    public RootsEnvironment(String id) throws Exception {
        this.id = id;
        this.dataStoreService = new DataStoreService(this);
        this.anomalyDetectorService = new AnomalyDetectorService(this);
        this.activeServices = new Stack<>();
        this.state = State.STANDBY;
    }

    public synchronized void init() throws Exception {
        checkState(state == State.STANDBY);
        initService(dataStoreService);
        initService(anomalyDetectorService);
        state = State.INITIALIZED;
    }

    private void initService(ManagedService service) throws Exception {
        service.init();
        activeServices.push(service);
    }

    public synchronized void destroy() {
        checkState(!activeServices.isEmpty());
        while (!activeServices.isEmpty()) {
            activeServices.pop().destroy();
        }
        state = State.DESTROYED;
        this.notifyAll();
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

    public static void main(String[] args) throws Exception {
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
