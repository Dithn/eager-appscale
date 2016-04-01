package edu.ucsb.cs.roots.data.es;

public class ScrollQuery extends Query {

    private static final String SCROLL_QUERY = "{\"scroll_id\": \"%s\", \"scroll\": \"%s\"}";

    private final String scrollId;
    private final String scroll;

    private ScrollQuery(String scrollId, String scroll) {
        this.scrollId = scrollId;
        this.scroll = scroll;
    }

    public static String build(String scrollId) {
        return new ScrollQuery(scrollId, "1m").getJsonString();
    }

    @Override
    public String getJsonString() {
        return String.format(SCROLL_QUERY, scrollId, scroll);
    }
}
