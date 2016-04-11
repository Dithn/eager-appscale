package edu.ucsb.cs.roots.workload;

import edu.ucsb.cs.roots.rlang.RService;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class RChangePointDetector extends ChangePointDetector {

    protected final RService rService;

    public RChangePointDetector(RService rService) {
        checkNotNull(rService, "RService is required");
        this.rService = rService;
    }

}
