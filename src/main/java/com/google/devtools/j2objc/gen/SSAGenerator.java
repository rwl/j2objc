package com.google.devtools.j2objc.gen;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import llvm.Builder;
import llvm.TypeRef;
import llvm.Value;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.devtools.j2objc.util.NameTable;

public class SSAGenerator extends ErrorReportingASTVisitor {

  private final boolean asFunction;
  private final Builder builder;

  private final Stack<Value> valueStack = new Stack<Value>();
  private final Stack<TypeRef> typeStack = new Stack<TypeRef>();

  public static void generate(ASTNode node, Builder builder, boolean asFunction) {
    SSAGenerator ssaGenerator = new SSAGenerator(node, builder, asFunction);
    ssaGenerator.run(node);
  }

  public SSAGenerator(ASTNode node, Builder builder, boolean asFunction) {
    CompilationUnit unit = null;
    if (node != null && node.getRoot() instanceof CompilationUnit) {
      unit = (CompilationUnit) node.getRoot();
    }
    this.asFunction = asFunction;
    this.builder = builder;
  }

  @Override
  public boolean visit(SimpleName node) {
    IBinding binding = Types.getBinding(node);
    if (binding instanceof IVariableBinding) {
      IVariableBinding var = (IVariableBinding) binding;
      return false;
    }
    return false;
  }

  @Override
  public boolean visit(VariableDeclarationFragment node) {
//    node.getName().accept(this);
    if (node.getInitializer() != null) {
      node.getInitializer().accept(this);
    }
    return false;
  }

  @Override
  public boolean visit(VariableDeclarationStatement node) {
    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> vars = node.fragments(); // safe by definition
    assert !vars.isEmpty();

    ITypeBinding binding = Types.getTypeBinding(vars.get(0));
    final TypeRef llvmType = NameTable.javaRefToLLVM(binding);

    for (Iterator<VariableDeclarationFragment> it = vars.iterator(); it.hasNext(); ) {
      VariableDeclarationFragment f = it.next();

      final Value op = builder.BuildAlloca(llvmType.type(), NameTable.getName(f.getName()));

      if (f.getInitializer() != null) {
        f.getInitializer().accept(new ErrorReportingASTVisitor() {

          @Override
          public boolean visit(NumberLiteral node) {
            String token = node.getToken();
            ITypeBinding binding = Types.getTypeBinding(node);
            assert binding.isPrimitive();
            char kind = binding.getKey().charAt(0);  // Primitive types have single-character keys.

            if (kind == 'D' || kind == 'F') {
              builder.BuildStore(llvmType.ConstReal(Double.valueOf(token)), op);
            } else if (kind == 'J') {
            } else if (kind == 'I') {
              builder.BuildStore(llvmType.ConstInt(Integer.valueOf(token), true), op);
            }
            return false;
          }

        });
      }
//      f.accept(this);
    }
    return false;
  }

  @Override
  public boolean visit(Initializer node) {
    // All Initializer nodes should have been converted during initialization
    // normalization.
    throw new AssertionError("initializer node not converted");
  }

}
