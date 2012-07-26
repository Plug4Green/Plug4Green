package org.f4g.optimizer;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import entropy.configuration.Configuration;
import entropy.configuration.Configurations;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import org.f4g.entropy.plan.constraint.sliceScheduling.LocalScheduler;
import org.apache.log4j.Logger;
import org.f4g.controller.IController;
import org.f4g.cost_estimator.NetworkCost;
import org.f4g.entropy.configuration.F4GConfigurationAdapter;
import org.f4g.optimizer.CloudTraditional.OptimizerEngineCloudTraditional;
import org.f4g.optimizer.CloudTraditional.SLAReader;
import org.f4g.optimizer.Optimizer.CloudTradCS;
import org.f4g.optimizer.utils.Recorder;
import org.f4g.optimizer.utils.Utils;
import org.f4g.power.PowerCalculator;
import org.f4g.schema.actions.AbstractBaseActionType;
import org.f4g.schema.actions.ActionRequestType;
import org.f4g.schema.actions.LiveMigrateVMActionType;
import org.f4g.schema.actions.MoveVMActionType;
import org.f4g.schema.actions.PowerOffActionType;
import org.f4g.schema.actions.PowerOnActionType;
import org.f4g.schema.constraints.optimizerconstraints.*;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.RackableServerType;
import org.f4g.schema.metamodel.ServerType;
import org.f4g.schema.metamodel.VirtualMachineType;
import org.f4g.test.ModelGenerator;
import org.f4g.util.Util;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * A tool to generate then solve placement problems.
 *
 * @author Fabien Hermenier
 */
public class Benchmark {
	
	private static Logger log;

    private static PowerCalculator powerCalculator = new PowerCalculator();

    public static boolean generateConfiguration(final SLAReader sla, int nbServers, String path, String prefix) {

        int NbVMsperServer = 6;
        int NBVMsTotal = nbServers * NbVMsperServer;
        int nbServers1 = nbServers / 2;
        int nbServers2 = (nbServers % 2 == 0 ? nbServers / 2 : nbServers / 2 + 1);

        ModelGenerator modelGenerator1 = new ModelGenerator();
        //Server1:
        //CPU Dual CPU, quad-core, Intel® Xeon® E5520 2.27 GHz
        //Memory    24 GB (6 x 4 GB DIMMs)
        //Hard disk  2 x 300 GB
        modelGenerator1.setCPU(2);
        modelGenerator1.setCORE(4);
        modelGenerator1.setFREQUENCY(2.27);
        modelGenerator1.setRAM_SIZE(24);
        modelGenerator1.setSTORAGE_SIZE(600);
        modelGenerator1.setNB_VIRTUAL_MACHINES(0);
        modelGenerator1.setNB_SERVERS(nbServers1);
        modelGenerator1.setNB_ROUTERS(0);
        modelGenerator1.setNB_SWITCHES(0);
        modelGenerator1.NUMBER_OF_TRANSISTORS = 731;
        modelGenerator1.SERVER_FRAMEWORK_ID = 100000;


        ModelGenerator modelGenerator2 = new ModelGenerator();
        //Server2:
        //CPU Dual CPU, quad-core, Intel® Xeon® E5540 2.53 GHz
        //Memory    24 GB (6 x 4 GB DIMMs)
        //Hard disk  2 x 300 GB
        modelGenerator2.setCPU(2);
        modelGenerator2.setCORE(4);
        modelGenerator2.setFREQUENCY(2.53);
        modelGenerator2.setRAM_SIZE(24);
        modelGenerator2.setSTORAGE_SIZE(600);
        modelGenerator2.setNB_VIRTUAL_MACHINES(0);
        modelGenerator2.setNB_SERVERS(nbServers2);
        modelGenerator2.SERVER_FRAMEWORK_ID = 100000;
        modelGenerator2.NIC_FRAMEWORK_ID = 300000;
        modelGenerator2.NUMBER_OF_TRANSISTORS = 731;
        FIT4GreenType model = modelGenerator1.createPopulatedFIT4GreenType();
        FIT4GreenType model2 = modelGenerator2.createPopulatedFIT4GreenType();


        //all the servers of model2 are added in model 1. model2 will not be used anymore.
        List<RackableServerType> rackServers = model.getSite().get(0).getDatacenter().get(0).getRack().get(0).getRackableServer();

        for (ServerType server2 : Utils.getAllServers(model2)) {
        	server2.setFrameworkID(server2.getFrameworkID() + "b");
            rackServers.add((RackableServerType) server2);
        }
        List<ServerType> servers;

        //


        //predicate to determine is a server is full according to our known constraints
        Predicate<ServerType> isFull = new Predicate<ServerType>() {
            @Override
            public boolean apply(ServerType server) {

                List<VirtualMachineType> vms = Utils.getVMs(server);

                int sumCPUs = 0;
                int sumCPUDemands = 0;
                int sumMemoryDemands = 0;
                for (VirtualMachineType vm : vms) {
                    VMTypeType.VMType SLAVM = Util.findVMByName(vm.getCloudVmType(), sla.getVMtypes());
                    sumCPUs += SLAVM.getCapacity().getVCpus().getValue();
                    sumCPUDemands += SLAVM.getExpectedLoad().getVCpuLoad().getValue();
                    sumMemoryDemands += SLAVM.getCapacity().getVRam().getValue(); //in GB
                }

                //constraint MaxVMperServer=15
                if (vms.size() >= 15) {
                	log.debug("MaxVMperServer limit for server " + server.getFrameworkID());
                	return true;
                }

                int nbCores = Utils.getNbCores(server);
                //constraint MaxVirtualCPUPerCore=2
                if (sumCPUs >= nbCores * 2){
                	log.debug("MaxVirtualCPUPerCore limit for server " + server.getFrameworkID());
               	    return true;
                }

                //regular CPU consumption constraint
                if (sumCPUDemands + 100 >= nbCores * 100){
                	log.debug("CPU consumption limit for server " + server.getFrameworkID());
               	    return true;
                }
                
                //regular Memory constraint
                if (sumMemoryDemands + 4 >= Utils.getMemory(server) ){
                	log.debug("Memory limit for server " + server.getFrameworkID());
               	    return true;
                }

                return false;
            }
        };


        Date date = new Date();
        GregorianCalendar gCalendar = new GregorianCalendar();
        gCalendar.setTime(date);
        XMLGregorianCalendar now = null;
        try {
            now = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCalendar);
        } catch (DatatypeConfigurationException e1) {
            e1.printStackTrace();
        }

        //List<VirtualMachineType> vms = new ArrayList<VirtualMachineType>();
        Random rand = new Random(System.currentTimeMillis());

        String [] VMType = {"m1.small", "m1.large", "m1.xlarge"};
        for (int i = 0; i < NBVMsTotal; i++) {
        	servers = Utils.getAllServers(model);
            VirtualMachineType VM = modelGenerator1.createVirtualMachineType(servers.get(0), model.getSite().get(0).getDatacenter().get(0).getFrameworkCapabilities().get(0), 1);
            VM.setCloudVmType(VMType[rand.nextInt(VMType.length)]);
            VM.setLastMigrationTimestamp(now);
            VM.setFrameworkID("VMa" + i);
            //vms.add(VM);

            Predicate<ServerType> notFull = Predicates.not(isFull);

            List<ServerType> myList = new ArrayList<ServerType>();
            for (ServerType s : servers) {
                if (notFull.apply(s)) {
                    myList.add(s);
                }
            }
            if (myList.isEmpty()) {
                break;
            }
            int item = rand.nextInt(myList.size());
            ServerType s = myList.get(item);
            s.getNativeOperatingSystem().getHostedHypervisor().get(0).getVirtualMachine().add(VM);

        }


        //Check the current configuration, just in case
        F4GConfigurationAdapter confAdapter = new F4GConfigurationAdapter(model, sla.getVMtypes(), powerCalculator);
        Configuration cfg = confAdapter.extractConfiguration();
        ManagedElementSet<Node> ns = Configurations.currentlyOverloadedNodes(cfg);
        if (!ns.isEmpty()) {
            log.error("Error: Generated configuration is not viable. Currently overloaded: " + ns);
            return false;
        }

        Recorder recorder = new Recorder(true, path, prefix);
        recorder.recordModel(model);
        return true;
    }

    //run all configurations in a directory
    static List<BenchmarkStatistics> runConfigurations(SLAReader sla, String pathName, int timeout) {
        List<BenchmarkStatistics> stats = new LinkedList<BenchmarkStatistics>();

        String fileName;
        File folder = new File(pathName);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
        for (File f : listOfFiles) {
            if (f.isFile()) {
                fileName = f.getName();
                if (fileName.endsWith(".xml")) {
                    log.info("Benching " + fileName);
                    BenchmarkStatistics st = runConfiguration(sla, pathName + File.separator + fileName, timeout);
                    stats.add(st);
                }
            }
        }
        } else {
            log.error("No files in '" + pathName + "'");
        }
        return stats;
    }

    //run a configuration file
    static BenchmarkStatistics runConfiguration(SLAReader sla, String pathName, int timeout) {
        ChocoLogging.setVerbosity(Verbosity.SILENT);
        ChocoLogging.setLoggingMaxDepth(2000);
        LocalScheduler.DEBUG = -1;
        ModelGenerator modelGenerator = new ModelGenerator();
        FIT4GreenType model = modelGenerator.getModel(pathName);

        for(ServerType server : Utils.getAllServers(model)) {
        	sla.getCluster().getCluster().get(0).getNodeController().getNodeName().add(server.getFrameworkID());
        }       

        BenchmarkStatistics st = new BenchmarkStatistics(pathName);

        OptimizerEngineCloudTraditional optimizer = new OptimizerEngineCloudTraditional(new MockController(st), powerCalculator, new NetworkCost(), CloudTradCS.CLOUD, sla);
    	
        optimizer.setSearchTimeLimit(timeout);
        optimizer.doOptimize(false);
        long start = System.currentTimeMillis();

        optimizer.runGlobalOptimization(model);
        long ed = System.currentTimeMillis();
        st.setSolvingDuration(ed - start);
        return st;
    }


    protected static class MockController implements IController {

        private BenchmarkStatistics st;

        public MockController(BenchmarkStatistics st) {
            this.st = st;
        }

        @Override
        public boolean executeActionList(ActionRequestType myActionRequest) {
            int nb = myActionRequest.getActionList().getAction().size();

            //FIXME: Assume having actions means we have a successful solving process.
            st.setSolved(nb > 0);
            st.setNbActions(nb);
			
            int ons = 0;
            int offs = 0;
            int moves = 0;
                        
			for (JAXBElement<? extends AbstractBaseActionType> action : myActionRequest.getActionList().getAction()){
				if (action.getValue() instanceof PowerOnActionType) {
					PowerOnActionType on = (PowerOnActionType)action.getValue();
					log.debug("executeActionList: power ON on :" + on.getNodeName());
					ons++;
				}
				if (action.getValue() instanceof PowerOffActionType) {
					PowerOffActionType off = (PowerOffActionType)action.getValue();
					log.debug("executeActionList: power OFF on :" + off.getNodeName());
					offs++;
				}
				if (action.getValue() instanceof MoveVMActionType) {
					MoveVMActionType move = (MoveVMActionType)action.getValue();
					log.debug("executeActionList: move VM " + move.getVirtualMachine() + " from " + move.getSourceNodeController() + " to " + move.getDestNodeController());
					moves++;
				}
				if (action.getValue() instanceof LiveMigrateVMActionType) {
					LiveMigrateVMActionType liveMigration = (LiveMigrateVMActionType)action.getValue();
					log.debug("executeActionList: live migrate VM " + liveMigration.getVirtualMachine() + " from " + liveMigration.getSourceNodeController() + " to " + liveMigration.getDestNodeController());
					moves++;
				}
			}

			log.debug("executeActionList: ComputedPowerBefore = " + myActionRequest.getComputedPowerBefore().getValue());
			log.debug("executeActionList: ComputedPowerAfter = " + myActionRequest.getComputedPowerAfter().getValue());
			
			st.setNbMigrations(moves);
			st.setNbPowerOn(ons);
			st.setNbPowerOff(offs);
			st.setPowerBefore(myActionRequest.getComputedPowerBefore().getValue());
			st.setPowerAfter(myActionRequest.getComputedPowerAfter().getValue());
			
			return true;
		}

        @Override
        public boolean dispose() {
            return false;
        }

        /* (non-Javadoc)
           * @see org.f4g.controller.IController#setActionsApproved(boolean)
           */
        @Override
        public void setActionsApproved(boolean actionsApproved) {
        }

        /* (non-Javadoc)
           * @see org.f4g.controller.IController#setActionsApproved(boolean)
           */
        @Override
        public void setApprovalSent(boolean actionsApproved) {
        }
    }


    private static void usage(int ret) {
        System.out.println("Usage: Benchmark -gen <number of instances> <number of servers> -sla <sla> -o <folder> [-p <prefix>]");
        System.out.println("Generate <number of instances> of data centres having <number of servers> each. Output files are stored in <folder>, with a optional <prefix> ");
        System.out.println("\nUsage: Benchmark -run <name> -sla <sla> -t <timeout> [-o output]");
        System.out.println("If '<name>' is an instance, compute a solution and print it on stdout. If '<name>' is a folder, run every instances.");
        System.exit(ret);
    }

    public static void main(String[] args) {

    	Utils.initLogger("../FIT4Green/Optimizer/src/main/config/log4j-benchmark.properties");
    	log = Logger.getLogger(Benchmark.class.getName());
    	
        if (args.length == 0) {
            usage(0);
        }

        SLAReader sla = null;
        
        if (args[0].equals("-gen")) {
        	
            if (args.length >= 7) {
            	
                int nbInstances = Integer.parseInt(args[1]);
                int nbServers = Integer.parseInt(args[2]);
                String fileAppend = "";
                if (args.length == 9) {
                	fileAppend = args[8];
            	} else {
            		fileAppend = nbServers + "_Servers_F4G_Model" ;	
            	}                
                try {
                    sla = new SLAReader(new File(args[4]));
                } catch (FileNotFoundException e) {
                    log.error(e.getMessage());
                    System.exit(1);
                }
                String output = args[6];
                log.info("Generating " + nbInstances + " models into '" + output + "'");
                log.info("Use SLA in " + args[4]);
                for (int i = 1; i <= nbInstances;) {
                    log.info("Generate model " + i + "/" + nbInstances);
                    if (generateConfiguration(sla, nbServers, output, fileAppend)) {
                        i++;
                    }
                }
            } else {
                usage(1);
            }
        } else if (args[0].equals("-run")) {
            if (args.length < 6) {
                usage(1);
            } else {

                try {
                    sla = new SLAReader(new File(args[3]));
                } catch (FileNotFoundException e) {
                    log.error(e.getMessage());
                    System.exit(1);
                }

                int timeout = Integer.parseInt(args[5]);
                File f = new File(args[1]);
                List<BenchmarkStatistics> stats = new LinkedList<BenchmarkStatistics>();
                if (f.isDirectory()) {
                    stats.addAll(runConfigurations(sla, args[1], timeout));
                } else {
                    BenchmarkStatistics st = runConfiguration(sla, args[1], timeout);
                    stats.add(st);
                }


                if (args.length == 8 && args[6].equals("-o")) {
                    PrintWriter out = null;
                    try {
                        out = new PrintWriter(args[7]);
                    for (BenchmarkStatistics st : stats) {
                        out.println(st.toRaw());
                    }
                    } catch (IOException e) {
                        System.err.println(e.getMessage());
                        System.exit(1);
                    } finally {
                        if (out != null) {
                            out.close();
                        }
                    }

                } else {
                    for (BenchmarkStatistics st : stats) {
                        System.out.println(st);
                    }
                }
            }
        } else {
            usage(1);
        }
    }
}
