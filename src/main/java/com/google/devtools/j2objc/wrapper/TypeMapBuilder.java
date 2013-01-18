package com.google.devtools.j2objc.wrapper;

import java.util.Map;


import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import com.google.common.collect.Maps;
import com.google.devtools.j2objc.types.IOSTypeBinding;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.j2objc.annotations.Register;

public class TypeMapBuilder extends ErrorReportingASTVisitor {

  private final IOSTypeBinding NSObject = new IOSTypeBinding("NSObject",
      false);

  private final Map<ITypeBinding, IOSTypeBinding> bindingMap = Maps
      .newHashMap();

  public static Map<ITypeBinding, IOSTypeBinding> buildMap(
      final CompilationUnit unit) {
    final TypeMapBuilder builder = new TypeMapBuilder();
    builder.run(unit);
    return builder.bindingMap;
  }

  private void put(ASTNode node, ITypeBinding binding) {
    if (binding == null) {
      return;
    }
    for (IAnnotationBinding ab : binding.getAnnotations()) {
      if (ab.getAnnotationType().getQualifiedName()
          .equals(Register.class.getName())) {
        if (Types.isInterface(binding)) {
          bindingMap.put(binding, new IOSTypeBinding(binding.getName(),
            true));
        } else {
          bindingMap.put(binding, new IOSTypeBinding(binding.getName(),
              NSObject));  // FIXME
        }
      }
    }
  }

  @Override
  public boolean visit(SimpleName node) {
    put(node, getTypeBinding(node.resolveBinding()));
    return true;
  }

  private static ITypeBinding getTypeBinding(IBinding binding) {
    if (binding instanceof ITypeBinding) {
      return (ITypeBinding) binding;
    } else if (binding instanceof IMethodBinding) {
      IMethodBinding m = (IMethodBinding) binding;
      return m.isConstructor() ? m.getDeclaringClass() : m
          .getReturnType();
    } else if (binding instanceof IVariableBinding) {
      return ((IVariableBinding) binding).getType();
    }
    return null;
  }
}
