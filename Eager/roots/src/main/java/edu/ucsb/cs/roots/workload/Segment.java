package edu.ucsb.cs.roots.workload;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public final class Segment {

    private final int start;
    private final int end;
    private final double mean;

    public Segment(int start, int end, List<Double> data) {
        checkArgument(start < end);
        this.start = start;
        this.end = end;
        this.mean = data.subList(start, end).stream().mapToDouble(Double::doubleValue)
                .average().getAsDouble();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public double getMean() {
        return mean;
    }

    public double percentageIncrease(Segment other) {
        return (other.mean - this.mean) * 100.0 / this.mean;
    }
}
