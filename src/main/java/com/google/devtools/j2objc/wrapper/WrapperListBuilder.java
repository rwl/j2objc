package com.google.devtools.j2objc.wrapper;

import java.util.List;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.collect.Lists;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

public class WrapperListBuilder extends ErrorReportingASTVisitor {

  private final List<ITypeBinding> bindingList = Lists.newArrayList();

  public static List<ITypeBinding> buildList(final CompilationUnit unit) {
    final WrapperListBuilder builder = new WrapperListBuilder();
    builder.run(unit);
    return builder.bindingList;
  }

  private void add(final ITypeBinding binding) {
    bindingList.add(binding);
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    addIfWrapper(typeBinding);
    return super.visit(node);
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    addIfWrapper(node.resolveTypeBinding());
    return super.visit(node);
  }

  private void addIfWrapper(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return;
    }
    if (Types.isWrapper(typeBinding)) {
      add(typeBinding);
    }
    if (typeBinding.getName().equals("NSObject")) {
      return;
    }
    addIfWrapper(typeBinding.getSuperclass());
  }
}
