package edu.ucsb.cs.roots.workload;

import com.google.common.primitives.Doubles;
import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.rlang.RService;
import org.rosuda.REngine.REXP;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class PELTChangePointDetector implements ChangePointDetector {

    private final RService rService;
    protected final String method;

    public PELTChangePointDetector(RService rService) {
        checkNotNull(rService, "RService is required");
        this.rService = rService;
        this.method = "cpt.mean(x, method='PELT')";
    }

    @Override
    public int[] computeChangePoints(List<Double> data) throws Exception {
        try (RClient r = new RClient(rService)) {
            r.assign("x", Doubles.toArray(data));
            r.evalAndAssign("result", method);
            REXP result = r.eval("cpts(result)");
            return Arrays.stream(result.asIntegers()).map(i -> i - 1).toArray();
        }
    }

    @Override
    public Segment[] computeSegments(List<Double> data) throws Exception {
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
