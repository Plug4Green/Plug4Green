package org.f4g.optimizer;

/**
 * Collect statistics from a benchmark
 * @author Fabien Hermenier
 */
public class BenchmarkStatistics {

    private boolean solved = false;

    private int nbActions = -1;

    private long solvingDuration = -1;
    
    private double powerBefore = -1;
    
    private double powerAfter = -1;

	private int nbMigrations = -1;

	private int nbPowerOn = -1;

	private int nbPowerOff = -1;
      

	private String id;

    public BenchmarkStatistics(String id) {
        this.id = id;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("id: ").append(id).append("\n");
        b.append("solved: ").append(solved).append("\n");
        b.append("#actions: ").append(nbActions).append("\n");
        b.append("#nb ons: ").append(nbPowerOn).append("\n");
        b.append("#nb offs: ").append(nbPowerOff).append("\n");
        b.append("#nb migrations: ").append(nbMigrations).append("\n");
        b.append("#power before: ").append(powerBefore).append(" Watts").append("\n");
        b.append("#power after: ").append(powerAfter).append(" Watts").append("\n");
        b.append("#solving duration: ").append(solvingDuration).append(" ms\n");
        return b.toString();
    }

    public String toRaw() {
        return new StringBuilder(id).append(" ")
                .append(solved ? 1 : 0)
                .append(" ")
                .append(nbActions)
                .append(" ")
                .append(nbPowerOn)
                .append(" ")
                .append(nbPowerOff)
                .append(" ")
                .append(nbMigrations)
                .append(" ")
                .append(powerBefore)
                .append(" ")
                .append(powerAfter)
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
    

    public double getPowerAfter() {
		return powerAfter;
	}

	public void setPowerAfter(double powerAfter) {
		this.powerAfter = powerAfter;
	}
	
	public double getPowerBefore() {
		return powerBefore;
	}

	public void setPowerBefore(double powerBefore) {
		this.powerBefore = powerBefore;
	}
	
	public int getNbMigrations() {
		return nbMigrations;
	}

	public void setNbMigrations(int nbMigrations) {
		this.nbMigrations = nbMigrations;
	}
		
	public int getNbPowerOn() {
		return nbPowerOn;
	}

	public void setNbPowerOn(int nbPowerOn) {
		this.nbPowerOn = nbPowerOn;
	}
		
	public int getNbPowerOff() {
		return nbPowerOff;
	}

	public void setNbPowerOff(int nbPowerOff) {
		this.nbPowerOff = nbPowerOff;
	}
}
