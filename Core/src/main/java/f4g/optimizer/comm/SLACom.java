package f4g.optimizer.comm;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import f4g.manager.monitor.Monitor;
import f4g.optimizer.cloud.OptimizerEngineCloud;

//SLA REST interface 
public class SLACom extends Thread {

	private SLAService svc;
	private Server server;
	private Logger logger = LoggerFactory.getLogger(SLACom.class);
	
    public SLACom(OptimizerEngineCloud opti, Monitor monitor) {

        this.svc = new SLAService(opti, monitor);

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(svc);
        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder sh = new ServletHolder(servletContainer);
        server = new Server(7777);
        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(sh, "/*");
    } 
           
    @Override
    public void run() {
        startServer();
    }

    
    public void stopServer() {
        try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private void startServer() {

        try {
        	logger.debug("plug4Green: Listening to SLA requests");
            server.start();
            server.join();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
