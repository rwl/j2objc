package com.google.devtools.j2objc.gen;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;


import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.llvm.Builder;
import org.llvm.TypeRef;
import org.llvm.Value;

import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.devtools.j2objc.util.NameTable;
import com.google.devtools.j2objc.util.UnicodeUtils;

public class SSAGenerator extends ErrorReportingASTVisitor {

  private final boolean asFunction;
  private final Builder builder;

  private final Map<String, Value> namedValues;

  private final Stack<Value> valueStack = new Stack<Value>();
  private final Stack<TypeRef> typeStack = new Stack<TypeRef>();
  private final Stack<String> nameStack = new Stack<String>();

  public static void generate(ASTNode node, Builder builder, Map<String, Value> namedValues, boolean asFunction) {
    SSAGenerator generator = new SSAGenerator(node, builder, namedValues, asFunction);
    generator.run(node);

    assert generator.valueStack.size() == 0: "value stack: " + generator.valueStack.size();
    assert generator.typeStack.size() == 0: "type stack: " + generator.typeStack.size();
    assert generator.nameStack.size() == 0: "name stack: " + generator.nameStack.size();
  }

  public SSAGenerator(ASTNode node, Builder builder, Map<String, Value> namedValues, boolean asFunction) {
    CompilationUnit unit = null;
    if (node != null && node.getRoot() instanceof CompilationUnit) {
      unit = (CompilationUnit) node.getRoot();
    }
    this.asFunction = asFunction;
    this.builder = builder;
    this.namedValues = namedValues;
  }

  @Override
  public boolean visit(Assignment node) {
    Operator op = node.getOperator();
    Expression lhs = node.getLeftHandSide();
    Expression rhs = node.getRightHandSide();
    if (op == Operator.ASSIGN) {
      IVariableBinding var = Types.getVariableBinding(lhs);

      lhs.accept(this);
      rhs.accept(this);
      builder.buildStore(valueStack.pop(), namedValues.get(nameStack.pop()));
    }
    return false;
  }

  @Override
  public boolean visit(StringLiteral node) {
    String s = UnicodeUtils.escapeStringLiteral(node.getEscapedValue());
    Value.constString(s, s.length(), false);
    return false;
  }

  @Override
  public boolean visit(NumberLiteral node) {
    String token = node.getToken();
    ITypeBinding binding = Types.getTypeBinding(node);
    assert binding.isPrimitive();
    char kind = binding.getKey().charAt(0);  // Primitive types have single-character keys.

    Value value = null;
    if (kind == 'D' || kind == 'F') {
      value = TypeRef.doubleType().constReal(Double.valueOf(token));
    } else if (kind == 'J') {
      value = TypeRef.int64Type().constInt(Long.valueOf(token), true);
    } else if (kind == 'I') {
      value = TypeRef.int32Type().constInt(Integer.valueOf(token), true);
    }

    assert value != null;
    valueStack.push(value);

    return false;
  }

  @Override
  public boolean visit(SimpleName node) {
    IBinding binding = Types.getBinding(node);
    if (binding instanceof IVariableBinding) {
      IVariableBinding var = (IVariableBinding) binding;
      if (Types.isPrimitiveConstant(var)) {

      } else if (Types.isStaticVariable(var)) {

      } else {
        String name = NameTable.getName(node);
        nameStack.push(name);
      }
      return false;
    }
    return false;
  }

  @Override
  public boolean visit(VariableDeclarationFragment node) {
    node.getName().accept(this);
    String name = nameStack.pop();
    Value alloca = builder.buildAlloca(typeStack.pop().type(), name);
    valueStack.push(alloca);
    namedValues.put(name, alloca);

    if (node.getInitializer() != null) {
      node.getInitializer().accept(this);
      builder.buildStore(valueStack.pop(), valueStack.pop());
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
    typeStack.push(llvmType);

    for (Iterator<VariableDeclarationFragment> it = vars.iterator(); it.hasNext(); ) {
      VariableDeclarationFragment f = it.next();
      f.accept(this);
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
