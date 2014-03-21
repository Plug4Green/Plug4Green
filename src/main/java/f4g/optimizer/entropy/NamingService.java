
package f4g.optimizer.entropy;

import btrplace.model.Element;
import btrplace.model.VM;
import btrplace.model.view.ModelView;

import java.util.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 *
 */
public class NamingService<T extends Element> implements ModelView {

    public static final String VIEW_ID_BASE = "NamingService.";
    
    private BiMap<T, String> names;

    private String viewId;

    private String rcId;
    
    public NamingService(String r) {
        this(r, new HashMap<T, String>());
    }

    public NamingService(String id, Map<T, String> names) {
    	
        this.rcId = id;
        this.viewId = VIEW_ID_BASE + rcId;
        this.names = HashBiMap.create(names);
    }
   
    public String getName(T n) {
        return names.get(n);
    }
    
    public T getElement(String name) {
    	return names.inverse().get(name);
    }

    public Set<T> getNames() {
        return names.keySet();
    }

    public List<String> getNames(List<T> ids) {
        List<String> res = new ArrayList<>(ids.size());
        for (T n : ids) {
            res.add(getName(n));
        }
        return res;
    }
        
    public void putElementName(T n, String s) {
    	names.put(n, s);
    }
    
    @Override
    public String getIdentifier() {
        return viewId;
    }

	//TODO should be deleted from parent?
    @Override
	public boolean substituteVM(VM curId, VM nextId) {
		// TODO Auto-generated method stub
		return false;
	}
   
    public String getResourceIdentifier() {
        return rcId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NamingService<T> that = (NamingService<T>) o;

        if (!that.getNames().equals(names.keySet())){
            return false;
        }

        for (T k : names.keySet()) {
            if (!names.get(k).equals(that.getName(k))) {
                return false;
            }
        }
        
        return rcId.equals(that.getResourceIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(rcId, names);
    }
    
    @Override
    public NamingService<T> clone() {
    	NamingService<T> rc = new NamingService<T>(rcId);
        for (Map.Entry<T, String> e : names.entrySet()) {
            rc.names.put(e.getKey(), e.getValue());
        }
        
        return rc;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("rc:").append(rcId).append(':');
        for (Iterator<Map.Entry<T, String>> ite = names.entrySet().iterator(); ite.hasNext(); ) {
            Map.Entry<T, String> e = ite.next();
            buf.append("<element ").append(e.getKey().toString()).append(',').append(e.getValue()).append('>');
            if (ite.hasNext()) {
                buf.append(',');
            }
        }
        
        return buf.toString();
    }

}
