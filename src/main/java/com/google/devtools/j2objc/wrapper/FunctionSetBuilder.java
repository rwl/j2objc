package com.google.devtools.j2objc.wrapper;

import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.google.common.collect.Sets;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.j2objc.annotations.Function;

public class FunctionSetBuilder extends ErrorReportingASTVisitor {

  private final Set<IMethodBinding> functionSet = Sets.newHashSet();

  public static Set<IMethodBinding> buildSet(final CompilationUnit unit) {
    final FunctionSetBuilder builder = new FunctionSetBuilder();
    builder.run(unit);
    return builder.functionSet;
  }

  @Override
  public boolean visit(MethodInvocation node) {
    IMethodBinding methodBinding = node.resolveMethodBinding();
    if (methodBinding != null) {
      for (IAnnotationBinding annotation : methodBinding.getAnnotations()) {
        if (annotation.getAnnotationType().getQualifiedName().equals(Function.class.getName())) {
          functionSet.add(methodBinding);
        }
      }
    }
    return super.visit(node);
  }

}
