package edu.ucsb.cs.roots.data;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.ConfigLoader;
import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class DataStoreService extends ManagedService {

    private static final String DATA_STORE_NAME = "name";
    private static final String DATA_STORE_TYPE = "type";
    private static final String DATA_STORE_ES_HOST = "es.host";
    private static final String DATA_STORE_ES_PORT = "es.port";
    private static final String DATA_STORE_ES_ACCESS_LOG_INDEX = "es.accessLog.index";

    private final Map<String,DataStore> dataStores = new ConcurrentHashMap<>();

    public DataStoreService(RootsEnvironment environment) {
        super(environment);
    }

    public synchronized void doInit() {
        environment.getConfigLoader().loadItems(ConfigLoader.DATA_STORES, false).forEach(p -> {
            String name = p.getProperty(DATA_STORE_NAME);
            checkArgument(!Strings.isNullOrEmpty(name), "Data store name is required");
            dataStores.put(name, createDataStore(p));
        });
        dataStores.values().stream().forEach(DataStore::init);
    }

    public synchronized void doDestroy() {
        dataStores.values().stream().forEach(DataStore::destroy);
        dataStores.clear();
    }

    public DataStore get(String name) {
        DataStore dataStore = dataStores.get(name);
        checkNotNull(dataStore, "No data store exists by the name: %s", name);
        return dataStore;
    }

    void put(String name, DataStore dataStore) {
        dataStores.put(name, dataStore);
    }

    private DataStore createDataStore(Properties properties) {
        String dataStore = getRequired(properties, DATA_STORE_TYPE);
        if (RandomDataStore.class.getSimpleName().equals(dataStore)) {
            return new RandomDataStore();
        } else if (ElasticSearchDataStore.class.getSimpleName().equals(dataStore)) {
            return ElasticSearchDataStore.newBuilder()
                    .setElasticSearchHost(getRequired(properties, DATA_STORE_ES_HOST))
                    .setElasticSearchPort(getRequiredInt(properties, DATA_STORE_ES_PORT))
                    .setAccessLogIndex(getRequired(properties, DATA_STORE_ES_ACCESS_LOG_INDEX))
                    .build();
        } else {
            throw new IllegalArgumentException("Unknown data store type: " + dataStore);
        }
    }

    private String getRequired(Properties properties, String name) {
        String value = properties.getProperty(name);
        checkArgument(!Strings.isNullOrEmpty(value), "Property %s is required", name);
        return value;
    }

    private int getRequiredInt(Properties properties, String name) {
        String value = properties.getProperty(name);
        checkArgument(!Strings.isNullOrEmpty(value), "Property %s is required", name);
        return Integer.parseInt(value);
    }

}
