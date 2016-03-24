package edu.ucsb.cs.roots.config;

import com.google.common.base.Strings;
import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.data.DataStore;
import edu.ucsb.cs.roots.data.ElasticSearchDataStore;
import edu.ucsb.cs.roots.data.TestDataStore;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class DataStoreService extends ManagedService {

    private static final String DATA_STORE_TYPE = "type";
    private static final String DATA_STORE_ES_HOST = "es.host";
    private static final String DATA_STORE_ES_PORT = "es.port";
    private static final String DATA_STORE_ES_ACCESS_LOG_INDEX = "es.accessLog.index";

    private final Map<String,DataStore> dataStores = new ConcurrentHashMap<>();

    public DataStoreService(RootsEnvironment environment) {
        super(environment);
    }

    public synchronized void doInit() {
        File dataStoreDir = new File("conf", "dataStores");
        if (!dataStoreDir.exists()) {
            log.warn("dataStores directory not found");
            return;
        }
        FileUtils.listFiles(dataStoreDir, new String[]{"properties"}, false).stream()
                .forEach(f -> dataStores.put(FilenameUtils.removeExtension(f.getName()),
                        createDataStore(f)));
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

    private DataStore createDataStore(File file) {
        Properties properties = new Properties();
        try (FileInputStream in = FileUtils.openInputStream(file)) {
            log.info("Loading data store from: " + file.getAbsolutePath());
            properties.load(in);
        } catch (IOException e) {
            String msg = "Error while loading data store from: " + file.getAbsolutePath();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        String dataStore = getRequired(properties, DATA_STORE_TYPE);
        if (TestDataStore.class.getSimpleName().equals(dataStore)) {
            return new TestDataStore();
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
