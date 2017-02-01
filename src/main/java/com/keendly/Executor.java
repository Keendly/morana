package com.keendly;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public class Executor {

    private static final long TIMEOUT = 60 * 5000; // 5 min;

    public String compress(String directory)
        throws IOException, TimeoutException, InterruptedException, ExecutorException {
        String resultFilePath = "/tmp/" + UUID.randomUUID().toString().replace("-", "") + ".tar.gz";
        String command = "tar zcf " + resultFilePath + " -C " + "/tmp" + File.separator + directory + " .";
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        Worker worker = new Worker(process);

        try {
            StreamConsumer streamConsumer = new StreamConsumer(process.getInputStream());
            streamConsumer.start();
            worker.start();
            worker.join(TIMEOUT);
            if (worker.exitValue != null) {
                if (new File(resultFilePath).exists()) {
                    return resultFilePath;
                } else {
                    throw new ExecutorException(worker.exitValue, streamConsumer.getOutput());
                }
            } else {
                throw new TimeoutException("timeout after " + TIMEOUT + "ms");
            }
        } catch (InterruptedException e) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            process.destroy();
        }
    }

    private static class Worker extends Thread {
        private final Process process;
        private Integer exitValue;

        private Worker(Process process) {
            this.process = process;
        }

        public void run() {
            try {
                process.waitFor();
                exitValue = process.exitValue();
            } catch (InterruptedException ignore) {
                return;
            }
        }
    }

    private static class StreamConsumer extends Thread {
        private InputStream inputStream;
        private StringBuilder output;

        StreamConsumer(final InputStream inputStream) {
            this.inputStream = inputStream;
            this.output = new StringBuilder();
        }

        public String getOutput() {
            return output.toString().trim();
        }

        @Override
        public void run() {
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    output.append(line + "\n");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class ExecutorException extends Exception {

        @Getter
        private int exitValue;

        @Getter
        private String output;

        public ExecutorException(int exitValue, String output) {
            this.exitValue = exitValue;
            this.output = output;
        }
    }

    private class TimeoutException extends Exception {

        public TimeoutException(String msg) {
            super(msg);
        }
    }
}
