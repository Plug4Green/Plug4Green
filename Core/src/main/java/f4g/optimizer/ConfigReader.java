package f4g.optimizer;

import com.google.common.base.Optional;
import f4g.schemas.java.metamodel.FIT4Green;
import org.yaml.snakeyaml.Yaml;
import java.io.*;

//This class generates default config files
public class ConfigReader {

        public static Optional<FIT4Green> readAppConfig(String configPath) {
            return readConfigFile(configPath);
        }

        public static <T> Optional<T> readConfigFile(String path) {
            try {
                InputStream input = new FileInputStream(new File(path));
                Yaml yaml = new Yaml();
                T config = (T) yaml.load(input);
                return Optional.of(config);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return Optional.absent();
            }

        }

}
