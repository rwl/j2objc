package com.google.devtools.j2objc.wrapper;

import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;

import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.devtools.j2objc.util.NameTable;
import com.google.j2objc.annotations.Bind;

public class Renamer extends ErrorReportingASTVisitor {

  @Override
  public boolean visit(EnumConstantDeclaration node) {
    return super.visit(node);
  }

  @Override
  public boolean visit(EnumDeclaration node) {
    return super.visit(node);
  }

  @Override
  public boolean visit(QualifiedName node) {
    IBinding binding = node.resolveBinding();
    if (binding != null) {
      for (IAnnotationBinding annotation : binding.getAnnotations()) {
        if (annotation.getAnnotationType().getQualifiedName().equals(Bind.class.getName())) {
          for (IMemberValuePairBinding pair : annotation.getDeclaredMemberValuePairs()) {
            if (pair.getName().equals("value")) {
              String value = (String) pair.getValue();
              if (!value.isEmpty()) {
                SimpleName name = node.getName();
                NameTable.rename(name, value);
              }
            }
          }
        }
      }
    }
    return super.visit(node);
  }

}
