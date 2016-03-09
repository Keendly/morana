package com.keendly.kindlegen;

import com.keendly.kindlegen.exception.KindleGenException;
import com.keendly.kindlegen.exception.TimeoutException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Executor {

  private static final long TIMEOUT = 60 * 10000; //1 min;
  private static final String RESULT_FILE_NAME = "keendly.mobi";

  private String kindleGenPath;

  private String workingDirectory;
  private String opfFileName;
  private String resultFilePath;

  public Executor(String kindleGenPath, String workingDirectory, String opfFileName){
    this.kindleGenPath = kindleGenPath;
    this.workingDirectory = workingDirectory;
    this.opfFileName = opfFileName;
    this.resultFilePath = workingDirectory + File.separator + RESULT_FILE_NAME;
  }

  public String run() throws IOException, TimeoutException, InterruptedException, KindleGenException {
    String command = kindleGenPath + " -c2 -o " + RESULT_FILE_NAME + " " + opfFileName;
    ProcessBuilder pb = new ProcessBuilder(command.split(" "));
    pb.directory(new File(workingDirectory));
    pb.redirectErrorStream(true);
    Process process =  pb.start();

    Worker worker = new Worker(process);

    try {
      StreamConsumer streamConsumer = new StreamConsumer(process.getInputStream());
      streamConsumer.start();
      worker.start();
      worker.join(TIMEOUT);
      if (worker.exitValue != null){
        if (new File(resultFilePath).exists()){
          return resultFilePath;
        } else {
          throw new KindleGenException(worker.exitValue, streamConsumer.getOutput());
        }
      } else {
        throw new TimeoutException("timeout after " + TIMEOUT + "ms");
      }
    } catch (InterruptedException e){
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

    StreamConsumer(final InputStream inputStream)
    {
      this.inputStream = inputStream;
      this.output = new StringBuilder();
    }

    public String getOutput(){
      return output.toString().trim();
    }

    @Override
    public void run(){
      try
      {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;
        while ((line = bufferedReader.readLine()) != null)
        {
          output.append(line + "\n");
        }
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }
    }
  }
}
