package f4g.optimizer;


import f4g.schemas.java.metamodel.FIT4Green;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;


//This class generates default config files
public class ConfigWriter {

	public ConfigWriter() {
	}	


    public static void writeYaml(String fileName, Object o) {

        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Yaml yaml = new Yaml(options);
            String output = yaml.dump(o);
            System.out.println(output);

            writer.print(output);
            writer.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    //Extension to Snake YAML to serialize/deserialize iCals & JScience
    public static class CalRepresenter extends Representer {
        public CalRepresenter() {

        }

    }    

    //for debugging purpose
    public static void writeConfig(FIT4Green f4g) {

        writeYaml("test.yaml", f4g);
    }

}
