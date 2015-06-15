package f4g.optimizer.comm;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import f4g.manager.monitor.Monitor;
import f4g.optimizer.cloud.OptimizerEngineCloud;

@Path("/{version}/plug4green/{name}")
public class SLAService {
	
	OptimizerEngineCloud opti;
	Monitor monitor;
	private Logger logger = LoggerFactory.getLogger(SLAService.class);
	
	public SLAService(OptimizerEngineCloud opti, Monitor monitor) {
		this.opti = opti;
		this.monitor = monitor;
	}

   /* @PUT
    @Path("/cpuovercommit")
    @Consumes(MediaType.TEXT_PLAIN)
    public void changeCPUOvercommit(@PathParam("version") String version, String cpuovercommit) {
        logger.debug("received cpu overcommit:");
        logger.debug(cpuovercommit.toString());
        opti.setCPUOvercommit(Float.parseFloat(cpuovercommit));
        monitor.requestGlobalOptimization();
    }*/
    
    @PUT
    @Path("/VMCPUDemand")
    @Consumes(MediaType.TEXT_PLAIN)
    public void changeVMCPUDemand(@PathParam("version") String version, @PathParam("name") String name, String VMCPUDemand) {
        logger.debug("received VM CPU Demand:" + name + ": " + VMCPUDemand);
        monitor.setVMCPUConstraint(name, Integer.parseInt(VMCPUDemand));

    }
    
}
