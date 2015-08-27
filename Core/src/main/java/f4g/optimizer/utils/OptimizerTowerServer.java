/**
* ============================== Header ============================== 
* file:          Server.java
* project:       FIT4Green/Optimizer
* created:       26 nov. 2010 by cdupont
* last modified: $LastChangedDate: 2011-10-21 14:40:57 +0200 (vie, 21 oct 2011) $ by $LastChangedBy: f4g.cnit $
* revision:      $LastChangedRevision: 923 $
* 
* short description:
*   optimizer's type to represent a server tower. 
*   
* ============================= /Header ==============================
*/

package f4g.optimizer.utils;


import java.util.List;

import org.apache.log4j.Logger;

import f4g.commons.com.util.PowerData;
import f4g.commons.power.IPowerCalculator;
import f4g.schemas.java.constraints.optimizerconstraints.VMFlavorType;
import f4g.schemas.java.metamodel.*;

import org.jvnet.jaxb2_commons.lang.CopyStrategy;
import org.jvnet.jaxb2_commons.lang.JAXBCopyStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import f4g.optimizer.utils.OptimizerServer.CandidateState;
import f4g.optimizer.utils.OptimizerServer.CreationImpossible;

/** 
 *  This class is the internal representation of a server tower used by the optimizer.
 */
@Deprecated
public class OptimizerTowerServer extends TowerServer implements IOptimizerServer {
		
	public Logger log;  
	
	OptimizerServer optimizerServer;
	
	/**
	 * Server constructor for Cloud
	 */
	public OptimizerTowerServer(Server modelServer, VMFlavorType myVMFlavors) throws CreationImpossible{
		
		log = Logger.getLogger(this.getClass().getName());		
		optimizerServer = new OptimizerServer(modelServer, myVMFlavors, (Server)this);
		
	}
	
	/**
	 * Server constructor for traditional
	 * if the first parameter is not null, it take these workloads. Otherwise it gets them from the model server.
	 */
	public OptimizerTowerServer(List<OptimizerWorkload> WLs, Server modelServer) throws CreationImpossible{

		log = Logger.getLogger(this.getClass().getName());		
		optimizerServer = new OptimizerServer(WLs, modelServer, (Server)this);
	}
	
	/**
	 * Default constructor
	 */
	public OptimizerTowerServer() {}
	
	//Forward calls to OptimizerServer.
	@Override public List<OptimizerWorkload> getWorkloads()                                   { return optimizerServer.getWorkloads(     );  }
	@Override public void                    setWorkloads(List<OptimizerWorkload> workloads)  {        optimizerServer.setWorkloads(     workloads);}
	@Override public int                     getNbCores()                                     { return optimizerServer.getNbCores(       this);}
	@Override public int                     getNbCPU()                                       { return optimizerServer.getNbCPU(         this);  }	
	@Override public long                    getMemory()                                      { return optimizerServer.getMemory(        this); }	
	@Override public double                  getStorage()   	                              { return optimizerServer.getStorage(       this);}
	@Override public double                  getNICBandwidth() 	                              { return optimizerServer.getNICBandwidth(  this);}
	@Override public PowerData               getPower(IPowerCalculator powerCalculator)       { return optimizerServer.getPower(         powerCalculator, this);}		
	@Override public void                    setCandidateState(CandidateState candState)      {        optimizerServer.setCandidateState(candState);}
	@Override public String                  getCandidateState()                              { return optimizerServer.getCandidateState();}
	@Override public double                  getLoadRate(AggregatedUsage ref)                 { return optimizerServer.getLoadRate(      ref);}
	@Override public void                    addVM(OptimizerWorkload WL)                      {        optimizerServer.addVM(            WL, this);}
	@Override public ServerStatus        getServerStatus()                                { return this           .getStatus();}
	@Override public void                    setServerStatus(ServerStatus value)          {        this           .setStatus(        value);}
	@Override public List<Mainboard>     getServerMainboard()                             { return this           .getMainboard();}
//	          public double                  getNICMaxPower()                                 { return optimizerServer.getNICMaxPower(   this);}
//              public double                  getNICIdlePower()                                {	return optimizerServer.getNICIdlePower(this);}
//              public double                  getIdlePower()                                   {	return optimizerServer.getIdlePower(this);}
//              public int                     getMaxPower()                                    {	return optimizerServer.getMaxPower(this);}
	
    @Override
    public Object clone() {
        return copyTo(createNewInstance());
    }

	@Override
    public Object copyTo(Object target) {
        return copyTo(null, target, JAXBCopyStrategy.INSTANCE);
    }

	@Override
    public Object copyTo(ObjectLocator locator, Object target, CopyStrategy strategy) {
        final Object draftCopy = ((target == null)?createNewInstance():target);
        super.copyTo(locator, draftCopy, strategy);
        if (draftCopy instanceof OptimizerTowerServer) {
        	final OptimizerTowerServer copy = ((OptimizerTowerServer) draftCopy);
        	           
            copy.log = this.log;
            copy.optimizerServer = (OptimizerServer)optimizerServer.clone();
            
        }
        return draftCopy;
    }

	@Override
    public Object createNewInstance() {
        return new OptimizerTowerServer();
    }


}
