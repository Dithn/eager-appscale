package edu.ucsb.cs.roots.workload;

import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;

public final class WorkloadAnalyzerService extends ManagedService {

    public WorkloadAnalyzerService(RootsEnvironment environment) {
        super(environment);
    }

    @Override
    protected void doInit() throws Exception {
        environment.subscribe(new WorkloadAnalyzer(environment));
    }

    @Override
    protected void doDestroy() {
    }
}
