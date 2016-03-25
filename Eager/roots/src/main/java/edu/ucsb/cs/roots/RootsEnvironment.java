package edu.ucsb.cs.roots;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import edu.ucsb.cs.roots.anomaly.AnomalyDetectorService;
import edu.ucsb.cs.roots.data.DataStoreService;
import edu.ucsb.cs.roots.rlang.RService;
import edu.ucsb.cs.roots.utils.RootsThreadFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkState;

public class RootsEnvironment {

    private final String id;
    private final Properties properties;

    private final DataStoreService dataStoreService;
    private final RService rService;
    private final AnomalyDetectorService anomalyDetectorService;

    private final Stack<ManagedService> activeServices;
    private final ExecutorService exec;
    private final EventBus eventBus;

    private State state;

    public RootsEnvironment(String id, Properties properties) throws Exception {
        this.id = id;
        this.properties = properties;
        this.dataStoreService = new DataStoreService(this);
        this.rService = new RService(this);
        this.anomalyDetectorService = new AnomalyDetectorService(this);
        this.activeServices = new Stack<>();
        this.exec = Executors.newCachedThreadPool(new RootsThreadFactory(id + "-event-bus"));
        this.eventBus = new AsyncEventBus(id, this.exec);
        this.state = State.STANDBY;
    }

    public synchronized void init() throws Exception {
        checkState(state == State.STANDBY);
        initService(dataStoreService);
        initService(rService);
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
        Properties properties = new Properties();
        File conf = new File("conf", "roots.properties");
        if (conf.exists()) {
            try (FileInputStream in = FileUtils.openInputStream(conf)) {
                properties.load(in);
            }
        }
        RootsEnvironment environment = new RootsEnvironment("Roots", properties);
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
