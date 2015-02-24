/**
* ============================== Header ============================== 
* file:          DisResponseStatus.java
* project:       FIT4Green/CommunicatorFzj
* created:       Nov 25, 2010 by Daniel Brinkers
* 
* $LastChangedDate: 2011-04-04 18:51:51 +0200 (Mo, 04 Apr 2011) $ 
* $LastChangedBy: f4g.julichde $
* $LastChangedRevision: 634 $
* 
* short description:
*   Parses DIS encoded status response
* ============================= /Header ==============================
*/
package f4g.communicatorFzj.com.pbs.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Parses DIS encoded status response
 * 
 *
 * @author Daniel Brinkers
 */
public class DisResponseStatus extends DisResponse {

	/**
	 * @param inputStream
	 * @param header
	 */
	public Status[] stati_ = null;
	
	/**
	 * A single status description.
	 * The DisResponseStatus is a collection of this class.
	 * @author Daniel Brinkers
	 */
	public class Status{
		int type;
		String name = null;
		Map<String, String> attributes = null;
		Map<String, Map<String, String> > resources = null;
	}

	/**
	 * Constructor of the response
	 * @param inputStream stream to read from
	 * @param header the already read header
	 * @throws IOException 
	 */
	public DisResponseStatus(InputStream inputStream, Header header) throws IOException {
		super(header);
		int nStati;
		nStati = readInt(inputStream);
		setStati(new Status[nStati]);
		for(int i=0; i<nStati; ++i){
			/*
			 * Status consists of a type(integer) a name(string) and some attributes
			 */
			int nAttributes;
			getStati()[i] = new Status();
			getStati()[i].type = readInt(inputStream);
			getStati()[i].name = readString(inputStream);
			nAttributes = readInt(inputStream);

			getStati()[i].attributes = new HashMap<String, String>();
			getStati()[i].resources = new HashMap<String, Map<String, String> >();
			for(int j=0; j<nAttributes; ++j){
				/*
				 * unused integer in message
				 */
				readInt(inputStream);
				/*
				 * A attribute is basically a key(string) value(string) pair
				 */
				String key = readString(inputStream);
				String value;
				if(readInt(inputStream) != 0){
					/*
					 * The value can be another key(string) value(string) pair 
					 */
					String key2 = readString(inputStream);
					value = readString(inputStream);
					if(!getStati()[i].resources.containsKey(key)){
						getStati()[i].resources.put(key, new HashMap<String, String>());
					}
					Map<String, String> map = getStati()[i].resources.get(key);
					map.put(key2, value);
				}else{
					value = readString(inputStream);
					getStati()[i].attributes.put(key, value);
				}
				/*
				 * unused integer in message
				 */
				readInt(inputStream);
			}
		}
	}
	
	/**
	 * Creates a String representation of the response message
	 * @return The String as a map like representation
	 */
	@Override
	public String toString(){
		String res = "";
		for(int i=0; i < getStati().length; ++i){
			res += getStati()[i].name + " (" + getStati()[i].type + "):\n";
			Iterator<Map.Entry<String, String> > iter = getStati()[i].attributes.entrySet().iterator();
			while(iter.hasNext()){
				Map.Entry<String, String> elem = iter.next();
				res += "  " + elem.getKey() + ": " + elem.getValue() + "\n";
			}
			Iterator<Map.Entry<String, Map<String, String> > > iter1 = getStati()[i].resources.entrySet().iterator();
			while(iter1.hasNext()){
				Map.Entry<String, Map<String, String> > elem1 = iter1.next();
				res += "  resources " + elem1.getKey() + ":\n";
				Iterator<Map.Entry<String, String> > iter2 = elem1.getValue().entrySet().iterator();
				while(iter2.hasNext()){
					Map.Entry<String, String> elem2 = iter2.next();
					res += "    " + elem2.getKey() + ": " + elem2.getValue() + "\n";
				}
			}
		}
		return res;
	}


	/**
	 * @param stati_ the stati_ to set
	 */
	public void setStati(Status[] status) {
		this.stati_ = status;
	}

	/**
	 * @return the stati_
	 */
	public Status[] getStati() {
		return stati_;
	}

}
