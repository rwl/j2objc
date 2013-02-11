package com.google.devtools.j2objc.wrapper;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.j2objc.annotations.Register;

public class HeaderMapBuilder extends ErrorReportingASTVisitor {

  private static final Map<String, String> prefixMap = Maps.newHashMap();

  static {
    prefixMap.put("NS", "Foundation/Foundation.h");
    prefixMap.put("UI", "UIKit/UIKit.h");
    prefixMap.put("CG", "CoreGraphics/CoreGraphics.h");
  }

  private final Map<String, String> bindingMap = Maps.newHashMap();

  private final Set<ITypeBinding> mappedBindings = Sets.newHashSet();

  public static Map<String, String> buildMap(final CompilationUnit unit) {
    final HeaderMapBuilder builder = new HeaderMapBuilder();
    builder.run(unit);
    return builder.bindingMap;
  }

  private void put(ITypeBinding typeBinding) {
    if (typeBinding == null ||
        mappedBindings.contains(typeBinding)) {
      return;
    } else {
      mappedBindings.add(typeBinding);
    }
    if (Types.isWrapper(typeBinding)) {
      String name = typeBinding.getName();
      for (IAnnotationBinding annotation : typeBinding.getAnnotations()) {
        if (annotation.getAnnotationType().getQualifiedName().equals(Register.class.getName())) {
          for (IMemberValuePairBinding pair : annotation.getDeclaredMemberValuePairs()) {
            if (pair.getName().equals("header") && !((String) pair.getValue()).isEmpty()) {
              bindingMap.put(name, (String) pair.getValue());
              return;
            }
          }
        }
      }
      String header = prefixMap.get(name.subSequence(0, 2));
      if (header != null) {
        bindingMap.put(name, header);
      }
    }
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    put(typeBinding);
    put(typeBinding.getSuperclass());
    return super.visit(node);
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    put(node.resolveTypeBinding());
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    for (ITypeBinding param : node.resolveBinding().getParameterTypes()) {
      put(param);
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    IMethodBinding methodBinding = node.resolveMethodBinding();
    if (methodBinding != null) {
      put(methodBinding.getDeclaringClass());
      put(methodBinding.getReturnType());
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(QualifiedName node) {
    put(node.resolveTypeBinding());
    return super.visit(node);
  }

  @Override
  public boolean visit(FieldAccess node) {
    put(Types.getTypeBinding(node.getName().resolveBinding()));
    return super.visit(node);
  }

  @Override
  public boolean visit(QualifiedType node) {
    put(Types.getTypeBinding(node.resolveBinding()));
    return super.visit(node);
  }

  @Override
  public boolean visit(SimpleType node) {
    put(Types.getTypeBinding(node.resolveBinding()));
    return super.visit(node);
  }
}
