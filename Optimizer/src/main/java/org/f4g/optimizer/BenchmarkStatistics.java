package org.f4g.optimizer;

/**
 * Collect statistics from a benchmark
 * @author Fabien Hermenier
 */
public class BenchmarkStatistics {

    private boolean solved = false;

    private int nbActions = -1;

    private long solvingDuration = -1;

    private String id;

    public BenchmarkStatistics(String id) {
        this.id = id;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("id: ").append(id).append("\n");
        b.append("solved: ").append(solved).append("\n");
        b.append("#actions: ").append(nbActions).append("\n");
        b.append("#solvingDuration: ").append(solvingDuration).append(" ms\n");
        return b.toString();
    }

    public String toRaw() {
        return new StringBuilder(id).append(" ")
                .append(solved ? 1 : 0)
                .append(" ")
                .append(nbActions)
                .append(" ")
                .append(solvingDuration).toString();
    }

    public boolean isSolved() {
        return solved;
    }

    public int getNbActions() {
        return nbActions;
    }

    public long getSolvingDuration() {
        return solvingDuration;
    }

    public String getId() {
        return id;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public void setNbActions(int nbActions) {
        this.nbActions = nbActions;
    }

    public void setSolvingDuration(long solvingDuration) {
        this.solvingDuration = solvingDuration;
    }
}
