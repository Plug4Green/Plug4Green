/**
* ============================== Header ============================== 
* file:          Properties.java
* project:       FIT4Green/Manager
* created:       16 sep. 2011 by vicky@almende.org
* 
* $LastChangedDate: 2011-10-25 17:13:45 +0200 (mar, 25 oct 2011) $ 
* $LastChangedBy: vicky@almende.org $
* $LastChangedRevision: 967 $
* 
* short description:
*   Database properties
* ============================= /Header ==============================
*/
package org.f4g.couchDB;

import java.util.ArrayList;

/**
 * Database properties
 *
 * @author Vasiliki Georgiadou
 */
public class Properties {
	
	private ArrayList<Element> elem = new ArrayList<Element>();
	
	// TODO (phase 3) Extract more properties as necessary
	
	private int diskSize;
	private int docCount;
	private int docDelCount;    // in Bytes
	
	
	public ArrayList<Element> getElem() {
		return elem;
	}

	public int getDiskSize() {
		return diskSize;
	}

	public int getDocCount() {
		return docCount;
	}

	public int getDocDelCount() {
		return docDelCount;
	}

	
	public void retrieveProperties(String input) {
    	
    	String[] tokens = input.split("[{}:\",]+");
    	
    	for (int i=1; i<tokens.length-1; i=i+2) {    		// first and last tokens are empty
    		Element e = new Element(tokens[i],tokens[i+1]);
    		elem.add(e);
    	}
    	
    	for (int i=0; i<elem.size(); i++) {	
    		if (elem.get(i).getKey().compareTo("disk_size") == 0) {
    			diskSize = Integer.parseInt(elem.get(i).getValue());
    		} else if (elem.get(i).getKey().compareTo("doc_count") == 0) {
    			docCount = Integer.parseInt(elem.get(i).getValue());
    		} else if (elem.get(i).getKey().compareTo("doc_del_count") == 0) {
    			docDelCount = Integer.parseInt(elem.get(i).getValue());
    		}
    	}
    	
    }
	
	class Element {
		
		private String key;
		private String value;
		
		public Element(String key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public String getKey() {
			return key;
		}
		
		public String getValue() {
			return value;
		}
			
	}

}
