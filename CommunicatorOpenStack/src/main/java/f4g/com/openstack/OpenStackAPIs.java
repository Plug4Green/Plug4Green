package f4g.com.openstack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.model.compute.Server;
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
	    log.info("Computes: " + hyperVisor.getHypervisorHostname());
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
	    if (hyperVisor.getHypervisorHostname().split(".")[0].equals(hyperVisorName)) {
		    log.info("CPU: " + hyperVisor.getVirtualCPU());
		return Optional.ofNullable(hyperVisor.getVirtualCPU());
	    }
	}
	return Optional.empty();
    }

    //Not used
    public Optional<Integer> getDisk(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().split(".")[0].equals(hyperVisorName)) {
		return Optional.ofNullable(hyperVisor.getLocalDisk());
	    }
	}
	return Optional.empty();
    }

    public Optional<Integer> getCurrentWorkload(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		 log.info("WorkLoad: " + hyperVisor.getCurrentWorkload());
		return Optional.ofNullable(hyperVisor.getCurrentWorkload());
	    }
	}
	return Optional.empty();
    }

    public Optional<Integer> getUsedRAM(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    log.info("hypervisor name: " + hyperVisor.getHypervisorHostname());
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		log.info("Local RAM: " + hyperVisor.getLocalMemory() + "Used RAM: " +  hyperVisor.getLocalMemoryUsed());
		return Optional.ofNullable(hyperVisor.getLocalMemoryUsed());
	    }
	}
	return Optional.empty();
    }

    public Optional<Integer> getUsedVirtualCPU(String hyperVisorName) {

	for (Hypervisor hyperVisor : admin.compute().hypervisors().list()) {
	    if (hyperVisor.getHypervisorHostname().equals(hyperVisorName)) {
		return Optional.ofNullable(hyperVisor.getVirtualCPU() / hyperVisor.getVirtualUsedCPU());
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
	log.info("VMs list is empty: " + admin.compute().servers().list().isEmpty());
	for (Server vm : admin.compute().servers().list()) {
	    if (vm.getHypervisorHostname().equals(hyperVisorName)) {
		log.info("VM saved: " + vm.getId());
		vmNames.add(vm.getId());
	    }
	}
	return vmNames;
    }
    
    public Optional<Integer> getVMCPUs(String vmId){
	log.info("Vcpu..." + admin.compute().servers().get(vmId).getFlavor().getVcpus());
	return Optional.ofNullable(admin.compute().servers().get(vmId).getFlavor().getVcpus());
    }

    public boolean liveMigrate(String dstServerId, String vmId) {
	LiveMigrateOptions options = LiveMigrateOptions.create().host(
		dstServerId);
	if (!admin.compute().servers().liveMigrate(vmId, options).isSuccess()) {
	    return false;
	}
	// TODO: should we check if we need to wait until the migration is done?
	return true;
    }

}
