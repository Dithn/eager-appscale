package edu.ucsb.cs.roots.workload;

import junit.framework.Assert;
import org.junit.Test;

public class ChangePointDetectorTest {

    @Test
    public void testNoChangePoints() throws Exception {
        TestDetector detector = new TestDetector(new int[]{-1});
        Segment[] segments = detector.computeSegments(new double[]{1,2,1,2});
        Assert.assertEquals(1, segments.length);
        Assert.assertEquals(0, segments[0].getStart());
        Assert.assertEquals(4, segments[0].getEnd());
        Assert.assertEquals(1.5, segments[0].getMean());
    }

    @Test
    public void testOneChangePoint() throws Exception {
        TestDetector detector = new TestDetector(new int[]{1});
        Segment[] segments = detector.computeSegments(new double[]{1,2,1,2});
        Assert.assertEquals(2, segments.length);
        Assert.assertEquals(0, segments[0].getStart());
        Assert.assertEquals(2, segments[0].getEnd());
        Assert.assertEquals(1.5, segments[0].getMean());
        Assert.assertEquals(2, segments[1].getStart());
        Assert.assertEquals(4, segments[1].getEnd());
        Assert.assertEquals(1.5, segments[1].getMean());
    }

    @Test
    public void testTwoChangePoints() throws Exception {
        TestDetector detector = new TestDetector(new int[]{1,3});
        Segment[] segments = detector.computeSegments(new double[]{1,2,1,2,1,2});
        Assert.assertEquals(3, segments.length);
        Assert.assertEquals(0, segments[0].getStart());
        Assert.assertEquals(2, segments[0].getEnd());
        Assert.assertEquals(1.5, segments[0].getMean());
        Assert.assertEquals(2, segments[1].getStart());
        Assert.assertEquals(4, segments[1].getEnd());
        Assert.assertEquals(1.5, segments[1].getMean());
        Assert.assertEquals(4, segments[2].getStart());
        Assert.assertEquals(6, segments[2].getEnd());
        Assert.assertEquals(1.5, segments[2].getMean());
    }

    private static class TestDetector extends ChangePointDetector {

        private final int[] indices;

        TestDetector(int[] indices) {
            this.indices = indices;
        }

        @Override
        protected int[] computeChangePoints(double[] data) throws Exception {
            return indices;
        }
    }

}
