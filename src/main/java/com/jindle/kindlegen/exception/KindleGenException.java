package com.jindle.kindlegen.exception;

import lombok.Getter;

public class KindleGenException extends Exception {

  @Getter
  private int exiteValue;

  @Getter
  private String output;

  public KindleGenException(int exiteValue, String kindleGenOutput){
    this.exiteValue = exiteValue;
    this.output = kindleGenOutput;
  }

}
