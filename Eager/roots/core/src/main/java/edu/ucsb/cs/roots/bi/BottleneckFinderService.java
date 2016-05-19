package edu.ucsb.cs.roots.bi;

import com.google.common.eventbus.Subscribe;
import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import edu.ucsb.cs.roots.anomaly.Anomaly;

public final class BottleneckFinderService extends ManagedService {

    public BottleneckFinderService(RootsEnvironment environment) {
        super(environment);
    }

    @Override
    protected void doInit() throws Exception {
        environment.subscribe(this);
    }

    @Override
    protected void doDestroy() {
    }

    @Subscribe
    public void run(Anomaly anomaly) {
        if (anomaly.getType() == Anomaly.TYPE_WORKLOAD) {
            return;
        }

        BottleneckFinder finder = new RelativeImportanceBasedFinder(environment);
        finder.analyze(anomaly);
    }
}
