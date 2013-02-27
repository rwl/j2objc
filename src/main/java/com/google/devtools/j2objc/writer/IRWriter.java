package com.google.devtools.j2objc.writer;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class IRWriter implements Closeable {

  private static final Logger LOGGER = Logger.getLogger(IRWriter.class.getName());

  private final Writer out;

  private int _constantCounter = 0;
  private int _globalCounter = 0;
  private int _globalNameCounter = 0;

  private int _localConstantCounter = 0;

  private final Set<String> globalNames = Sets.newHashSet();

  public IRWriter(Writer out) {
    this.out = out;
  }

  public IRWriter emitHeader(String moduleId) {
    assert moduleId != null;
    write("; ModuleID = '%s'\n", moduleId);
    return this;
  }

  public IRWriter emitDataLayout(List<String> specifications) {
    assert specifications != null;
    assert specifications.size() > 0;
    write("target datalayout = \"");
    for (int i = 0; i < specifications.size(); i++) {
      write(specifications.get(i));
      if (i != specifications.size() - 1) {
        write("-");
      }
    }
    write("\"\n");
    return this;
  }

  public IRWriter emitTriple(ArchType arch, VendorType vendor, OSType os) {
    return emitTriple(arch, vendor, os, null);
  }

  public IRWriter emitTriple(ArchType arch, VendorType vendor, OSType os,
      EnvironmentType env) {
    assert arch != null;
    assert vendor != null;
    assert os != null;
    write("target triple = \"%s-%s-%s", arch.arch(), vendor.vendor(), os.os());
    if (env != null) {
      write("-");
      write(env.env());
    }
    write("\"\n");
    return this;
  }

  public IRWriter emitNamedType(String name, IType type) {
    assert type != null;
    assert !"void".equals(name);
    if (name == null || name.isEmpty()) {
      name = getGlobalNameCounter();
    }
    write("%%%s = type %s\n", name, type.ir());
    return this;
  }

  public IRWriter emitConstant(String name, String constant) {
    assert constant != null;
    if (name == null || name.isEmpty()) {
      name = getConstantCounter();
    }
    write("@%s = private unnamed_addr constant [%d x i8] c\"%s\\00\"\n",
        name, constant.length() + 1, constant);
    return this;
  }

  public IRWriter emitGlobal(String name, IType type, IValue init) {
    assert type != null;
    if (name == null || name.isEmpty()) {
      name = getGlobalCounter();
    }
    write("@%s = internal global %s %s\n", name, type.ir(),
        init != null ? init.ir() : "zeroinitializer");
    return this;
  }

  public IRWriter beginFunction(IType retType, String name, List<IType> argTypes,
      List<String> argNames, List<AttrKind> attrs, boolean isVarArgs) {
    if (retType == null) {
      retType = VoidType.INSTANCE;
    }
    assert name != null;
    if (argTypes == null) {
      argTypes = Lists.newArrayList();
    }
    if (argNames != null) {
      assert argTypes != null;
      assert argTypes.size() == argNames.size();
    } else {
      argNames = Lists.newArrayList();
      for (Iterator<IType> it = argTypes.iterator(); it.hasNext();) {
        argNames.add(getLocalConstantCounter());
      }
    }
    write("define %s @%s(", retType.ir(), name);
    for (int i = 0; i < argTypes.size(); i++) {
      IType argType = argTypes.get(i);
      String argName = argNames.get(i);
      write("%s %%%s", argType, argName);
    }
    write(")");
    for (AttrKind attrKind : attrs) {
      write(" %s", attrKind.kind());
    }
    write(" {\n");
    return this;
  }

  public IRWriter emitFunctionDecl(IType retType, String name, List<IType> argTypes,
      List<AttrKind> attrs, boolean isVarArgs) {
    return this;
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  private void write(String s, Object... args) {
    try {
      out.write(String.format(s, args));
    } catch (IOException e) {
      LOGGER.severe("Error writing: " + e.getMessage());
    }
  }

  private String getConstantCounter() {
    int cnt = _constantCounter;
    _constantCounter += 1;
    return String.valueOf(cnt);
  }

  private String getGlobalNameCounter() {
    int cnt = _globalNameCounter;
    _globalNameCounter += 1;
    return String.valueOf(cnt);
  }

  private String getGlobalCounter() {
    int cnt = _globalCounter;
    _globalCounter += 1;
    return String.valueOf(cnt);
  }

  private String getLocalConstantCounter() {
    int cnt = _localConstantCounter;
    _localConstantCounter += 1;
    return String.valueOf(cnt);
  }

}
