package edu.ucsb.cs.roots.data.es;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Query {

    private final boolean rawStringFilter;

    public Query() {
        this(false);
    }

    public Query(boolean rawStringFilter) {
        this.rawStringFilter = rawStringFilter;
    }

    public abstract String getJsonString();

    protected final String stringFieldName(String field) {
        if (rawStringFilter) {
            return field + ".raw";
        }
        return field;
    }

    static String loadTemplate(String name) {
        try (InputStream in = Query.class.getResourceAsStream(name)) {
            checkNotNull(in, "Failed to load resource: %s", name);
            return IOUtils.toString(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
