package f4g.initServlet.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import f4g.commons.core.Configuration;
import f4g.commons.core.Constants;
import f4g.commons.core.IMain;
import f4g.pluginCore.core.Main;

/**
 * Servlet implementation class InitServlet
 */
public class InitServlet extends HttpServlet {
	static Logger log = Logger.getLogger(InitServlet.class.getName());
	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public InitServlet() {
        super();
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		InputStream isLog4j = this.getClass().getClassLoader().getResourceAsStream("initServlet/log4j.properties");
		Properties log4jProperties = new Properties();
		try {
			log4jProperties.load(isLog4j);
			PropertyConfigurator.configure(log4jProperties);
			log.debug("In servlet init...");
			
			IMain f4gInstance = Main.getInstance();
			
			Configuration configuration = new Configuration(config.getInitParameter("configuration"));
			log.debug("Got configuration");
			
			boolean automaticStartup = Boolean.parseBoolean(configuration.get(Constants.AUTOMATIC_STARTUP));
			log.debug("automaticStartup: " + automaticStartup);
			
			boolean res = true;
			if(automaticStartup){
				log.debug("Starting up the f4g instance...");
				res = f4gInstance.startup();
				if(!res){
					throw new Exception("Error in launching the f4g framework");
				}
			} else {
				log.debug("automaticStartup is set to 'false'. f4g instance not launched");				
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e);
		}				
	}
}
