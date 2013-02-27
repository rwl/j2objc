package com.google.devtools.j2objc.writer;

public class OpaqueType implements IType {

  public static final OpaqueType INSTANCE = new OpaqueType();

  private OpaqueType() {
  }

  @Override
  public String ir() {
    return "opaque";
  }

}
