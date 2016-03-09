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

}
