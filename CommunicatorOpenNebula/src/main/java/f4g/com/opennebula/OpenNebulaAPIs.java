package f4g.com.opennebula;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.host.HostPool;
import org.opennebula.client.host.Host;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaAPIs {

    private final Logger log = Logger.getLogger(getClass());

    private String datacenter = "datacenter.xml";

    private Client oneClient;

    private Map<String, Map<String, String>> config2;

    public boolean init() {
	// CONFIG LOADER
	InputStream input = null;
	InputStream input2 = null;
	try {

	    input = new FileInputStream(new File(
		    "src/main/config/ComOpennebula/config.yaml"));

	input2 = new FileInputStream(new File("src/main/config/ComOpennebula/confighosts.yaml"));

	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	Yaml yaml = new Yaml();
	Map<String, String> config = (Map<String, String>) yaml.load(input);
	config2 = (Map<String, Map<String, String>>) yaml.load(input2);
	try {
	    oneClient = new Client(config.get("credentials"), config.get("endpoint"));
	} catch (ClientConfigurationException e) {
	    log.error("Connection to OpenNebula fails: {}", e);
	    return false;

	}
	return true;
    }

    public Set<String> getComputeNames() {
	Set<String> hyperVisors = new HashSet<String>();
	HostPool hp = new HostPool(oneClient);
	hp.info();
	Iterator it = hp.iterator();
	Host host;
	String name;
	while (it.hasNext()){
		host = (Host) it.next();
		name = host.getName();
		log.info("Computes: " + name);
		hyperVisors.add(name);
	}
	return hyperVisors;
    }

    public String getPowerState(String hyperVisorId) {
	Host host = new Host((new Integer(config2.get(hyperVisorId).get("id"))).intValue(), oneClient);
	host.info();

	String state = host.stateStr();

        InputStream input3 = null;
        try {

            input3 = new FileInputStream(new File("src/main/config/Ipmi/config.yaml"));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Yaml yaml = new Yaml();
        Map<String, Map<String, String>> config = (Map<String, Map<String, String>>) yaml.load(input3);
        String ipipmi = config.get(hyperVisorId).get("ipipmi");
        Ipmi ipmi = new Ipmi();
        ipmi.init(ipipmi);
	String status = ipmi.getStatus();


	if (state.equals("INIT")||(state.equals("ERROR")&&status.equals("up"))) state="POWERING_ON";
	else if (state.equals("ERROR")&&status.equals("down")) state="POWERING_OFF";
	else if (state.equals("DISABLED")) state="OFF";
	else if (state.startsWith("MONITOR")) state="ON";


	// TODO: need to keep track of the hypervisor on/off
	return state;
    }

    public Optional<Integer> getCPU(String hyperVisorId) {
        Host host = new Host((new Integer(config2.get(hyperVisorId).get("id"))).intValue(), oneClient);
        host.info();
	String xpath = host.xpath("/HOST/HOST_SHARE/MAX_CPU/text()");
	int totalvcpu = 0;
	if (xpath!=null) totalvcpu = ((((new Integer(xpath)).intValue())/100)/2);
	log.info("CPU: " + totalvcpu);
	return Optional.ofNullable(new Integer(totalvcpu));
    }

    // Not used
    public Optional<Integer> getDisk(String hyperVisorName) {
	return Optional.empty();
    }

    public Optional<Integer> getCurrentWorkload(String hyperVisorName) {
	//not given in OpenNebula
	return Optional.empty();
    }

    public Optional<Integer> getUsedRAM(String hyperVisorId) {
        Host host = new Host((new Integer(config2.get(hyperVisorId).get("id"))).intValue(), oneClient);
        host.info();
        String xpath = host.xpath("/HOST/HOST_SHARE/MEM_USAGE/text()");
	int usedRAM = 0;
	if (xpath!=null) usedRAM = ((new Integer(xpath)).intValue())/1024;
        log.info("usedRAM: " + usedRAM);
        return Optional.ofNullable(new Integer(usedRAM));
    }

    public Optional<Integer> getUsedVirtualCPU(String hyperVisorId) {
        Host host = new Host((new Integer(config2.get(hyperVisorId).get("id"))).intValue(), oneClient);
        host.info();
        String xpath = host.xpath("/HOST/HOST_SHARE/CPU_USAGE/text()");
	int usedVirtualCPU = 0;
        if (xpath!=null) usedVirtualCPU = ((((new Integer(xpath)).intValue())/100)/2);
        log.info("usedVirtualCPU: " + usedVirtualCPU);
        return Optional.ofNullable(new Integer(usedVirtualCPU));
    }

    public Optional<Integer> getFreeDisk(String hyperVisorId) {
        Host host = new Host((new Integer(config2.get(hyperVisorId).get("id"))).intValue(), oneClient);
        host.info();
        String xpath = host.xpath("/HOST/HOST_SHARE/FREE_DISK/text()");
	int FreeDisk=0;
        if (xpath!=null) FreeDisk = (((new Integer(xpath)).intValue())/1024);
        log.info("FreeDisk: " + FreeDisk);
        return Optional.ofNullable(new Integer(FreeDisk));
    }

    public Set<String> getVMsId(String hyperVisorId) {
	Set<String> vmNames = new HashSet<String>();
        Host host = new Host((new Integer(config2.get(hyperVisorId).get("id"))).intValue(), oneClient);
        host.info();
	String xpath = host.xpath("count(/HOST/VMS/ID)");
	if (xpath!=null){
	int numVms = (new Integer(xpath)).intValue();
	for (int j=1; j<numVms+1; j++)
	{
		xpath = host.xpath("/HOST/VMS/ID["+j+"]/text()");
		log.info("VM founded: " + xpath);
		vmNames.add(xpath);
	}
	}
	return vmNames;
    }

    public Optional<Integer> getVMCPUs(Integer vmId) {
	VirtualMachine vm = new VirtualMachine(vmId.intValue(), oneClient);
	vm.info();
	//String xpath = vm.xpath("/VM/TEMPLATE/CPU/text()");
	String xpath = vm.xpath("/VM/TEMPLATE/VCPU");
	return Optional.ofNullable(new Integer(xpath));
    }

	public boolean migrateVM(String destServerId, Integer vmId){
		VirtualMachine vm = new VirtualMachine(vmId.intValue(), oneClient);
		vm.migrate((new Integer(config2.get(destServerId).get("id"))).intValue(), false);

	        int i = 1;
	        while (i < 120) {
        	    i++;
        	    if ((getVMsId(destServerId).contains(""+vmId.intValue()))&&(vm.status().equals("RUNNING"))) {
	                return true;
        	    }
	            try {
        	        Thread.currentThread().sleep(5000);
	            } catch (InterruptedException e1) {
        	        // TODO Auto-generated catch block
                	e1.printStackTrace();
	            }
        	}
		return false;
	}

	public void enableHost(String hyperVisorId){
		Host host = new Host((new Integer(config2.get(hyperVisorId).get("id"))).intValue(), oneClient);
		host.info();
		host.enable();
	}

        public void disableHost(String hyperVisorId){
                Host host = new Host((new Integer(config2.get(hyperVisorId).get("id"))).intValue(), oneClient);
                host.info();
                host.disable();
        }

}
