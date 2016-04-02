package edu.ucsb.cs.roots.rlang;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.util.HashSet;
import java.util.Set;

public final class RClient implements AutoCloseable {

    private final RService rService;
    private final RConnection r;
    private final Set<String> symbols = new HashSet<>();

    public RClient(RService rService) throws Exception {
        this.rService = rService;
        this.r = rService.borrow();
    }

    public void assign(String symbol, double[] values) throws REngineException {
        r.assign(symbol, values);
        symbols.add(symbol);
    }

    public void assign(String symbol, String[] values) throws REngineException {
        r.assign(symbol, values);
        symbols.add(symbol);
    }

    public void evalAndAssign(String symbol, String cmd) throws RserveException {
        r.eval(symbol + " <- " + cmd);
        symbols.add(symbol);
    }

    public REXP eval(String cmd) throws RserveException {
        return r.eval(cmd);
    }

    @Override
    public void close() throws Exception {
        try {
            for (String s : symbols) {
                r.eval("rm(" + s + ")");
            }
            symbols.clear();
        } finally {
            rService.release(r);
        }
    }

}
