package f4g.P4GDemo;

import java.util.HashMap;

import f4g.com.openstack.ComOpenstack;
import f4g.commons.com.ICom;
import f4g.pluginCore.core.Main;

/**
 * Hello world!
 *
 */
public class P4GDemo {
    public static void main(String[] args) {
	
	ComOpenstack comOS = new ComOpenstack();
	HashMap<String, ICom> map = new HashMap<String, ICom>();
	map.put("comOpenstack", comOS);
	Main p4g = new Main(map);
	p4g.init("src/main/config/core/f4gconfig.properties");
	p4g.startup();
    }
}
