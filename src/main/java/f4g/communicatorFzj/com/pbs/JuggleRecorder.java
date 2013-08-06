/**
* ============================== Header ============================== 
* file:          Recorder.java
* project:       FIT4Green/Optimizer
* created:       24 jan. 2011 by cdupont
* last modified: $LastChangedDate: 2012-03-26 18:18:16 +0200 (Mon, 26 Mar 2012) $ by $LastChangedBy: f4g.julichde $
* revision:      $LastChangedRevision: 1244 $
* 
* short description:
*   recording facilities.
* ============================= /Header ==============================
*/

package f4g.communicatorFzj.com.pbs;


import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.log4j.Logger;
import f4g.commons.core.Configuration;
import f4g.commons.core.Constants;
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.allocation.AllocationRequestType;
import f4g.schemas.java.allocation.AllocationResponseType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.FrameworkCapabilitiesType;
import f4g.schemas.java.metamodel.ObjectFactory;
import f4g.schemas.java.metamodel.PSUType;
import f4g.schemas.java.metamodel.RackableServerType;
import f4g.schemas.java.metamodel.ServerType;
import org.xml.sax.SAXException;


/**
 * Records the different schemas in file
 */
public class JuggleRecorder {
	
	
	private Logger log;  
	
	private Boolean isRecording;
	private String recorderDirectory;
	private String name;
	
	public JuggleRecorder(String frameworkName){
		log = Logger.getLogger(JuggleRecorder.class.getName()); 
			
		try {
			isRecording = true;
			recorderDirectory = Configuration.get(Constants.RECORDER_FILE_PATH);
		} catch (NullPointerException e){
			isRecording = false;
			recorderDirectory = "";
		}
		
		name = frameworkName;
	}
	
	
	public void recordModel(FIT4GreenType model){
		
		if(isRecording) {
			
			log.debug("recording Model...");
			
			JAXBElement<FIT4GreenType> fIT4Green = (new ObjectFactory()).createFIT4Green(model);
			
			String fileName = getFileName("F4G Model");
			
			saveToXML(fIT4Green, 
					fileName, 
			          Constants.METAMODEL_FILE_NAME, //../Schemas/src/main/schema/
			          Constants.METAMODEL_PACKAGE_NAME);
			
			saveParamsToFile(model, fileName.replaceAll("xml", "txt"), 
					Constants.METAMODEL_FILE_NAME, //../Schemas/src/main/schema/
			        Constants.METAMODEL_PACKAGE_NAME);
		}
		
	}
	
public void saveParamsToFile(FIT4GreenType model, String fileName, String schemaPathName, String schemaPackage){
		
	URL schemaLocation = 
		this.getClass().getClassLoader().getResource("schema/" + schemaPathName);
	
	log.debug("schemaLocation: " + schemaLocation);
	
	String myQuery = "//rackableServer";
	JXPathContext context = JXPathContext.newContext(model);
	Iterator<?> elements = context.iterate(myQuery);
	
	java.io.FileWriter fw = null;
	try
	{
		fw = new FileWriter(fileName);
		
		// Iteration over the "rackableServer" items		
		while (elements.hasNext())
		{       	
			Object element = elements.next();
			String id = ((ServerType)element).getFrameworkID();

			FrameworkCapabilitiesType framweworkRef = 
				(FrameworkCapabilitiesType)((ServerType)element).getFrameworkRef();			
			
			boolean correct_framework = false;
			String myframeworkQuery = "//frameworkCapabilities";
			JXPathContext context_rackableServer = JXPathContext.newContext(model);		
			Iterator<?> fCapabilitiesIterator = context_rackableServer.iterate(myframeworkQuery);			
			while (fCapabilitiesIterator.hasNext()){	        	
	        	FrameworkCapabilitiesType fCapability = 
	        		(FrameworkCapabilitiesType)fCapabilitiesIterator.next();
	        	if (framweworkRef.getId().equals((fCapability.getId()))&&getName().equals(fCapability.getFrameworkName())) {	 
	        		correct_framework = true;
	        		break;
	        	}
			}
			if(!correct_framework){
				continue;
			}

			// The node is a worker node
			if(((ServerType)element).getName().toString().matches("HPC_COMPUTE_NODE"))
			{					
				DecimalFormat df =
					  (DecimalFormat)DecimalFormat.getInstance(Locale.GERMAN);
					df.applyPattern( "#,###,##0.000" );
					
				
				fw.write( id + "\n" );
				
				Double val = ((ServerType)element).getMeasuredPower().getValue();//.doubleValue();
				if(val!=null){
					String s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				if(((ServerType)element).getComputedPower()!=null){
					val = ((ServerType)element).getComputedPower().getValue();
					if(val!=null){
						String s = df.format( val );
						fw.write( s + "\n");
					}					
					else
						fw.write( 0 + "\n");
				}				

				if(((ServerType)element).getComputedPower()!=null){
					
				
				val = ((ServerType)element).getMainboard().get(0).getPowerIdle().getValue();
				String s = df.format( val );
				fw.write( s + "\n");

				if(((ServerType)element).getMainboard().get(0).getComputedPower()!=null){
					val = ((ServerType)element).getMainboard().get(0).getComputedPower().getValue();
					if(val!=null){
						s = df.format( val );
						fw.write( s + "\n");
					}					
					else
						fw.write( 0 + "\n");
				}
				else{
					fw.write( 0 + "\n");
				}

				if(((ServerType)element).getMainboard().get(0).getCPU().get(0).getComputedPower()!=null){
					val = ((ServerType)element).getMainboard().get(0).getCPU().get(0).getComputedPower().getValue();
					if(val!=null){
						s = df.format( val );
						fw.write( s + "\n");
					}					
					else
						fw.write( 0 + "\n");
				}
				else
					fw.write( 0 + "\n");
				

				val = ((ServerType)element).getMainboard().get(0).getCPU().get(0).getCore().get(0).getCoreLoad().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");
				
				val = ((ServerType)element).getMainboard().get(0).getCPU().get(0).getCore().get(1).getCoreLoad().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");
				
				val = ((ServerType)element).getMainboard().get(0).getCPU().get(0).getCpuUsage().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				val = ((ServerType)element).getMainboard().get(0).getCPU().get(0).getCore().get(0).getFrequency().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				val = ((ServerType)element).getMainboard().get(0).getCPU().get(0).getCore().get(0).getVoltage().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				if(((ServerType)element).getMainboard().get(0).getCPU().get(1).getComputedPower()!=null){
					val = ((ServerType)element).getMainboard().get(0).getCPU().get(1).getComputedPower().getValue();
					if(val!=null){
						s = df.format( val );
						fw.write( s + "\n");
					}					
					else
						fw.write( 0 + "\n");
				}
				else
					fw.write( 0 + "\n");
				
				val = ((ServerType)element).getMainboard().get(0).getCPU().get(1).getCore().get(0).getCoreLoad().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");
				
				val = ((ServerType)element).getMainboard().get(0).getCPU().get(1).getCore().get(1).getCoreLoad().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				val = ((ServerType)element).getMainboard().get(0).getCPU().get(1).getCpuUsage().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				val = ((ServerType)element).getMainboard().get(0).getCPU().get(1).getCore().get(0).getFrequency().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				val = ((ServerType)element).getMainboard().get(0).getCPU().get(1).getCore().get(0).getVoltage().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				val = ((ServerType)element).getMainboard().get(0).getMemoryUsage().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				int sticks = ((ServerType)element).getMainboard().get(0).getRAMStick().size();
				double ramStickSum = 0.0;
				for(int i=0;i<sticks;i++){
					if(((ServerType)element).getMainboard().get(0).getRAMStick().get(i).getComputedPower()!=null){
						val = ((ServerType)element).getMainboard().get(0).getRAMStick().get(i).getComputedPower().getValue();
						if(val!=null){
							if(i<2){
								s = df.format( val );
								fw.write( s + "\n");
							}						
							ramStickSum = ramStickSum + val;
						}				
						else
							fw.write( 0 + "\n");
					}
					else
						fw.write( 0 + "\n");
				}
				s = df.format( ramStickSum );
				fw.write( s + "\n");

				context = JXPathContext.newContext((RackableServerType)element);
				String psuQuery = "PSU";
				@SuppressWarnings("rawtypes")
				Iterator psuPowerIterator = context.iterate(psuQuery);
				psuPowerIterator = context.iterate(psuQuery);    	
				while(psuPowerIterator.hasNext())
				{
					PSUType myPSU = (PSUType)psuPowerIterator.next();
					if(myPSU.getComputedPower() == null || myPSU.getComputedPower().getValue() <= 0)
						val = 0.0;
					else
						val = myPSU.getComputedPower().getValue();
				}
					
				s = df.format( val );
				fw.write( s + "\n");

				int actualRPM_values = ((RackableServerType)element).getFan().size();
				int rpmSum = 0;
				for(int i=0;i<actualRPM_values;i++){
					int ival = ((RackableServerType)element).getFan().get(i).getActualRPM().getValue();
					if(ival>0){
						fw.write( ival + "\n");
						rpmSum = rpmSum + ival;
					}						
					else
						fw.write( 0 + "\n");

				}
				fw.write( rpmSum + "\n");

				double fanConsumptionSum = 0.0;
				for(int i=0;i<actualRPM_values;i++){
					if(((RackableServerType)element).getFan().get(i).getComputedPower()!=null){
						val = ((RackableServerType)element).getFan().get(i).getComputedPower().getValue();
						if(val!=null){
							s = df.format( val );
							fw.write( s + "\n");
							fanConsumptionSum = fanConsumptionSum + val;
						}						
						else
							fw.write( 0 + "\n");
					}
					else
						fw.write( 0 + "\n");
				}
				s = df.format( fanConsumptionSum );
				fw.write( s + "\n");

				val = ((ServerType)element).getMainboard().get(0).getHardDisk().get(0).getReadRate().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				val = ((ServerType)element).getMainboard().get(0).getHardDisk().get(0).getWriteRate().getValue();
				if(val!=null){
					s = df.format( val );
					fw.write( s + "\n");
				}					
				else
					fw.write( 0 + "\n");

				if(((ServerType)element).getMainboard().get(0).getHardDisk().get(0).getComputedPower()!=null){
					val = ((ServerType)element).getMainboard().get(0).getHardDisk().get(0).getComputedPower().getValue();
					if(val!=null){
						s = df.format( val );
						fw.write( s + "\n");
					}					
					else
						fw.write( 0 + "\n");					
				}
				else
					fw.write( 0 + "\n");
				}
			}
		}

	}
    catch ( IOException e ) {
      System.err.println( "Konnte Datei nicht erstellen" );
    }
    finally {
      if ( fw != null )
        try { fw.close(); } catch ( IOException e ) { e.printStackTrace(); }
    }
		
	}
	
	public void recordActionRequest(ActionRequestType actions){
		
		if(isRecording) {
			
			log.debug("recording Action Request...");
			
			JAXBElement<ActionRequestType> fit4GreenActionRequest = (new f4g.schemas.java.actions.ObjectFactory()).createActionRequest(actions);
			
			saveToXML(fit4GreenActionRequest, 
					  getFileName("F4G Action Request"), 
					  Constants.ACTIONS_FILE_NAME,
					  Constants.ACTIONS_PACKAGE_NAME); 	
		}
	}
	
	public void recordAllocationResponse(AllocationResponseType response){
		
		if(isRecording) {
			
			log.debug("recording Allocation Response...");
			
			JAXBElement<AllocationResponseType> fit4GreenAllocationResponse = 
				(new f4g.schemas.java.allocation.ObjectFactory()).createAllocationResponse(response);

			
			saveToXML(fit4GreenAllocationResponse, 
			          getFileName("F4G Allocation Response"), 
			          Constants.ALLOCATION_FILE_NAME,
			          Constants.ALLOCATION_PACKAGE_NAME); 
		}
			
	}
	
	public void recordAllocationRequest(AllocationRequestType request){
		
		if(isRecording) {
			
			log.debug("recording Allocation Request...");
			
			JAXBElement<AllocationRequestType> fit4GreenAllocationRequest = 
				(new f4g.schemas.java.allocation.ObjectFactory()).createAllocationRequest(request);

			
			saveToXML(fit4GreenAllocationRequest, 
			          getFileName("F4G Allocation Request"), 
			          Constants.ALLOCATION_FILE_NAME,
			          Constants.ALLOCATION_PACKAGE_NAME); 	
		}
		
	}
	

	private String getFileName(String recordType) {
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String date = dateFormat.format(new Date());
		
		return recorderDirectory + "/" + date + " " + recordType + ".xml";
	}
	
	public void logElement(Object element, String schemaLocation){
		
		JAXBContext jc = null;
		
		// create a Marshaller and marshal to System.out
		try {
			log.debug("element: " + element + " " + element.getClass().getName());
			if(schemaLocation.contains("MetaModel.")){
				jc = JAXBContext.newInstance(Constants.METAMODEL_PACKAGE_NAME);
			} else 
				if(schemaLocation.contains("ActionRequest.")){
				jc = JAXBContext.newInstance(Constants.ACTIONS_PACKAGE_NAME);
			} else
			if(schemaLocation.contains("AllocationRequest.") || schemaLocation.contains("AllocationResponse.")){
				jc = JAXBContext.newInstance(Constants.ALLOCATION_PACKAGE_NAME);
			} 
			
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			//m.marshal(element, System.out);
		} catch (PropertyException e) {
			log.error(e);
		} catch (JAXBException e) {
			log.error(e);
		}

	}

	private void saveToXML(Object objectToSave, String filePathName, String schemaPathName, String schemaPackage) {
		
		URL schemaLocation = 
			this.getClass().getClassLoader().getResource("schema/" + schemaPathName);
		
		log.debug("schemaLocation: " + schemaLocation);

		try {
			log.debug("**** Logging element: " + filePathName);
			logElement(objectToSave, schemaLocation.toString());
			java.io.FileWriter fw = new FileWriter(filePathName);
			
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = schemaFactory.newSchema(schemaLocation);
			// create an marshaller
			Marshaller marshaller = JAXBContext.newInstance(schemaPackage).createMarshaller();
			marshaller.setSchema(schema);

			// unmarshal a tree of Java content into an XML document
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
	        marshaller.marshal(objectToSave, fw);
	        
		} catch (JAXBException e) {
			log.debug("Error when unmarchalling");
			log.error(e);
		} catch (IOException e) {
			log.debug("Error when unmarchalling");
			log.error(e);
		}  catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	
}
