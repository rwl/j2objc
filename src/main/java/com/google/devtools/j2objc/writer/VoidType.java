package com.google.devtools.j2objc.writer;

public class VoidType implements IType {

  public static final VoidType INSTANCE = new VoidType();

  private VoidType() {
  }

  @Override
  public String ir() {
    return "void";
  }

}
