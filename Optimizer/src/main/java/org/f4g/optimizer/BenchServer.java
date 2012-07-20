package org.f4g.optimizer;

import entropy.jobsManager.CommitedJobHandler;
import entropy.jobsManager.Job;
import entropy.jobsManager.JobDispatcher;

import java.io.File;

/**
 * A server to dispatch models to solve to clients.
 * @author Fabien Hermenier
 */
public class BenchServer implements CommitedJobHandler {

    private JobDispatcher dispatcher;

    private String root;

    private String input;

    private String output;

    public BenchServer(String input, String output, int port) {
        dispatcher = new JobDispatcher(port, System.getenv("user.dir"), this);
        this.input = input;
        this.output = output;

        fillServer();
    }


    private void fillServer() {
        File f = new File(input);
        if (f.isDirectory()) {
            for (File i : f.listFiles()) {

            }
        }
    }
    @Override
    public void jobCommited(Job job) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public static void main(String [] args) {
        if (args.length != 6) {
            usage(0);
        }
        int port = Integer.parseInt(args[5]);
        String input = args[1];
        String output = args[2];

        BenchServer srv = new BenchServer(input, output, port);
    }


    public static void usage(int err) {
        System.out.println("Usage: BenchServer -i <folder> -o <output> -p <port>");
        System.out.println("Get the instances in <folder> and listen for clients on port <port>");
        System.out.println("Results will be stored in file <output>");
        System.exit(err);
    }
}
