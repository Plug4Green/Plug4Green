package f4g.f4gDeveloperTools.tools.simulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import f4g.commons.core.IMain;
import f4g.pluginCore.core.Main;
import f4g.schemas.java.metamodel.ServerStatusType;
import f4g.schemas.java.metamodel.ServerType;
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.actions.PowerOffActionType;
import f4g.schemas.java.actions.PowerOnActionType;

/**
 * Servlet implementation class HandleServerAction
 */
public class HandleServerAction extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(HandleServerAction.class.getName());
      
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HandleServerAction() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String frameworkId = request.getParameter("frameworkId");
		String action = request.getParameter("serverAction");

		ActionRequestType.ActionList actionList = new ActionRequestType.ActionList();
		org.f4g.schema.actions.ObjectFactory actionFactory = new org.f4g.schema.actions.ObjectFactory();
	    		
		log.debug("Action is: " + action);
		log.debug("frameworkId is: " + frameworkId);
		
		if(!"ALL".equalsIgnoreCase(frameworkId)){
			if("OFF".equalsIgnoreCase(action)){
				PowerOffActionType pOff = new PowerOffActionType();
				pOff.setNodeName(frameworkId);
				pOff.setFrameworkName("ComHP");
				actionList.getAction().add(actionFactory.createPowerOff(pOff));
			} else if("ON".equalsIgnoreCase(action)){
				PowerOnActionType pOn = new PowerOnActionType();
				pOn.setNodeName(frameworkId);
				pOn.setFrameworkName("ComHP");
				actionList.getAction().add(actionFactory.createPowerOn(pOn));			
			}
		} else {
			List<ServerType> servers = 	(ArrayList<ServerType>)request.getSession().getAttribute("SERVER_LIST");
			for(ServerType server : servers){
				
				if("OFF".equalsIgnoreCase(action) && server.getStatus().equals(ServerStatusType.ON)){
					PowerOffActionType pOff = new PowerOffActionType();
					pOff.setNodeName(server.getFrameworkID());
					pOff.setFrameworkName("ComHP");
					actionList.getAction().add(actionFactory.createPowerOff(pOff));
				} else if("ON".equalsIgnoreCase(action) && server.getStatus().equals(ServerStatusType.OFF)){
					PowerOnActionType pOn = new PowerOnActionType();
					pOn.setNodeName(server.getFrameworkID());
					pOn.setFrameworkName("ComHP");
					actionList.getAction().add(actionFactory.createPowerOn(pOn));			
				}		
			}
		}

		//actionList.getAction().get(0).getValue().setFrameworkName("ComHP");
		ActionRequestType actionRequest = new ActionRequestType();
		actionRequest.setActionList(actionList);		

		IMain f4gInstance = Main.getInstance();
		
		log.debug("Forwarding action to the controller");
		log.debug("Action is: " + actionRequest.getActionList().getAction().get(0).getDeclaredType() + " on server " + frameworkId);
		boolean res = f4gInstance.getController().executeActionList(actionRequest);

		log.debug(res?"Action done":"Action not done");

		RequestDispatcher dispatcher = request.getRequestDispatcher("/ServerState");
		if (dispatcher != null)
			dispatcher.forward(request, response);


}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
