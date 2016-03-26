package edu.ucsb.cs.roots;

import com.google.common.base.Strings;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorService;
import edu.ucsb.cs.roots.data.DataStoreService;
import edu.ucsb.cs.roots.rlang.RService;
import edu.ucsb.cs.roots.utils.RootsThreadFactory;
import edu.ucsb.cs.roots.workload.WorkloadAnalyzerService;

import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public final class RootsEnvironment {

    private final String id;
    private final ConfigLoader configLoader;
    private final Properties properties;

    private final DataStoreService dataStoreService;
    private final RService rService;
    private final WorkloadAnalyzerService workloadAnalyzerService;
    private final AnomalyDetectorService anomalyDetectorService;

    private final Stack<ManagedService> activeServices;
    private final ExecutorService exec;
    private final EventBus eventBus;

    private State state;

    public RootsEnvironment(String id, ConfigLoader configLoader) throws Exception {
        checkArgument(!Strings.isNullOrEmpty(id), "Environment ID is required");
        checkNotNull(configLoader);
        this.id = id;
        this.configLoader = configLoader;
        this.properties = configLoader.loadGlobalProperties();

        this.dataStoreService = new DataStoreService(this);
        this.rService = new RService(this);
        this.workloadAnalyzerService = new WorkloadAnalyzerService(this);
        this.anomalyDetectorService = new AnomalyDetectorService(this);
        this.activeServices = new Stack<>();
        this.exec = Executors.newCachedThreadPool(new RootsThreadFactory(id + "-event-bus"));
        this.eventBus = new AsyncEventBus(id, this.exec);
        this.state = State.STANDBY;
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public synchronized void init() throws Exception {
        checkState(state == State.STANDBY);
        initService(dataStoreService);
        initService(rService);
        initService(workloadAnalyzerService);
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
        properties.clear();
        state = State.DESTROYED;
        this.notifyAll();
    }

    public String getId() {
        return id;
    }

    public AnomalyDetectorService getAnomalyDetectorService() {
        checkState(anomalyDetectorService.getState() == State.INITIALIZED);
        return anomalyDetectorService;
    }

    public DataStoreService getDataStoreService() {
        checkState(dataStoreService.getState() == State.INITIALIZED);
        return dataStoreService;
    }

    public RService getRService() {
        checkState(rService.getState() == State.INITIALIZED);
        return rService;
    }

    public String getProperty(String key, String def) {
        return properties.getProperty(key, def);
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
        RootsEnvironment environment = new RootsEnvironment("Roots", new FileConfigLoader("conf"));
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
