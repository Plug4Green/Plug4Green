/**
 * ============================== Header ==============================
 * file:          Recorder.java
 * project:       FIT4Green/Optimizer
 * created:       24 jan. 2011 by cdupont
 * last modified: $LastChangedDate: 2012-07-05 16:23:09 +0200 (jeu. 05 juil. 2012) $ by $LastChangedBy: f4g.cnit $
 * revision:      $LastChangedRevision: 1512 $
 *
 * short description:
 *   recording facilities.
 * ============================= /Header ==============================
 */

package f4g.optimizer.utils;


import org.apache.log4j.Logger;
import f4g.commons.core.Configuration;
import f4g.commons.core.Constants;
import f4g.schemas.java.actions.ActionRequestType;
import f4g.schemas.java.allocation.AllocationRequestType;
import f4g.schemas.java.allocation.AllocationResponseType;
import f4g.schemas.java.metamodel.FIT4GreenType;
import f4g.schemas.java.metamodel.ObjectFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//import f4g.schemas.java.request.action.ObjectFactory;


/**
 * Records the different schemas in file
 */
public class Recorder {


    private Logger log;

    private Boolean isRecording;
    private String recorderDirectory;
    private String prefix;

    public Recorder() {
        log = Logger.getLogger(Recorder.class.getName());

        try {
            isRecording = Boolean.parseBoolean(Configuration.get(Constants.RECORDER_IS_ON));
            recorderDirectory = Configuration.get(Constants.RECORDER_FILE_PATH);
            prefix = "";
        } catch (NullPointerException e) {
            isRecording = false;
            recorderDirectory = "";
            prefix = "";
        }

    }

    public Recorder(boolean myIsRecording, String myRecorderDirectory, String myPrefix) {
        log = Logger.getLogger(Recorder.class.getName());

        isRecording = myIsRecording;
        recorderDirectory = myRecorderDirectory;
        prefix = myPrefix;

    }


    public void recordModel(FIT4GreenType model) {

        if (isRecording) {

            log.debug("recording Model...");

            (new File(recorderDirectory)).mkdirs();

            JAXBElement<FIT4GreenType> fIT4Green = (new ObjectFactory()).createFIT4Green(model);

            saveToXML(fIT4Green,
                    getFileName(prefix),
                    Constants.METAMODEL_FILE_NAME, //../Schemas/src/main/schema/
                    Constants.METAMODEL_PACKAGE_NAME);

        }


    }

    public void recordActionRequest(ActionRequestType actions) {

        if (isRecording) {

            log.debug("recording Action Request...");

            JAXBElement<ActionRequestType> fit4GreenActionRequest = (new f4g.schemas.java.actions.ObjectFactory()).createActionRequest(actions);

            saveToXML(fit4GreenActionRequest,
                    getFileName("F4G Action Request"),
                    Constants.ACTIONS_FILE_NAME,
                    Constants.ACTIONS_PACKAGE_NAME);
        }
    }

    public void recordAllocationResponse(AllocationResponseType response) {

        if (isRecording) {

            log.debug("recording Allocation Response...");

            JAXBElement<AllocationResponseType> fit4GreenAllocationResponse =
                    (new f4g.schemas.java.allocation.ObjectFactory()).createAllocationResponse(response);


            saveToXML(fit4GreenAllocationResponse,
                    getFileName("F4G Allocation Response"),
                    Constants.ALLOCATION_FILE_NAME,
                    Constants.ALLOCATION_PACKAGE_NAME);
        }

    }

    public void recordAllocationRequest(AllocationRequestType request) {

        if (isRecording) {

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

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_S");

        String date = dateFormat.format(new Date());

        return recorderDirectory + "/" + date + "_" + recordType + ".xml";
    }

    public void logElement(Object element, String schemaLocation) {

        JAXBContext jc = null;

        // create a Marshaller and marshal to System.out
        try {
            log.debug("element: " + element + " " + element.getClass().getName());
            if (schemaLocation.contains("MetaModel.")) {
                jc = JAXBContext.newInstance(Constants.METAMODEL_PACKAGE_NAME);
            } else if (schemaLocation.contains("ActionRequest.")) {
                jc = JAXBContext.newInstance(Constants.ACTIONS_PACKAGE_NAME);
            } else if (schemaLocation.contains("AllocationRequest.") || schemaLocation.contains("AllocationResponse.")) {
                jc = JAXBContext.newInstance(Constants.ALLOCATION_PACKAGE_NAME);
            }

            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(element, System.out);
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
            //logElement(objectToSave, schemaLocation.toString());
            java.io.FileWriter fw = new FileWriter(filePathName);

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaLocation);
            // create an marshaller
            Marshaller marshaller = JAXBContext.newInstance(schemaPackage).createMarshaller();
            marshaller.setSchema(schema);

            // unmarshal a tree of Java content into an XML document
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(objectToSave, fw);

        } catch (JAXBException e) {
            log.debug("Error when unmarchalling");
            log.error(e);
        } catch (IOException e) {
            log.debug("Error when unmarchalling");
            log.error(e);
        } catch (SAXException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }


}