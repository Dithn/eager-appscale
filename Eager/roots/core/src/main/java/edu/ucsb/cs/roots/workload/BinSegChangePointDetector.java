package edu.ucsb.cs.roots.workload;

import edu.ucsb.cs.roots.rlang.RService;

public class BinSegChangePointDetector extends PELTChangePointDetector {

    public BinSegChangePointDetector(RService rService) {
        super(rService);
    }

    @Override
    protected String getRCall() {
        return "cpt.mean(x, method='BinSeg')";
    }
}
