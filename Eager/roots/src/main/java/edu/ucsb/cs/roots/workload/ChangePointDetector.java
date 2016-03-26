package edu.ucsb.cs.roots.workload;

import java.util.List;

public interface ChangePointDetector {

    int[] computeChangePoints(List<Double> data) throws Exception;

    Segment[] computeSegments(List<Double> data) throws Exception;

}
