package com.google.devtools.j2objc.wrapper;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.devtools.j2objc.types.IOSTypeBinding;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.j2objc.annotations.BaseType;
import com.google.j2objc.annotations.Register;

public class TypeMapBuilder extends ErrorReportingASTVisitor {

  private static final IOSTypeBinding NSObject = new IOSTypeBinding("NSObject",
      false);

  private final Map<ITypeBinding, IOSTypeBinding> bindingMap = Maps
      .newHashMap();

  private final Set<ITypeBinding> mappedBindings = Sets.newHashSet();

  public static Map<ITypeBinding, IOSTypeBinding> buildMap(
      final CompilationUnit unit) {
    final TypeMapBuilder builder = new TypeMapBuilder();
    builder.run(unit);
    return builder.bindingMap;
  }

  private void put(ITypeBinding typeBinding) {
    if (typeBinding == null ||
        mappedBindings.contains(typeBinding) ||
        typeBinding.getQualifiedName().equals(Object.class.getName())) {
      return;
    } else {
      mappedBindings.add(typeBinding);
    }
    for (IAnnotationBinding ab : typeBinding.getAnnotations()) {
      if (ab.getAnnotationType().getQualifiedName()
          .equals(Register.class.getName())) {
        if (Types.isInterface(typeBinding)) {
          bindingMap.put(typeBinding, new IOSTypeBinding(typeBinding.getName(),
              true));
        } else if (typeBinding.isEnum()) {
          bindingMap.put(typeBinding, new IOSTypeBinding(typeBinding.getName(),
              false));
        } else {
          bindingMap.put(typeBinding, new IOSTypeBinding(typeBinding.getName(),
              getSuperClass(typeBinding)));
        }
      }
    }
    put(typeBinding.getSuperclass());
    for (ITypeBinding iface : typeBinding.getInterfaces()) {
      put(iface);
    }
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    final ITypeBinding typeBinding = node.resolveBinding();
    put(typeBinding);
    return super.visit(node);
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    put(node.resolveTypeBinding());
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
  public boolean visit(FieldDeclaration node) {
    put(node.getType().resolveBinding());
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
    IMethodBinding binding = node.resolveMethodBinding();
    if (binding != null) {
      put(binding.getDeclaringClass());
      put(binding.getReturnType());
    }
    return super.visit(node);
  }

  private static ITypeBinding getSuperClass(ITypeBinding typeBinding) {
    for (IAnnotationBinding annotation : typeBinding.getAnnotations()) {
      if (annotation.getAnnotationType().getQualifiedName().equals(BaseType.class.getName())) {
        for (IMemberValuePairBinding pair : annotation.getDeclaredMemberValuePairs()) {
          if (pair.getKey().equals("value")) {
            Class<?> baseType = (Class<?>) pair.getValue();
            return new IOSTypeBinding(baseType.getSimpleName(), false);
          }
        }
      }
    }
    return NSObject;
  }

}
