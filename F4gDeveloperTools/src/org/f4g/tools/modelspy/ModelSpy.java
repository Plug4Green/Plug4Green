package org.f4g.tools.modelspy;

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
import org.f4g.core.Configuration;
import org.f4g.core.IMain;
import org.f4g.core.Main;
import org.f4g.schema.metamodel.FIT4GreenType;
import org.f4g.schema.metamodel.ObjectFactory;




/**
 * Servlet implementation class ModelSpy
 */
public class ModelSpy extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(ModelSpy.class.getName());
	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ModelSpy() {
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
			log.debug("In Model Spy servlet init... " + request.getRequestURI());
			
			response.setContentType("text/xml");
			
			//response.setHeader("Refresh", "15; URL="+request.getRequestURL().toString());
			PrintWriter out = response.getWriter();
			
			IMain f4gInstance = Main.getInstance();
			
			FIT4GreenType model = f4gInstance.getMonitor().getModelCopy();
			log.debug("In servlet init... model is " + model);
			//Cloner cloner=new Cloner();
			//FIT4GreenType modelClone = cloner.deepClone(model);
			log.debug("In servlet init... CLONED model is " + model);
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

		/*
		* Get the value of form parameter
		*/
		String name = request.getParameter("name");
		String welcomeMessage = "Welcome "+name;
		/*
		* Set the content type(MIME Type) of the response.
		*/
		response.setContentType("text/html");
		 
		PrintWriter out = response.getWriter();
		/*
		* Write the HTML to the response
		*/
		out.println("<html>");
		out.println("<head>");
		out.println("<title> A very simple servlet example</title>");
		out.println("</head>");
		out.println("<body>");
		out.println("<h1>"+welcomeMessage+"</h1>");
		out.println("<a href=\"/servletexample/pages/form.html\">"+"Click here to go back to input page "+"</a>");
		out.println("</body>");
		out.println("</html>");
		out.close();
		 

	}

}
