package edu.ucsb.cs.roots.workload;

import edu.ucsb.cs.roots.rlang.RService;

public class BinSegChangePointDetector extends PELTChangePointDetector {

    public BinSegChangePointDetector(RService rService) {
        super(rService);
        this.method = "cpt.mean(x, method='BinSeg')";
    }
}
