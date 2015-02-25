package f4g.com.openstack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.compute.actions.LiveMigrateOptions;
import org.openstack4j.openstack.OSFactory;
import org.yaml.snakeyaml.Yaml;

import f4g.commons.com.AbstractCom;
import f4g.schemas.java.actions.LiveMigrateVMAction;
import f4g.schemas.java.actions.MoveVMAction;
import f4g.schemas.java.actions.PowerOffAction;
import f4g.schemas.java.actions.PowerOnAction;
import f4g.schemas.java.actions.StandByAction;
import f4g.schemas.java.actions.StartJobAction;


public class ComOpenstack extends AbstractCom {
   
    OSClient admin;

    @Override
    public boolean powerOn(PowerOnAction action) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean powerOff(PowerOffAction action) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean liveMigrate(LiveMigrateVMAction action) {
	
	LiveMigrateOptions options = LiveMigrateOptions.create().host(action.getDestNodeController());
		//otherHypervisor.getHypervisorHostname().replace(".domain.tld",""));
	if (!admin.compute().servers().liveMigrate(action.getVirtualMachine(), options).isSuccess()) {
	    return false;
	}
	//TODO: should we check if we need to wait until the migration is done?
	return true;
    }

    @Override
    public boolean moveVm(MoveVMAction action) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean startJob(StartJobAction action) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public boolean standBy(StandByAction action) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public void run() {

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

	admin = OSFactory.builder().endpoint("http://" + config.get("ip") + ":" + config.get("port") + "/v2.0")
		.credentials(config.get("user"), config.get("password")).tenantName(config.get("tenant"))
		.authenticate();

    }
}
