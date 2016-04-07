package edu.ucsb.cs.roots.data.es;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Query {

    public abstract String getJsonString();

    static String loadTemplate(String name) {
        try (InputStream in = Query.class.getResourceAsStream(name)) {
            checkNotNull(in, "Failed to load resource: %s", name);
            return IOUtils.toString(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
