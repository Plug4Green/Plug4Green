package f4g.commons.optimizer;

import entropy.jobsManager.Job;
import entropy.jobsManager.JobHandler;
import f4g.commons.optimizer.CloudTraditional.SLAReader;
import f4g.commons.optimizer.utils.Utils;
import f4g.optimizer.ModelGenerator;

import java.io.File;
import java.io.FileOutputStream;

/**
 *  A client for BenchServer.
 *
 *  @author Fabien Hermenier
 */
public class BenchClient {



    public static File storeResource(JobHandler h, String rc, File root) throws Exception {
//        String name = rc.substring(rc.lastIndexOf('/') + 1, rc.length());
        File f = new File(root + File.separator + rc);
        f.getParentFile().mkdirs();
        byte[] content = h.getResource(rc);
        FileOutputStream out = new FileOutputStream(f);
        out.write(content);
        out.close();
        return f;
    }

    public static void bench(String hostname, int port) throws Exception{
        File root = new File("/tmp/" + System.currentTimeMillis());



        JobHandler client = new JobHandler(hostname, port, 1);
        Job j = client.dequeue();

        SLAReader sla = null;
        ModelGenerator modelGenerator = new ModelGenerator();
        modelGenerator.schema_location = "config/MetaModel.xsd";
        while (j != null) {
            File model = storeResource(client, j.get(BenchServer.MODEL_KEY), root);
            if (sla == null) {
                File f = storeResource(client, j.get(BenchServer.SLA_KEY), root);
                sla = new SLAReader(f);
            }
            int timeout = Integer.parseInt(j.get(BenchServer.TIMEOUT_KEY));
            BenchmarkStatistics st = Benchmark.runConfiguration(modelGenerator, sla, model.getPath(), timeout);

            j.put(BenchServer.RESULT_KEY, st.toRaw());
            client.commit(j);
            j = client.dequeue();
            System.gc();
            System.gc();
        }

    }
    public static void main(String [] args) {
        Utils.initLogger("config/log4j-benchmark.properties");
        if (args.length == 0) {
            usage(1);
        }


        int port = Integer.parseInt(args[0].substring(args[0].indexOf(':') + 1, args[0].length()));
        String hostname = args[0].substring(0, args[0].indexOf(':'));
        try {
            bench(hostname, port);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


    }

    private static final void usage(int code) {
        System.out.println("BenchClient <server:port>");
        System.exit(code);
    }
}
