/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.types;

import com.google.common.collect.Sets;
import com.google.devtools.j2objc.sym.Symbols;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.Set;

/**
 * Binding class for types created during translation.
 *
 * @author Keith Stanger
 */
public class GeneratedTypeBinding implements ITypeBinding {

  protected final String name;
  private final IPackageBinding packageBinding;
  private final ITypeBinding superClass;
  private final boolean isInterface;
  private final boolean isArray;
  private final Set<IVariableBinding> fields = Sets.newHashSet();
  private final Set<IMethodBinding> methods = Sets.newHashSet();

  public GeneratedTypeBinding(
      String name, IPackageBinding packageBinding, ITypeBinding superClass, boolean isInterface,
      boolean isArray) {
    this.name = name;
    this.packageBinding = packageBinding;
    this.superClass = superClass;
    this.isInterface = isInterface;
    this.isArray = isArray;
    Symbols.queueForResolution(this);
  }

  /**
   * Creates a new non-array type, extracting the package from the provided name.
   */
  public static GeneratedTypeBinding newTypeBinding(
      String name, ITypeBinding superClass, boolean isInterface) {
    int idx = name.lastIndexOf('.');
    IPackageBinding packageBinding = null;
    if (idx >= 0) {
      packageBinding = new GeneratedPackageBinding(name.substring(0, idx));
      name = name.substring(idx + 1);
    }
    return new GeneratedTypeBinding(name, packageBinding, superClass, isInterface, false);
  }

  @Override
  public IAnnotationBinding[] getAnnotations() {
    return new IAnnotationBinding[0];
  }

  @Override
  public int getKind() {
    return IBinding.TYPE;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean isRecovered() {
    return false;
  }

  @Override
  public boolean isSynthetic() {
    return true;
  }

  @Override
  public String getKey() {
    return name;
  }

  @Override
  public boolean isEqualTo(IBinding binding) {
    if (binding == this) {
      return true;
    }
    if (binding instanceof GeneratedTypeBinding) {
      return name.equals(((GeneratedTypeBinding) binding).name);
    }
    return false;
  }

  @Override
  public ITypeBinding createArrayType(int dimension) {
    throw new AssertionError("not implemented");
  }

  @Override
  public String getBinaryName() {
    return getQualifiedName();
  }

  @Override
  public ITypeBinding getErasure() {
    return this;
  }

  @Override
  public ITypeBinding[] getInterfaces() {
    return new ITypeBinding[0];
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getQualifiedName() {
    if (packageBinding != null) {
      return packageBinding.getName() + "." + name;
    }
    return name;
  }

  @Override
  public boolean isArray() {
    return isArray;
  }

  @Override
  public boolean isClass() {
    return !isInterface;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public boolean isFromSource() {
    return false;
  }

  @Override
  public boolean isGenericType() {
    return false;
  }

  @Override
  public boolean isInterface() {
    return isInterface;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  @Override
  public boolean isMember() {
    return false;
  }

  @Override
  public boolean isNested() {
    return false;
  }

  @Override
  public boolean isNullType() {
    return false;
  }

  @Override
  public boolean isParameterizedType() {
    return false;
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  public boolean isRawType() {
    return false;
  }

  @Override
  public boolean isTopLevel() {
    return true;
  }

  @Override
  public boolean isTypeVariable() {
    return false;
  }

  @Override
  public boolean isUpperbound() {
    return false;
  }

  @Override
  public boolean isWildcardType() {
    return false;
  }

  // unused methods

  @Override
  public IJavaElement getJavaElement() {
    return null;
  }

  @Override
  public ITypeBinding getBound() {
    return null;
  }

  @Override
  public ITypeBinding getGenericTypeOfWildcardType() {
    return null;
  }

  @Override
  public int getRank() {
    return -1;
  }

  @Override
  public ITypeBinding getComponentType() {
    return isArray ? Types.getNSObject() : null;
  }

  @Override
  public IVariableBinding[] getDeclaredFields() {
    return fields.toArray(new IVariableBinding[0]);
  }

  public void addField(IVariableBinding field) {
    fields.add(field);
  }

  @Override
  public IMethodBinding[] getDeclaredMethods() {
    return methods.toArray(new IMethodBinding[0]);
  }

  public void addMethod(IMethodBinding method) {
    methods.add(method);
  }

  @Override
  public int getDeclaredModifiers() {
    return Modifier.PUBLIC;
  }

  @Override
  public ITypeBinding[] getDeclaredTypes() {
    return new ITypeBinding[0];
  }

  @Override
  public ITypeBinding getDeclaringClass() {
    return null; // no outer class declared this type
  }

  @Override
  public IMethodBinding getDeclaringMethod() {
    return null;
  }

  @Override
  public int getDimensions() {
    return isArray ? 1 : 0;
  }

  @Override
  public ITypeBinding getElementType() {
    return isArray ? Types.getNSObject() : null;
  }

  @Override
  public int getModifiers() {
    return 0;
  }

  @Override
  public IPackageBinding getPackage() {
    return packageBinding;
  }

  @Override
  public ITypeBinding getSuperclass() {
    return superClass;
  }

  @Override
  public ITypeBinding[] getTypeArguments() {
    return new ITypeBinding[0];
  }

  @Override
  public ITypeBinding[] getTypeBounds() {
    return new ITypeBinding[0];
  }

  @Override
  public ITypeBinding getTypeDeclaration() {
    return this;
  }

  @Override
  public ITypeBinding[] getTypeParameters() {
    return new ITypeBinding[0];
  }

  @Override
  public ITypeBinding getWildcard() {
    return null;
  }

  @Override
  public boolean isAnnotation() {
    return false;
  }

  @Override
  public boolean isAnonymous() {
    return false;
  }

  @Override
  public boolean isAssignmentCompatible(ITypeBinding variableType) {
    return isEqualTo(variableType);
  }

  @Override
  public boolean isCapture() {
    return false;
  }

  @Override
  public boolean isCastCompatible(ITypeBinding type) {
    return isEqualTo(type);
  }

  @Override
  public boolean isSubTypeCompatible(ITypeBinding type) {
    throw new AssertionError("not implemented");
  }

  @Override
  public String toString() {
    return name;
  }
}
