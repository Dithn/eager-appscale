package edu.ucsb.cs.roots.utils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class CommandLineUtils {

    public static CommandOutput runCommand(File dir, String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(dir);
        Process process = pb.start();
        StreamReader outputReader = new StreamReader(process.getInputStream());
        StreamReader errorReader = new StreamReader(process.getErrorStream());
        int status = process.waitFor();
        return new CommandOutput(status, outputReader.getOutput(), errorReader.getOutput());
    }

    private static class StreamReader extends Thread {

        private final InputStream in;
        private String output;

        public StreamReader(InputStream in) {
            this.in = in;
            this.setDaemon(true);
            this.start();
        }

        @Override
        public void run() {
            try {
                output = IOUtils.toString(in);
            } catch (IOException e) {
                output = e.getMessage();
            }
        }

        public String getOutput() throws InterruptedException {
            this.join();
            return output;
        }
    }

}
