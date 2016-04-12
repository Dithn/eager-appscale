package edu.ucsb.cs.roots.rlang;

import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.apache.commons.pool2.impl.GenericObjectPool;

public final class RService extends ManagedService {

    private static final String R_MAX_TOTAL = "r.maxTotal";
    private static final String R_MAX_IDLE = "r.maxIdle";
    private static final String R_MIN_IDLE_TIME_MILLIS = "r.minIdleTimeMillis";

    private final GenericObjectPool<RClient> rConnectionPool;

    public RService(RootsEnvironment environment) {
        super(environment);
        this.rConnectionPool = new GenericObjectPool<>(new RConnectionPoolFactory());
    }

    public RClient borrow() {
        try {
            return rConnectionPool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void release(RClient client) {
        if (client != null) {
            rConnectionPool.returnObject(client);
        }
    }

    @Override
    protected void doInit() throws Exception {
        this.rConnectionPool.setMaxTotal(Integer.parseInt(
                environment.getProperty(R_MAX_TOTAL, "10")));
        this.rConnectionPool.setMaxIdle(Integer.parseInt(
                environment.getProperty(R_MAX_IDLE, "2")));
        this.rConnectionPool.setMinEvictableIdleTimeMillis(Long.parseLong(
                environment.getProperty(R_MIN_IDLE_TIME_MILLIS, "10000")));
    }

    @Override
    protected void doDestroy() {
        rConnectionPool.close();
    }
}
