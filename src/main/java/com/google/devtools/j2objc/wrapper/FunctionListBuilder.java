package com.google.devtools.j2objc.wrapper;

import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.google.common.collect.Lists;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.j2objc.annotations.Function;

public class FunctionListBuilder extends ErrorReportingASTVisitor {

  private final List<IMethodBinding> functionList = Lists.newArrayList();

  public static List<IMethodBinding> buildList(final CompilationUnit unit) {
    final FunctionListBuilder builder = new FunctionListBuilder();
    builder.run(unit);
    return builder.functionList;
  }

  @Override
  public boolean visit(MethodInvocation node) {
    IMethodBinding methodBinding = node.resolveMethodBinding();
    for (IAnnotationBinding annotation : methodBinding.getAnnotations()) {
      if (annotation.getAnnotationType().getQualifiedName().equals(Function.class.getName())) {
        functionList.add(methodBinding);
      }
    }
    return super.visit(node);
  }

}
