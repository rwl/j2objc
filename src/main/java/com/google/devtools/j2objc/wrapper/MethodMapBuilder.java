package com.google.devtools.j2objc.wrapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;

public class MethodMapBuilder extends ErrorReportingASTVisitor {

  private final Map<String, String> bindingMap = Maps
      .newHashMap();

  private final Set<ITypeBinding> mappedBindings = Sets.newHashSet();

  public static Map<String, String> buildMap(final CompilationUnit unit) {
    final MethodMapBuilder builder = new MethodMapBuilder();
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
    if (Types.isWrapper(typeBinding)) {
      for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
        String signature = getSignature(methodBinding);
        String iosSignature = getIOSSignature(methodBinding);
        bindingMap.put(signature, iosSignature);
      }
    }
    put(typeBinding.getSuperclass());
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

  private static String getSignature(final IMethodBinding methodBinding) {
    final String clazz = methodBinding.getDeclaringClass().getQualifiedName();
    final String name = methodBinding.getName();
    final String signature = Types.getSignature(methodBinding);
    return clazz + '.' + name + signature;
  }

  private static String getIOSSignature(final IMethodBinding methodBinding) {
    for (IAnnotationBinding anno : methodBinding.getAnnotations()) {
      for (IMemberValuePairBinding pair : anno.getDeclaredMemberValuePairs()) {
        if (pair.getName().equals("selector")) {
          return parameterizeSelector((String) pair.getValue(), methodBinding);
        }
      }
    }
    final String selector = methodBinding.getName()
        + Strings.repeat(": ", methodBinding.getParameterTypes().length).trim();
    return parameterizeSelector(selector, methodBinding);
  }

  private static String parameterizeSelector(String selector,
      final IMethodBinding methodBinding) {
    final ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
    final List<String> segments = Lists.newArrayList(selector.split(":"));
    for (int i = 0; i < parameterTypes.length; i++) {
      String paramType = parameterTypes[i].getName();
      String param = String.format(":(%s *)%s", paramType,
          paramType.substring(0, 1).toLowerCase() + paramType.substring(1) + "_");
      if (i != parameterTypes.length - 1) {
        param += ' ';
      }
      segments.add((2 * i) + 1, param);
    }
    final String clazz = methodBinding.getDeclaringClass().getName();
    final StringBuilder sb = new StringBuilder();
    for (final String segment : segments) {
      sb.append(segment);
    }
    return clazz + ' ' + sb.toString();
  }
}
