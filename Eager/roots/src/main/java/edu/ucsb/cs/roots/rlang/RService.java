package edu.ucsb.cs.roots.rlang;

import edu.ucsb.cs.roots.ManagedService;
import edu.ucsb.cs.roots.RootsEnvironment;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.rosuda.REngine.Rserve.RConnection;

public final class RService extends ManagedService {

    private static final String R_MAX_TOTAL = "r.maxTotal";
    private static final String R_MAX_IDLE = "r.maxIdle";
    private static final String R_MIN_IDLE_TIME_MILLIS = "r.minIdleTimeMillis";

    private final GenericObjectPool<RConnection> rConnectionPool;

    public RService(RootsEnvironment environment) {
        super(environment);
        this.rConnectionPool = new GenericObjectPool<>(new RConnectionPoolFactory());
        this.rConnectionPool.setMaxTotal(Integer.parseInt(
                environment.getProperty(R_MAX_TOTAL, "10")));
        this.rConnectionPool.setMaxIdle(Integer.parseInt(
                environment.getProperty(R_MAX_IDLE, "10")));
        this.rConnectionPool.setMinEvictableIdleTimeMillis(Long.parseLong(
                environment.getProperty(R_MIN_IDLE_TIME_MILLIS, "10000")));
    }

    public RConnection borrow() throws Exception {
        return rConnectionPool.borrowObject();
    }

    public void release(RConnection connection) {
        if (connection != null) {
            rConnectionPool.returnObject(connection);
        }
    }

    @Override
    protected void doInit() throws Exception {
    }

    @Override
    protected void doDestroy() {
        rConnectionPool.close();
    }
}
