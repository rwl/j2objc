package com.google.devtools.j2objc.wrapper;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

public class WrapperListBuilder extends ErrorReportingASTVisitor {

  private final List<ITypeBinding> bindingList = Lists.newArrayList();

  private final Set<ITypeBinding> mappedBindings = Sets.newHashSet();

  public static List<ITypeBinding> buildList(final CompilationUnit unit) {
    final WrapperListBuilder builder = new WrapperListBuilder();
    builder.run(unit);
    return builder.bindingList;
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
      bindingList.add(typeBinding);
    }
    add(typeBinding.getSuperclass());
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    add(typeBinding);
    return super.visit(node);
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    add(node.resolveTypeBinding());
    return super.visit(node);
  }

  @Override
  public boolean visit(FieldAccess node) {
    add(Types.getTypeBinding(node.getName().resolveBinding()));
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    add(node.resolveMethodBinding().getDeclaringClass());
    return super.visit(node);
  }
}
