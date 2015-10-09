package f4g.com.openstack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.actions.LiveMigrateOptions;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.openstack.OSFactory;
import org.yaml.snakeyaml.Yaml;

public class OpenStackAPIs {

    private final Logger log = Logger.getLogger(getClass());

    private String datacenter = "datacenter.xml";

    private OSClient admin;

    public boolean init() {
	// CONFIG LOADER
	InputStream input = null;
	try {

	    input = new FileInputStream(new File(
		    "src/main/config/ComOpenstack/config.yaml"));
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	Yaml yaml = new Yaml();
	Map<String, String> config = (Map<String, String>) yaml.load(input);
	try {
	    admin = OSFactory
		    .builder()
		    .endpoint(
			    "http://" + config.get("ip") + ":"
				    + config.get("port") + "/v2.0")
		    .credentials(config.get("user"), config.get("password"))
		    .tenantName(config.get("tenant")).authenticate();
	} catch (AuthenticationException e) {
	    log.error("Connection to OpenStack datacenter fails: {}", e);
	    return false;

	}
	return true;
    }

    public Set<String> getComputeNames() {
	Set<String> hyperVisors = new HashSet<String>();
	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    hyperVisors.add(hyperVisor.getHypervisorHostname());
	}
	return hyperVisors;
    }

    public String getPowerState(String hyperVisorName) {
	// TODO: need to keep track of the hypervisor on/off
	return "";
    }

    public Optional<Integer> getCPU(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		log.info("CPU: " + hyperVisor.getVirtualCPU());
		return Optional.ofNullable(hyperVisor.getVirtualCPU());
	    }
	}
	return Optional.empty();
    }

    // Not used
    public Optional<Integer> getDisk(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		return Optional.ofNullable(hyperVisor.getLocalDisk());
	    }
	}
	return Optional.empty();
    }

    public Optional<Integer> getCurrentWorkload(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		return Optional.ofNullable(hyperVisor.getCurrentWorkload());
	    }
	}
	return Optional.empty();
    }

    public Optional<Integer> getUsedRAM(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		return Optional.ofNullable(hyperVisor.getLocalMemoryUsed());
	    }
	}
	return Optional.empty();
    }

    public Optional<Integer> getUsedVirtualCPU(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		return Optional.ofNullable(hyperVisor.getVirtualCPU()
			/ hyperVisor.getVirtualUsedCPU());
	    }
	}
	return Optional.empty();
    }

    public Optional<Integer> getFreeDisk(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		return Optional.ofNullable(hyperVisor.getFreeDisk());
	    }
	}
	return Optional.empty();
    }

    public Set<String> getVMsId(String hyperVisorName) {
	Set<String> vmNames = new HashSet<String>();
	for (Server vm : admin.compute().servers().list()) {
	    if (vm.getHypervisorHostname().equals(hyperVisorName)) {
		if(!vm.getId().isEmpty() && vm.getStatus()==Status.ACTIVE) {
		log.info("VM founded: " + vm.getId());
		vmNames.add(vm.getId());
		}
	    }
	}
	return vmNames;
    }

    public Optional<Integer> getVMCPUs(String vmId) {
	return Optional.ofNullable(admin.compute().servers().get(vmId)
		.getFlavor().getVcpus());
    }
    
    public Optional<String> getFlavorName(String vmId){
		String flavorName;
		try {
			return Optional.ofNullable(admin.compute().servers().get(vmId).getFlavor().getName());
		} catch (Exception e) {
			e.printStackTrace();
			return Optional.empty();
		}
    }


}
