package edu.ucsb.cs.roots.workload;

import edu.ucsb.cs.roots.rlang.RClient;
import edu.ucsb.cs.roots.rlang.RService;
import org.rosuda.REngine.REXP;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

public class PELTChangePointDetector extends ChangePointDetector {

    private final RService rService;

    public PELTChangePointDetector(RService rService) {
        checkNotNull(rService, "RService is required");
        this.rService = rService;
    }

    @Override
    public int[] computeChangePoints(double[] data) throws Exception {
        try (RClient r = new RClient(rService)) {
            r.assign("x", data);
            r.evalAndAssign("result", getRCall());
            REXP result = r.eval("cpts(result)");
            return Arrays.stream(result.asIntegers()).map(i -> i - 1).toArray();
        }
    }

    protected String getRCall() {
        return "cpt.mean(x, method='PELT')";
    }

}
