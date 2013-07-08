package f4g.f4gDeveloperTools.tools.power;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import f4g.commons.com.util.PowerData;
import f4g.commons.core.IMain;
import f4g.pluginCore.core.Main;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.ObjectFactory;
import f4g.f4gDeveloperTools.tools.modelspy.ModelSpy;

/**
 * Servlet implementation class PowerMeter
 */
public class PowerMeter extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(PowerMeter.class.getName());
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public PowerMeter() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		InputStream isLog4j = this.getClass().getClassLoader().getResourceAsStream("config/log4j.properties");
		Properties log4jProperties = new Properties();
		try {
			log4jProperties.load(isLog4j);
			PropertyConfigurator.configure(log4jProperties);
			log.debug("In Power Meter servlet init... " + request.getRequestURI());
			
			response.setContentType("text/xml");
			
			//response.setHeader("Refresh", "15; URL="+request.getRequestURL().toString());
			PrintWriter out = response.getWriter();
			
			IMain f4gInstance = Main.getInstance();
			
			FIT4GreenType model = f4gInstance.getMonitor().getModelCopy();
			log.debug("In servlet init... model is " + model);
			
			PowerData w = f4gInstance.getPowerCalculator().computePowerFIT4Green(model);
			
			log.debug("Computed power is " + w.getActualConsumption());
			
			JAXBContext jc = JAXBContext.newInstance("org.f4g.schema.metamodel");
			
			ObjectFactory factory=new ObjectFactory(); 
			;
			JAXBElement<FIT4GreenType>  f4gElement=(JAXBElement<FIT4GreenType>)(factory.createFIT4Green(model)); 
			log.debug("In servlet init... factory model element is " + f4gElement);
			
			// create an Marshaller
			Marshaller m = jc.createMarshaller();
			
			
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(f4gElement, out);
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new ServletException(e);
		}				
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
