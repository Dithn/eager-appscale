package edu.ucsb.cs.roots.data;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class ElasticSearchTemplates {

    static final String RESPONSE_TIME_SUMMARY_QUERY = loadTemplate("response_time_summary_query.json");

    private static String loadTemplate(String name) {
        try (InputStream in = ElasticSearchTemplates.class.getResourceAsStream(name)) {
            checkNotNull(in, "Failed to load resource: %s", name);
            return IOUtils.toString(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
