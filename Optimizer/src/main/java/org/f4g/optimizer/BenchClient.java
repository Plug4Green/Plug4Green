package org.f4g.optimizer;

import entropy.jobsManager.Job;
import entropy.jobsManager.JobHandler;
import org.f4g.optimizer.CloudTraditional.SLAReader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

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
        while (j != null) {
            File model = storeResource(client, j.get(BenchServer.MODEL_KEY), root);
            if (sla == null) {
                File f = storeResource(client, j.get(BenchServer.SLA_KEY), root);
                sla = new SLAReader(f);
            }

            BenchmarkStatistics st = Benchmark.runConfiguration(sla, model.getPath());

            j.put(BenchServer.RESULT_KEY, st.toRaw());
            client.commit(j);
            j = client.dequeue();
        }

    }
    public static void main(String [] args) {
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