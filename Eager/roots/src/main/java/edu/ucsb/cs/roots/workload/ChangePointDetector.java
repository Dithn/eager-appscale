package edu.ucsb.cs.roots.workload;

import java.util.List;

public abstract class ChangePointDetector {

    public abstract int[] computeChangePoints(List<Double> data) throws Exception;

    public final Segment[] computeSegments(List<Double> data) throws Exception {
        int[] changePoints = computeChangePoints(data);
        if (changePoints[0] == -1) {
            return new Segment[0];
        }
        Segment[] segments = new Segment[changePoints.length + 1];
        int prevIndex = 0;
        for (int i = 0; i <= changePoints.length; i++) {
            int cp;
            if (i == changePoints.length) {
                cp = data.size();
            } else {
                cp = changePoints[i] + 1;
            }
            segments[i] = new Segment(prevIndex, cp, data);
            prevIndex = cp;
        }
        return segments;
    }


}
