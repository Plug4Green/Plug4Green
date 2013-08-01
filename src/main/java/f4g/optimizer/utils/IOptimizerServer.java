/**
 * ============================== Header ============================== 
 * file:          IOptimizerServer.java
 * project:       FIT4Green/Optimizer
 * created:       25 janv. 2011 by cdupont
 * last modified: $LastChangedDate: 2011-10-21 14:40:57 +0200 (vie, 21 oct 2011) $ by $LastChangedBy: f4g.cnit $
 * revision:      $LastChangedRevision: 923 $
 * 
 * short description:
 *   Interface to a server used by the optimizer
 * ============================= /Header ==============================
 */
package f4g.optimizer.utils;

import java.util.List;

import f4g.commons.com.util.PowerData;
import f4g.cloudTraditional.OptimizerEngineCloudTraditional.AlgoType;
import f4g.utils.OptimizerServer.CandidateState;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.metamodel.MainboardType;
import f4g.schemas.java.metamodel.ServerStatusType;
import org.jvnet.jaxb2_commons.lang.CopyTo;

/**
 * Interface to a server used by the optimizer 
 *
 * @author cdupont
 */
public interface IOptimizerServer extends Cloneable, CopyTo {
	
		
	public abstract List<OptimizerWorkload> getWorkloads();
	
	public abstract void setWorkloads(List<OptimizerWorkload> workloads);

	public abstract int getNbCores();

	public abstract int getNbCPU();

	public abstract long getMemory();

	public abstract double getStorage();
	
	public abstract double getNICBandwidth();

	public abstract void setCandidateState(CandidateState candidateState);

	public abstract String getCandidateState();

	public abstract double getLoadRate(AggregatedUsage reference, AlgoType algoType);

	public abstract void addVM(OptimizerWorkload WL, AlgoType algoType);

    public abstract String getFrameworkID();
    
    public abstract PowerData getPower(IPowerCalculator powerCalculator);
    
    public abstract ServerStatusType getServerStatus();

    public abstract void setServerStatus(ServerStatusType value);

    public abstract List<MainboardType> getServerMainboard();
    
    public abstract Object clone();

}