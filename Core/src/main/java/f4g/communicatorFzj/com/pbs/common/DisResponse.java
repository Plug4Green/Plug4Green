/**
* ============================== Header ============================== 
* file:          DisResponse.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Parsing of DIS encoded responses
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

/**
 * 
 * Parent class for classes, which parses DIS encoded responses.
 * More about DIS is documented at the DisRequest documentation
 * 
 * @see DisRequest
 * @author Daniel Brinkers
 */
public class DisResponse {
	
	static Logger log = Logger.getLogger(DisResponse.class.getName());
	
	private int returnValue_;
	private int aux_;
	private Response type_;
	
	/**
	 * Data structure holding the common Header of a DIS response  
	 *
	 * @author Daniel Brinkers
	 */
	static class Header{
		int returnValue;
		int aux;
		Response type;
	}
	
	/**
	 * Type of the response
	 *
	 * @author Daniel Brinkers
	 */
	static enum Response{
		NULL,
		QUEUE,
		RDYTOCOM,
		COMMIT,
		SELECT,
		STATUS,
		TEXT,
		LOCATE,
		RESCQUERY,
	}
	/**
	 * Constructor, which sets the common data fields (used my child classes) 
	 * 
	 * @author Daniel Brinkers
	 */
	DisResponse(Header header){
		setReturnValue(header.returnValue);
		setAux(header.aux);
		setType(header.type);
	}
	
	/**
	 * Reads in a DIS encoded string value from a InputStream 
	 * 
	 * @param inputStream the stream to read from
	 * @return the read string
	 *
	 * @author Daniel Brinkers
	 * @throws IOException 
	 */
	static String readString(InputStream inputStream) throws IOException {;
		int s = readInt(inputStream);
		String res = "";
		for(int i=0; i<s; ++i){
			int t;
			t = inputStream.read();
			if(t == -1)
				throw new IOException("EOF");
			res += (char)t;
		}
		//log.debug("Got string from DisResponse: " + res);
		return res;
	}

	/**
	 * Reads a DIS encoded integer from an InputStream
	 * 	 
	 * @param inputStream the stream to read from
	 * @return the read integer
	 *
	 * @author Daniel Brinkers
	 * @throws IOException 
	 */
	static int readInt(InputStream inputStream) throws IOException {
		//iteger to save a char or -1
		int t;
		char c;
		int sign;
		int number;
		int nDigits = 1;
		t = inputStream.read();
		if(t == -1)
			throw new IOException("EOF");
		c = (char)t;
		/*
		 * One digit integer, if the encoded string starts with a sign
		 * Otherwise the string starts with the number of digits
		 */
		if(c != '-' && c != '+'){
			nDigits = c - '0';
			t = inputStream.read();
			if(t == -1)
				throw new IOException("EOF");
			c = (char)t;
			/*
			 * The number of digits is >= 10 no sign will follow and the number of digits must be encoded first
			 */
			if(c != '-' && c != '+'){
				int nDigitsOfnDigits = nDigits;
				nDigits = c - '0';
				for(int i=1; i<nDigitsOfnDigits; ++i){
					t = inputStream.read();
					if(t == -1)
						throw new IOException("EOF");
					c = (char)t;
					assert(c >= '0');
					assert(c <= '9');
					nDigits = 10 * nDigits + c - '0';
				}
				t = inputStream.read();
				if(t == -1)
					throw new IOException("EOF");
				c = (char)t;
			}
		}
		/*
		 * the previous action reads the number of digits and stopped after reading the sign into c
		 */
		sign = c == '-' ? -1 : 1;
		number = 0;
		/*
		 * read the digits of the number
		 */
		for(int i=0; i<nDigits; ++i){
			t = inputStream.read();
			if(t == -1)
				throw new IOException("EOF");
			c = (char)t;
			assert(c >= '0');
			assert(c <= '9');
			number = 10 * number + c - '0';
		}
		return sign * number;
	}

	/**
	 * Parses a InputStream for a DIS encoded response
	 * The return value is of a type of one of the child classes of this class
	 * 
	 * @param inputStream the stream to read from
	 * @return The parsed DIS message
	 *
	 * @author Daniel Brinkers
	 * @throws IOException 
	 */
	public static DisResponse read(InputStream inputStream) throws IOException {
		Header header = readHeader(inputStream);
		DisResponse disResponse = null;
		switch(header.type){
		case NULL:
			disResponse = new DisResponseNull(inputStream, header);
			break;
		case QUEUE:
			disResponse = new DisResponseQueue(inputStream, header);
			break;
		case RDYTOCOM:
			disResponse = new DisResponseRdyToCom(inputStream, header);
			break;
		case COMMIT:
			disResponse = new DisResponseCommit(inputStream, header);
			break;
		case STATUS:
			disResponse = new DisResponseStatus(inputStream, header);
			break;
		case TEXT:
			disResponse = new DisResponseText(inputStream, header);
			break;
		}
		//log.debug("Read from DisResponse: " + disResponse.toString());
		return disResponse;

	}

	/**
	 * Parses the common header of DIS responses
	 * 
	 * @param inputStream the stream to read from
	 * @return the Header of the response (the type is encoded there)
	 *
	 * @author Daniel Brinkers
	 * @throws IOException 
	 */
	private static Header readHeader(InputStream inputStream) throws IOException {
		Header header = new Header();
		int type, version;
		type = readInt(inputStream);
		version = readInt(inputStream);
		assert(type == 2);
		assert(version == 1);
		header.returnValue = readInt(inputStream);
		header.aux = readInt(inputStream);
		switch(readInt(inputStream)){
		case 1:
			header.type = Response.NULL;
			break;
		case 2:
			header.type = Response.QUEUE;
			break;
		case 3:
			header.type = Response.RDYTOCOM;
			break;
		case 4:
			header.type = Response.COMMIT;
			break;
		case 5:
			header.type = Response.SELECT;
			break;
		case 6:
			header.type = Response.STATUS;
			break;
		case 7:
			header.type = Response.TEXT;
			break;
		case 8:
			header.type = Response.LOCATE;
			break;
		case 9:
			header.type = Response.RESCQUERY;
			break;
		}
		return header;
	}

	/**
	 * @param returnValue_ the returnValue_ to set
	 */
	public void setReturnValue(int returnValue_) {
		this.returnValue_ = returnValue_;
	}

	/**
	 * @return the returnValue_
	 */
	public int getReturnValue() {
		return returnValue_;
	}

	/**
	 * @param aux_ the aux_ to set
	 */
	public void setAux(int aux_) {
		this.aux_ = aux_;
	}

	/**
	 * @return the aux_
	 */
	public int getAux() {
		return aux_;
	}

	/**
	 * @param type_ the type_ to set
	 */
	public void setType(Response type_) {
		this.type_ = type_;
	}

	/**
	 * @return the type_
	 */
	public Response getType() {
		return type_;
	}
}
