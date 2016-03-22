package edu.ucsb.cs.roots.config;

import edu.ucsb.cs.roots.data.DataStore;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DefaultDataStore {

    private static final DefaultDataStore instance = new DefaultDataStore();

    private final DataStore dataStore;

    private DefaultDataStore() {
        File file = new File("conf", "_roots_dds.properties");
        if (file.exists()) {
            Properties properties = new Properties();
            try (FileInputStream in = FileUtils.openInputStream(file)) {
                properties.load(in);
                dataStore = AnomalyDetectorFactory.initDataStore(properties);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            dataStore = null;
        }
    }

    public static DefaultDataStore getInstance() {
        return instance;
    }

    public DataStore get() {
        return dataStore;
    }

}
