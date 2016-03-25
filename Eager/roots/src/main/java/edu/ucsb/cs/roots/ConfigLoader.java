package edu.ucsb.cs.roots;

import java.util.Properties;
import java.util.stream.Stream;

public interface ConfigLoader {

    int DETECTORS = 100;
    int DATA_STORES = 101;

    default Properties loadGlobalProperties() throws Exception {
        return new Properties();
    }

    default Stream<ItemConfig> loadItems(int type, boolean ignoreFaults) {
        return Stream.empty();
    }

    class ItemConfig {
        private final String name;
        private final Properties properties;

        public ItemConfig(String name, Properties properties) {
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public Properties getProperties() {
            return properties;
        }
    }

}
