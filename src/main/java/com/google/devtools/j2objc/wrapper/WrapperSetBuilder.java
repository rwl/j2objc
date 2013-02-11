package com.google.devtools.j2objc.wrapper;

import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.collect.Sets;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

public class WrapperSetBuilder extends ErrorReportingASTVisitor {

  private final Set<ITypeBinding> bindingSet = Sets.newHashSet();

  private final Set<ITypeBinding> mappedBindings = Sets.newHashSet();

  public static Set<ITypeBinding> buildSet(final CompilationUnit unit) {
    final WrapperSetBuilder builder = new WrapperSetBuilder();
    builder.run(unit);
    return builder.bindingSet;
  }

  private void add(ITypeBinding typeBinding) {
    if (typeBinding == null ||
        mappedBindings.contains(typeBinding) ||
        typeBinding.getQualifiedName().equals(Object.class.getName())) {
      return;
    } else {
      mappedBindings.add(typeBinding);
    }
    if (Types.isWrapper(typeBinding)) {
      bindingSet.add(typeBinding);
    }
    add(typeBinding.getSuperclass());
    for (ITypeBinding iface : typeBinding.getInterfaces()) {
      add(iface);
    }
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    add(typeBinding);
    return super.visit(node);
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    add(Types.getTypeBinding(node));
    return super.visit(node);
  }

  @Override
  public boolean visit(FieldAccess node) {
    add(Types.getTypeBinding(node.getName().resolveBinding()));
    return super.visit(node);
  }

  @Override
  public boolean visit(FieldDeclaration node) {
    add(node.getType().resolveBinding());
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    IMethodBinding methodBinding = node.resolveMethodBinding();
    if (methodBinding != null) {
      add(methodBinding.getDeclaringClass());
      add(methodBinding.getReturnType());
    }
    return super.visit(node);
  }

}
