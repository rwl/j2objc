package com.google.devtools.j2objc.gen;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;


import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.llvm.Builder;
import org.llvm.Module;
import org.llvm.TypeRef;
import org.llvm.Value;
import org.llvm.binding.LLVMLibrary.LLVMLinkage;

import com.google.common.collect.Lists;
import com.google.devtools.j2objc.types.IOSArrayTypeBinding;
import com.google.devtools.j2objc.types.IOSMethod;
import com.google.devtools.j2objc.types.IOSMethodBinding;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.ErrorReportingASTVisitor;
import com.google.devtools.j2objc.util.NameTable;
import com.google.devtools.j2objc.util.UnicodeUtils;

public class SSAGenerator extends AbstractGenerator {

  private final boolean asFunction;
  private final Builder builder;
  private final Module mod;

  private final Map<String, Value> namedValues;

  private final Stack<Value> valueStack = new Stack<Value>();
  private final Stack<TypeRef> typeStack = new Stack<TypeRef>();
  private final Stack<String> nameStack = new Stack<String>();

  private static Value funcObjcLookupClass = null;
  private static Value funcObjcMsgLookup = null;

  public static void generate(ASTNode node, Module mod, Builder builder, Map<String, Value> namedValues, boolean asFunction) {
    SSAGenerator generator = new SSAGenerator(node, mod, builder, namedValues, asFunction);
    generator.run(node);

    assert generator.valueStack.size() == 0: "value stack: " + generator.valueStack.size();
    assert generator.typeStack.size() == 0: "type stack: " + generator.typeStack.size();
    assert generator.nameStack.size() == 0: "name stack: " + generator.nameStack.size();
  }

  public SSAGenerator(ASTNode node, Module mod, Builder builder, Map<String, Value> namedValues, boolean asFunction) {
    CompilationUnit unit = null;
    if (node != null && node.getRoot() instanceof CompilationUnit) {
      unit = (CompilationUnit) node.getRoot();
    }
    this.asFunction = asFunction;
    this.mod = mod;
    this.builder = builder;
    this.namedValues = namedValues;
  }

  private Value[] buildArguments(IMethodBinding method, List<Expression> args) {
    if (method != null && method.isVarargs()) {
      return buildVarArgs(method, args);
    } else if (!args.isEmpty()) {
      int nArgs = args.size();
      Value[] argVals = new Value[nArgs];
      for (int i = 0; i < nArgs; i++) {
        Expression arg = args.get(i);
        buildArgument(method, arg, i);
        argVals[i] = valueStack.pop();
      }
      return argVals;
    }
    return new Value[0];
  }

  private void buildArgument(IMethodBinding method, Expression arg, int index) {
    if (method != null) {
      IOSMethod iosMethod = getIOSMethod(method);
      if (iosMethod != null) {
        // mapped methods already have converted parameters
        if (index > 0) {
          //iosMethod.getParameters().get(index).getParameterName();
        }
      } else if (method.getDeclaringClass() instanceof IOSArrayTypeBinding) {
        assert method.getName().startsWith("arrayWith");
        if (index == 1) {
          //"count); // IOSArray methods' 2nd parameter is the same.
        } else if (index == 2) {
          assert method.getName().equals("arrayWithObjects");
          //"type";
        }
      } else {
        method = Types.getOriginalMethodBinding(method.getMethodDeclaration());
        ITypeBinding[] parameterTypes = method.getParameterTypes();
        assert index < parameterTypes.length : "method called with fewer parameters than declared";
        ITypeBinding parameter = parameterTypes[index];
        String typeName = method.isParameterizedMethod() || parameter.isTypeVariable()
            ? "id" : getSimpleTypeName(Types.mapType(parameter));
        if (typeName.equals("long long")) {
          typeName = "long";
        }
        String keyword = ObjectiveCSourceFileGenerator.parameterKeyword(typeName, parameter);
        if (index == 0) {
          keyword = NameTable.capitalize(keyword);
        }
        //keyword;
      }
    }
    //':';
    if (arg instanceof ArrayInitializer) {
      //printArrayLiteral((ArrayInitializer) arg);
    } else {
      arg.accept(this);
    }
  }

  private Value[] buildVarArgs(IMethodBinding method, List<Expression> args) {
    method = method.getMethodDeclaration();
    ITypeBinding[] parameterTypes = method.getParameterTypes();
    Iterator<Expression> it = args.iterator();
    Value[] argVals = new Value[parameterTypes.length];
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i < parameterTypes.length - 1) {
        // Not the last parameter
        buildArgument(method, it.next(), i);
        argVals[i] = valueStack.pop();
        if (it.hasNext() || i + 1 < parameterTypes.length) {
          //' ';
        }
      } else if (hasVarArgsTarget(method)) {
        if (i == 0) {
          //':';
          if (it.hasNext()) {
            it.next().accept(this);
            argVals[i] = valueStack.pop();
          }
        }
        // Method mapped to Obj-C varargs method call, so just append args.
        while (it.hasNext()) {
          //", ";
          it.next().accept(this);
          argVals[i] = valueStack.pop();
        }
        //", nil";
      } else {
        // Last parameter; Group remain arguments into an array.
        assert parameterTypes[i].isArray();
        if (method instanceof IOSMethodBinding) {
          if (i > 0) {
            IOSMethod iosMethod = getIOSMethod(method);
            //iosMethod.getParameters().get(i).getParameterName();
          }
        } else {
          String typename = getSimpleTypeName(Types.mapType(parameterTypes[i]));
          String keyword =
              ObjectiveCSourceFileGenerator.parameterKeyword(typename, parameterTypes[i]);
          if (i == 0) {
            keyword = NameTable.capitalize(keyword);
          }
          //keyword;
        }
        //':';
        List<Expression> objs = Lists.newArrayList(it);
        if (objs.size() == 1 && Types.getTypeBinding(objs.get(0)).isArray() &&
            parameterTypes[i].getDimensions() == 1) {
          // Varargs method invoked with an array, so just pass it on.
          objs.get(0).accept(this);
        } else {
          //"[IOSObjectArray arrayWithType:";
          //buildObjectArrayType(parameterTypes[i].getElementType());
          //" count:";
          //objs.size();
          it = objs.iterator();
          while (it.hasNext()) {
            //", ";
            it.next().accept(this);
            argVals[i] = valueStack.pop();
          }
          //" ]";
        }
      }
    }
    return argVals;
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

  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(MethodInvocation node) {
    String methodName = NameTable.getName(node.getName());
    IMethodBinding binding = Types.getMethodBinding(node);
    assert binding != null;
    // Object receiving the message, or null if it's a method in this class.
    Expression receiver = node.getExpression();
    ITypeBinding receiverType = receiver != null ? Types.getTypeBinding(receiver) : null;

    if (Types.isFunction(binding)) {
      //methodName;
      //"(";
      int narg = node.arguments().size();
      TypeRef[] paramTypes = new TypeRef[narg - (binding.isVarargs() ? 1 : 0)];
      Value[] argVals = new Value[narg];
      int i = 0;
      for (Iterator<Expression> it = node.arguments().iterator(); it.hasNext(); i++) {
        Expression expr = it.next();
        ITypeBinding typeBinding = Types.getTypeBinding(expr);
        final TypeRef llvmType = NameTable.javaRefToLLVM(typeBinding);
        typeStack.push(llvmType);

        expr.accept(this);
        if (it.hasNext()) {
          //", ";
          paramTypes[i] = typeStack.pop();
        } else {
          typeStack.pop();
        }
        argVals[i] = valueStack.pop();
      }
      TypeRef returnType = TypeRef.voidType();//NameTable.javaRefToLLVM(binding.getReturnType());
      TypeRef funcType = TypeRef.functionType(returnType, binding.isVarargs(), paramTypes);
      Value func = mod.addFunction(methodName, funcType);

      builder.buildCall(func, "", argVals);
    } else {
      if (funcObjcLookupClass == null) {
        buildFuncObjcLookupClass();
      }
      if (funcObjcMsgLookup == null) {
        buildFuncObjcMsgLookup();
      }
      if (binding instanceof IOSMethodBinding) {
        //binding.getName();
      } else {
        //methodName;
      }
      Value[] args = buildArguments(binding, node.arguments());
      builder.buildCall(funcObjcLookupClass, "", args);
    }
    return false;
  }

  private void buildFuncObjcLookupClass() {
    TypeRef ty_i8p = TypeRef.intType(8).pointerType();
    TypeRef ty_func = TypeRef.functionType(ty_i8p, true, ty_i8p);

    funcObjcLookupClass = mod.addFunction("objc_lookup_class", ty_func);
  }

  private void buildFuncObjcMsgLookup() {
    TypeRef ty_i8p = TypeRef.intType(8).pointerType();
    TypeRef ty_func = TypeRef.functionType(ty_i8p, ty_i8p, ty_i8p);

    funcObjcMsgLookup = mod.addFunction("objc_msg_lookup", ty_func);
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
  public boolean visit(StringLiteral node) {
    String s = node.getLiteralValue();//UnicodeUtils.escapeStringLiteral(node.getLiteralValue());

    Value str = builder.buildGlobalStringPtr(s, "");

    Value obj_str = mod.addGlobal(TypeRef.structType(
        TypeRef.int8Type().pointerType(0),
        TypeRef.int8Type().pointerType(0),
        TypeRef.int32Type()), ".objc_str");
    obj_str.setLinkage(LLVMLinkage.LLVMInternalLinkage);
    obj_str.setInitializer(Value.constStruct(
        TypeRef.int8Type().pointerType().constNull(),
        str,
        TypeRef.int32Type().constInt(s.length(), true)));

    Value bitcast = builder.buildBitCast(obj_str, NameTable.OPAQUE_TYPE.type(), "");

    valueStack.push(bitcast);

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
