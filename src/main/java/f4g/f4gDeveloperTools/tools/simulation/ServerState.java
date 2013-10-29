package f4g.f4gDeveloperTools.tools.simulation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import f4g.commons.com.util.PowerData;
import f4g.commons.core.IMain;
import f4g.pluginCore.core.Main;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.FrameworkCapabilitiesType;
import f4g.schemas.java.metamodel.ObjectFactory;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.f4gDeveloperTools.tools.power.PowerMeter;

/**
 * Servlet implementation class ServerState
 */
public class ServerState extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(ServerState.class.getName());
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServerState() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
			try {
				InputStream isLog4j = this.getClass().getClassLoader().getResourceAsStream("f4gDeveloperTools/log4j.properties");
				Properties log4jProperties = new Properties();
				log4jProperties.load(isLog4j);
				PropertyConfigurator.configure(log4jProperties);
				
				log.debug("In ServerState servlet... " + request.getRequestURI());
				
				IMain f4gInstance = Main.getInstance();
				
				FIT4GreenType model = f4gInstance.getMonitor().getModelCopy();

				JXPathContext jc = JXPathContext.newContext(model);
				
				/* 
				 //m0:TowerServer
				 //m0:RackableServer/status
				 //m0:BladeServer/status
				 */
				
				List<ServerType> servers = new ArrayList<ServerType>();			
				
				String[] queries = {"//towerServer", "//rackableServer", "//bladeServer"};
				
				for(int i=0; i<queries.length; i++){
					
				    Iterator serversIterator = jc.iterate(queries[i]);
				   
				    // Iteration over the "FrameworkCapabilities" items
				    while(serversIterator.hasNext()){
				    	ServerType server = 
				    		(ServerType)serversIterator.next();
				    	servers.add(server);	        	
				    }	
				}
				
				request.getSession().setAttribute("SERVER_LIST", servers);
			
				RequestDispatcher dispatcher = request.getRequestDispatcher("/serverState.jsp");
				if (dispatcher != null)
					dispatcher.forward(request, response);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
