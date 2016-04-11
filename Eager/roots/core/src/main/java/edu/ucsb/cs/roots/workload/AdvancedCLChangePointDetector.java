package edu.ucsb.cs.roots.workload;

import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.rlang.RService;

import java.util.stream.IntStream;

public class AdvancedCLChangePointDetector extends RChangePointDetector {

    public AdvancedCLChangePointDetector(RService rService) {
        super(rService);
    }

    @Override
    public int[] computeChangePoints(double[] data) throws Exception {
        try (RClient client = new RClient(rService)) {
            client.assign("x", data);
            client.evalAndAssign("x_ts", "ts(x)");
            client.evalAndAssign("result", "tso(x_ts, maxit.iloop=10)");
            String[] labels = client.eval("result$outliers[,1]").asStrings();
            int[] indices = client.eval("result$outliers[,2]").asIntegers();
            return IntStream.range(0, labels.length)
                    .filter(i -> labels[i].equals("LS"))
                    .map(i -> indices[i] - 2)
                    .filter(i -> i >= 0)
                    .toArray();
        }
    }
}
