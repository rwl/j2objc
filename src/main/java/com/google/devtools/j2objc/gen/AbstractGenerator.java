package com.google.devtools.j2objc.gen;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import com.google.devtools.j2objc.types.IOSMethod;
import com.google.devtools.j2objc.types.IOSMethodBinding;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.devtools.j2objc.util.NameTable;

public abstract class AbstractGenerator extends ErrorReportingASTVisitor {

  protected String getSimpleTypeName(ITypeBinding binding) {
    if (binding == null) {
      // Parse error already reported.
      return "<unknown>";
    }
    if (binding.isPrimitive()) {
      return Types.getPrimitiveTypeName(binding);
    }
    return Types.mapSimpleTypeName(NameTable.javaTypeToObjC(binding, true));
  }

  protected IOSMethod getIOSMethod(IMethodBinding method) {
    if (method instanceof IOSMethodBinding) {
      IMethodBinding delegate = ((IOSMethodBinding) method).getDelegate();
      return Types.getMappedMethod(delegate);
    }
    return Types.getMappedMethod(method);
  }

  protected boolean hasVarArgsTarget(IMethodBinding method) {
    return method instanceof IOSMethodBinding && ((IOSMethodBinding) method).hasVarArgsTarget();
  }

}
