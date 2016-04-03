package edu.ucsb.cs.roots.workload;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

public final class Segment {

    private final int start;
    private final int end;
    private final double mean;

    /**
     * Create a new data segment.
     *
     * @param start Start index for the data segment (inclusive)
     * @param end End index for the data segment (exclusive)
     * @param data Array of data entries
     */
    public Segment(int start, int end, double[] data) {
        checkArgument(start < end);
        this.start = start;
        this.end = end;
        this.mean = Arrays.stream(data, start, end).average().getAsDouble();
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
