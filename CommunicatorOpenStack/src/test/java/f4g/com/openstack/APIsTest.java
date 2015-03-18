package f4g.com.openstack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.actions.LiveMigrateOptions;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.compute.ext.Migration;
import org.openstack4j.model.compute.ext.MigrationsFilter;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.identity.Tenant;
import org.openstack4j.model.identity.User;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.yaml.snakeyaml.Yaml;

import junit.framework.TestCase;

public class APIsTest extends TestCase {
	OSClient admin;
	Optional<Tenant> tenantAdmin;
	Optional<Network> networkTest;
	Optional<Subnet> subnetTest;
	Server serverTest;

	String serverName1 = "Test-1-Openstack4J";

	@Override
	protected void setUp() {

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
		}
		
		assertNotNull(admin);
		for (Tenant tenant : admin.identity().tenants().list()) {
			if (tenant.getName().equals("admin")) {
				tenantAdmin = Optional.ofNullable(tenant);
			}
		}
		assertTrue(tenantAdmin.isPresent());
		
		networkTest = createNetwork();

		serverTest = createAndBootServer(serverName1, networkTest.get());
		
	}

	@Override
	protected void tearDown() {
		if (admin != null) {
				admin.compute().servers().delete(serverTest.getId());
				admin.compute()
						.servers()
						.waitForServerStatus(serverTest.getId(), Status.DELETED,
								120, TimeUnit.SECONDS);
			subnetTest.ifPresent(subnet -> admin.networking().subnet()
					.delete(subnet.getId()));
			networkTest.ifPresent(network -> admin.networking().network()
					.delete(network.getId()));
		}

	}
	
	  @Test
	  public void fails() {
	      tearDown();
	  }


	public void testMigration() {
		assertTrue("Live migration test requires at least two compute node",
				admin.compute().hypervisors().list().size() > 1);

		String hyperVisor = admin.compute().servers().get(serverTest.getId())
				.getHypervisorHostname();

		Hypervisor otherHypervisor = null;
		for (Hypervisor hyper : admin.compute().hypervisors().list()) {
			if (!hyper.equals(hyperVisor)) {
				otherHypervisor = hyper;
				break;
			}

		}
		assertNotNull(otherHypervisor);
		LiveMigrateOptions options = LiveMigrateOptions.create().host(
				otherHypervisor.getHypervisorHostname().replace(".domain.tld",""));
		admin.compute().servers().liveMigrate(serverTest.getId(), options);

		for (int i = 0; i < 120; i++){
			assertNotNull(admin.compute().servers().get(serverTest.getId()));
			if(admin.compute().servers().get(serverTest.getId()).getHypervisorHostname().equals(otherHypervisor.getHypervisorHostname())){
				break;
			}
			try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		assertTrue(admin.compute().servers().get(serverTest.getId()).getHypervisorHostname().equals(otherHypervisor.getHypervisorHostname()));
	}
	
	public void testStats(){
		assertNotNull(admin.compute().hypervisors().statistics().getCurrentWorkload());
		assertNotNull(admin.compute().hypervisors().statistics().getVirtualUsedCPU());
		assertNotNull(admin.compute().hypervisors().statistics().getFreeRam());
		
	}

	public void testPosition(){
		
		assertNotNull(admin.compute().servers().get(serverTest.getId()).getVmState());
		assertNotNull(admin.compute().hypervisors().statistics().getRunningVM());
	}
	
	protected Optional<Network> createNetwork() {
		networkTest = Optional.ofNullable(admin
				.networking()
				.network()
				.create(Builders.network().name("test-openstack4j")
						.tenantId(tenantAdmin.get().getId()).build()));
		assertTrue(networkTest.isPresent());

		subnetTest = Optional.ofNullable(admin
				.networking()
				.subnet()
				.create(Builders.subnet().name("TestSubnet")
						.networkId(networkTest.get().getId())
						.tenantId(tenantAdmin.get().getId())
						.ipVersion(IPVersionType.V4).cidr("192.168.151.0/24")
						.addPool("192.168.151.2", "192.168.151.254")
						.enableDHCP(true).build()));

		assertTrue(subnetTest.isPresent());
		return networkTest;
	}

	protected Server createAndBootServer(String serverName, Network network) {
		Flavor flavor = admin.compute().flavors().get("1");
		assertNotNull(flavor);
		Image TestVM = null;
		for (Image img : admin.compute().images().list()) {
			if (img.getName().trim().equals("TestVM")) {
				TestVM = img;
				assertNotNull(img);
				break;
			}
		}

		assertEquals(TestVM.getName().trim(), "TestVM");

		ServerCreate sc = Builders.server().name(serverName).flavor(flavor)
				.networks(Collections.singletonList(network.getId()))
				.image(TestVM).build();
		assertNotNull(sc);

		Server server = admin.compute().servers().bootAndWaitActive(sc, 120000);
		assertNotNull(server);

		return server;
	}


}
