package edu.ucsb.cs.roots.workload;

import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.rlang.RService;
import org.rosuda.REngine.REXP;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A change point detector based on "Joint Estimation of Model Parameters and Outlier
 * Effects in Time Series" by Chen and Lui (1993). Uses the tsoutliers package of R.
 */
public class CLChangePointDetector extends ChangePointDetector {

    private final RService rService;

    public CLChangePointDetector(RService rService) {
        checkNotNull(rService, "RService is required");
        this.rService = rService;
    }

    @Override
    public int[] computeChangePoints(double[] data) throws Exception {
        try (RClient client = new RClient(rService)) {
            client.assign("x", data);
            client.evalAndAssign("x_ts", "ts(x)");
            client.evalAndAssign("result", "tso(x_ts, types=c('LS'))");
            REXP result = client.eval("result$outliers[,2]");
            int[] indices = result.asIntegers();
            if (indices.length == 0) {
                return new int[]{-1};
            }
            return Arrays.stream(indices).map(i -> i - 2).toArray();
        }
    }
}
