package f4g.optimizer;

import entropy.jobsManager.CommitedJobHandler;
import entropy.jobsManager.Job;
import entropy.jobsManager.JobDispatcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A server to dispatch models to solve to clients.
 * @author Fabien Hermenier
 */
public class BenchServer implements CommitedJobHandler {

    private JobDispatcher dispatcher;

    private String output;

    private int timeout;

    public static final String SLA_KEY = "SLA";

    public static final String MODEL_KEY = "model";

    public static final String TIMEOUT_KEY = "timeout";

    public static final String RESULT_KEY = "result";

    public BenchServer(String input, String sla, String output, int timeout, int port) {
        dispatcher = new JobDispatcher(port, System.getProperty("user.dir"), this);
        this.output = output;
        this.timeout = timeout;

        File outF = new File(output);
        File p = outF.getParentFile();
        if (p != null && !p.exists()) {
            if (!p.mkdirs()) {
               JobDispatcher.getLogger().error("Unable to create the folder '" + p.getPath() + "'");
            }
        }
        fillServer(input, sla);
    }


    private void fillServer(String input, String sla) {

        File f = new File(input);
        if (f.isDirectory()) {
            int id = 0;
            for (File i : f.listFiles()) {
                Job j = new Job(id);
                j.put(SLA_KEY, new File(sla).getPath());
                j.put(MODEL_KEY, i.getPath());
                j.put(TIMEOUT_KEY, Integer.toString(timeout));
                dispatcher.enqueue(j);
                id++;
            }
        }
    }

    @Override
    public void jobCommited(Job job) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(output, true), true);
            out.println(job.get(RESULT_KEY));
            out.close();
        } catch (IOException e) {
            JobDispatcher.getLogger().error(e.getMessage(), e);
        }
    }

    public void start() {
        dispatcher.getLogger().info(dispatcher.getWaitings().size() + " jobs enqueued");
        dispatcher.run();
    }

    public static void main(String [] args) {
        if (args.length != 10) {
            usage(0);
        }
        int port = Integer.parseInt(args[9]);
        String input = args[1];
        String sla = args[3];
        int timeout = Integer.parseInt(args[7]);

        String output = args[5];
        BenchServer srv = new BenchServer(input, sla, output, timeout, port);
        srv.start();
    }


    public static void usage(int err) {
        System.out.println("Usage: BenchServer -i <folder> -sla <sla> -o <output> -t <timeout> -p <port>");
        System.out.println("Get the instances in <folder> and the sla in <sla> and listen for clients on port <port>");
        System.out.println("The timeout value to compute a solution is limited to <timeout> seconds");
        System.out.println("Results will be stored in file <output>");
        System.exit(err);
    }
}
