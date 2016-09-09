package com.keendly.kindlegen.exception;

import lombok.Getter;

public class KindleGenException extends Exception {

  @Getter
  private int exitValue;

  @Getter
  private String output;

  public KindleGenException(int exitValue, String kindleGenOutput){
    this.exitValue = exitValue;
    this.output = kindleGenOutput;
  }

  @Override
  public String getMessage(){
    StringBuilder sb = new StringBuilder();
    String[] lines = this.output.split("\n");
    for (String line : lines){
      if (line.startsWith("Error")){
        sb.append(line);
        sb.append("\n");
      }
    }
    return sb.toString().trim();
  }
}
