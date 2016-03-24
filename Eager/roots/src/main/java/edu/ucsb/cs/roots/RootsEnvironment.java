package edu.ucsb.cs.roots;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorService;
import edu.ucsb.cs.roots.data.DataStoreService;
import edu.ucsb.cs.roots.utils.RootsThreadFactory;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkState;

public class RootsEnvironment {

    private final String id;
    private final DataStoreService dataStoreService;
    private final AnomalyDetectorService anomalyDetectorService;
    private final Stack<ManagedService> activeServices;
    private final ExecutorService exec;
    private final EventBus eventBus;

    private State state;

    public RootsEnvironment(String id) throws Exception {
        this.id = id;
        this.dataStoreService = new DataStoreService(this);
        this.anomalyDetectorService = new AnomalyDetectorService(this);
        this.activeServices = new Stack<>();
        this.exec = Executors.newCachedThreadPool(new RootsThreadFactory(id + "-event-bus"));
        this.eventBus = new AsyncEventBus(id, this.exec);
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
        exec.shutdownNow();
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

    public void publishEvent(Object event) {
        eventBus.post(event);
    }

    public void subscribe(Object subscriber) {
        eventBus.register(subscriber);
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
