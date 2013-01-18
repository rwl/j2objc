package com.google.devtools.j2objc.wrapper;

import java.util.Map;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.collect.Maps;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

public class HeaderMapBuilder extends ErrorReportingASTVisitor {

  private static final Map<String, String> prefixMap = Maps.newHashMap();

  static {
    prefixMap.put("NS", "Foundation/Foundation");
    prefixMap.put("UI", "UIKit/UIKit");
    prefixMap.put("CG", "CoreGraphics/CoreGraphics");
  }

  private final Map<String, String> bindingMap = Maps.newHashMap();

  public static Map<String, String> buildMap(final CompilationUnit unit) {
    final HeaderMapBuilder builder = new HeaderMapBuilder();
    builder.run(unit);
    return builder.bindingMap;
  }

  private void put(String name, String header) {
    bindingMap.put(name, header);
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    putIfWrapper(typeBinding);
    putIfWrapper(typeBinding.getSuperclass());
    return super.visit(node);
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    putIfWrapper(node.resolveTypeBinding());
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    for (ITypeBinding param : node.resolveBinding().getParameterTypes()) {
      putIfWrapper(param);
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(QualifiedName node) {
    putIfWrapper(node.resolveTypeBinding());
    return super.visit(node);
  }

  @Override
  public boolean visit(FieldAccess node) {
    putIfWrapper(getTypeBinding(node.getName().resolveBinding()));
    return super.visit(node);
  }

  private void putIfWrapper(ITypeBinding typeBinding) {
    if (typeBinding == null) {
      return;
    }
    if (Types.isWrapper(typeBinding)) {
      String name = typeBinding.getName();
      String header = prefixMap.get(name.subSequence(0, 2));
      if (header != null) {
        put(name, header);
      }
    }
  }

  public static ITypeBinding getTypeBinding(IBinding binding) {
    if (binding instanceof ITypeBinding) {
      return (ITypeBinding) binding;
    } else if (binding instanceof IMethodBinding) {
      IMethodBinding m = (IMethodBinding) binding;
      return m.isConstructor() ? m.getDeclaringClass() : m.getReturnType();
    } else if (binding instanceof IVariableBinding) {
      return ((IVariableBinding) binding).getType();
    }
    return null;
  }
}
