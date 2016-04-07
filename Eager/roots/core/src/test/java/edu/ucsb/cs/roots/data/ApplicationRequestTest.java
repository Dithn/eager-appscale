package edu.ucsb.cs.roots.data;

import com.google.common.collect.ImmutableList;
import junit.framework.Assert;
import org.junit.Test;

public class ApplicationRequestTest {

    @Test
    public void testApiCall() {
        ApiCall call = new ApiCall(100, "foo", "bar", 10);
        Assert.assertEquals("foo:bar", call.name());
    }

    @Test
    public void testPathString0() {
        ImmutableList<ApiCall> calls = ImmutableList.of();
        ApplicationRequest request = new ApplicationRequest("test", 100, "app", "op", calls);
        Assert.assertEquals("", request.getPathAsString());
        Assert.assertEquals(0, request.getResponseTime());
    }

    @Test
    public void testPathString1() {
        ImmutableList<ApiCall> calls = ImmutableList.of(
                new ApiCall(100, "foo", "bar", 10)
        );
        ApplicationRequest request = new ApplicationRequest("test", 100, "app", "op", calls);
        Assert.assertEquals("foo:bar", request.getPathAsString());
        Assert.assertEquals(10, request.getResponseTime());
    }

    @Test
    public void testPathString2() {
        ImmutableList<ApiCall> calls = ImmutableList.of(
                new ApiCall(100, "foo", "bar", 10),
                new ApiCall(100, "foo", "baz", 10)
        );
        ApplicationRequest request = new ApplicationRequest("test", 100, "app", "op", calls);
        Assert.assertEquals("foo:bar, foo:baz", request.getPathAsString());
        Assert.assertEquals(20, request.getResponseTime());
    }

}
