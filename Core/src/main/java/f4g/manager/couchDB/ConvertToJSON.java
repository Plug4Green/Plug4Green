/**
* ============================== Header ============================== 
* file:          ConvertToJSON.java
* project:       FIT4Green/Manager
* created:       16 sep. 2011 by vicky@almende.org
* 
* $LastChangedDate: 2011-10-25 17:13:45 +0200 (mar, 25 oct 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 967 $
* 
* short description:
*   Utility class to marshall an object tree to XML and convert the
*   latter to JSON string
* ============================= /Header ==============================
*/
package f4g.manager.couchDB;

import java.io.StringWriter;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import net.sf.json.JSON;
import net.sf.json.xml.XMLSerializer;

import f4g.commons.core.Constants;
import f4g.schemas.java.actions.ActionRequest;
import f4g.schemas.java.metamodel.FIT4Green;
import org.xml.sax.SAXException;

/**
 * Utility class to marshall an object tree to XML and convert the
 * latter to JSON string
 *
 * @author Vasiliki Georgiadou
 */
public class ConvertToJSON {
	
	/**
	 * Marshalls an object to XML and converts the later to JSON
	 * 
	 * @param actions	The action request type
	 * @return			JSON as string
	 */
	public String convert (ActionRequest actions) {
		
		String result = null;
		
		try {			
//			File temp = File.createTempFile("temp", ".xml");
//			FileOutputStream fos = new FileOutputStream(temp);
			
			StringWriter s = new StringWriter();
		
			String schemaFileName = Constants.ACTIONS_FILE_NAME;
			String schemaPackageName = Constants.ACTIONS_PACKAGE_NAME;
		
			URL schemaLocation = 
				this.getClass().getClassLoader().getResource("schema/" + schemaFileName);
			SchemaFactory schemaFactory = 
				SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(schemaLocation);
			
			Marshaller marshaller = 
				JAXBContext.newInstance(schemaPackageName).createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
			f4g.schemas.java.actions.ObjectFactory obj = new f4g.schemas.java.actions.ObjectFactory();
			marshaller.marshal(obj.createActionRequest(actions), s);
			
//			fos.close();
			
//			InputStream is = new BufferedInputStream(new FileInputStream(temp));
//			String xml = IOUtils.toString(s);
			String xml = s.toString();
			XMLSerializer xmlSerializer = new XMLSerializer(); 
		    JSON json = xmlSerializer.read(xml);
		    result = json.toString(2);
		    
//		    is.close();
//		    temp.delete();
		    
		} catch (SAXException e) {
			e.printStackTrace();
//		} catch (IOException e1) {
//			e1.printStackTrace();
		} catch (JAXBException e2) {
			e2.printStackTrace();
		}
	    
		return result;
	}
	
	/**
	 * Marshalls an object to XML and converts the later to JSON
	 * 
	 * @param model		The FIT4Green type
	 * @return			JSON as string
	 */
	public String convert (FIT4Green model) {
		
		String result = null;
		
		try {
//			File temp = File.createTempFile("temp", ".xml");
//			FileOutputStream fos = new FileOutputStream(temp);
			
			StringWriter s = new StringWriter();
		
			String schemaFileName = Constants.METAMODEL_FILE_NAME;
			String schemaPackageName = Constants.METAMODEL_PACKAGE_NAME;
		
			URL schemaLocation = 
				this.getClass().getClassLoader().getResource("schema/" + schemaFileName);
			SchemaFactory schemaFactory = 
				SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(schemaLocation);
			
			Marshaller marshaller = 
				JAXBContext.newInstance(schemaPackageName).createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
			f4g.schemas.java.metamodel.ObjectFactory obj = new f4g.schemas.java.metamodel.ObjectFactory();
			marshaller.marshal(obj.createFIT4Green(model), s);
			
//			fos.close();
			
//			InputStream is = new BufferedInputStream(new FileInputStream(temp));
//			String xml = IOUtils.toString(is);
			String xml = s.toString();
			XMLSerializer xmlSerializer = new XMLSerializer(); 
		    JSON json = xmlSerializer.read(xml);
		    result = json.toString(2);
		    
//		    is.close();
//		    temp.delete();
		    
		} catch (SAXException e) {
			e.printStackTrace();
//		} catch (IOException e1) {
//			e1.printStackTrace();
		} catch (JAXBException e2) {
			e2.printStackTrace();
		}
	    
		return result;
	}

}
