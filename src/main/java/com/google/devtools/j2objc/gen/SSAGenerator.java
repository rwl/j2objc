package com.google.devtools.j2objc.gen;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
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

import com.github.rwl.irbuilder.IRBuilder;
import com.github.rwl.irbuilder.enums.AttrKind;
import com.github.rwl.irbuilder.enums.Linkage;
import com.github.rwl.irbuilder.types.ArrayType;
import com.github.rwl.irbuilder.types.FunctionType;
import com.github.rwl.irbuilder.types.IType;
import com.github.rwl.irbuilder.types.IntType;
import com.github.rwl.irbuilder.types.LongType;
import com.github.rwl.irbuilder.types.OpaqueType;
import com.github.rwl.irbuilder.values.BitCast;
import com.github.rwl.irbuilder.values.DoubleValue;
import com.github.rwl.irbuilder.values.GlobalVariable;
import com.github.rwl.irbuilder.values.IValue;
import com.github.rwl.irbuilder.values.IntValue;
import com.github.rwl.irbuilder.values.LongValue;
import com.github.rwl.irbuilder.values.StringValue;
import com.github.rwl.irbuilder.values.StructValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.j2objc.types.GeneratedMethodBinding;
import com.google.devtools.j2objc.types.IOSArrayTypeBinding;
import com.google.devtools.j2objc.types.IOSMethod;
import com.google.devtools.j2objc.types.IOSMethodBinding;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.NameTable;
import com.google.devtools.j2objc.util.NamedTypes;

public class SSAGenerator extends AbstractGenerator {

  private final boolean asFunction;
  private final IRBuilder builder;
  private final NamedTypes namedTypes;

//  private final Map<String, NamedType> namedTypes;

  private final List<IValue> llvmUsed;

  private final Stack<IValue> valueStack = new Stack<IValue>();
  private final Stack<IType> typeStack = new Stack<IType>();
  private final Stack<String> nameStack = new Stack<String>();

  private static GlobalVariable constStringClassRef = null;

  private GlobalVariable objcMsgSend = null;

  private static final String OBJC_MSG_SEND = "objc_msgSend";

  private static final String STR_NAME = ".str";
  private static final String UNAMED_CF_STRING_NAME = "_unamed_cfstring_";
  private static final String CONST_STRING_CLASS_REF_NAME = "__CFConstantStringClassReference";
  private static final String OBJC_CLASS_LIST_REFERENCES_NAME = "\"\\01L_OBJC_CLASSLIST_REFERENCES_$_\"";
  private static final String OBJC_METH_VAR_NAME_NAME = "\"\\01L_OBJC_METH_VAR_NAME_\"";
  private static final String OBJC_SEL_REFERENCES_NAME = "\"\\01L_OBJC_SELECTOR_REFERENCES_\"";

  public static void generate(ASTNode node, IRBuilder builder,
      List<IValue> llvmUsed, boolean asFunction) {
    SSAGenerator generator = new SSAGenerator(node, builder, llvmUsed,
        asFunction);
    generator.run(node);

    assert generator.valueStack.size() == 0: "value stack: " + generator.valueStack.size();
    assert generator.typeStack.size() == 0: "type stack: " + generator.typeStack.size();
    assert generator.nameStack.size() == 0: "name stack: " + generator.nameStack.size();
  }

  public SSAGenerator(ASTNode node, IRBuilder builder, List<IValue> llvmUsed,
      boolean asFunction) {
    CompilationUnit unit = null;
    if (node != null && node.getRoot() instanceof CompilationUnit) {
      unit = (CompilationUnit) node.getRoot();
    }
    this.asFunction = asFunction;
    this.builder = builder;
    this.namedTypes = new NamedTypes(builder);
    this.llvmUsed = llvmUsed;
  }

  private List<IValue> buildArguments(IMethodBinding method, List<Expression> args) {
    if (method != null && method.isVarargs()) {
      return buildVarArgs(method, args);
    } else if (!args.isEmpty()) {
      int nArgs = args.size();
      List<IValue> argVals = Lists.newArrayList();
      for (int i = 0; i < nArgs; i++) {
        Expression arg = args.get(i);
        buildArgument(method, arg, i);
        argVals.add(valueStack.pop());
      }
      return argVals;
    }
    return ImmutableList.of();
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

  private List<IValue> buildVarArgs(IMethodBinding method, List<Expression> args) {
    method = method.getMethodDeclaration();
    ITypeBinding[] parameterTypes = method.getParameterTypes();
    Iterator<Expression> it = args.iterator();
    List<IValue> argVals = Lists.newArrayList();
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i < parameterTypes.length - 1) {
        // Not the last parameter
        buildArgument(method, it.next(), i);
        argVals.add(valueStack.pop());
        if (it.hasNext() || i + 1 < parameterTypes.length) {
          //' ';
        }
      } else if (hasVarArgsTarget(method)) {
        if (i == 0) {
          //':';
          if (it.hasNext()) {
            it.next().accept(this);
            argVals.add(valueStack.pop());
          }
        }
        // Method mapped to Obj-C varargs method call, so just append args.
        while (it.hasNext()) {
          //", ";
          it.next().accept(this);
          argVals.add(valueStack.pop());
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
            argVals.add(valueStack.pop());
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
      builder.store(nameStack.pop(), valueStack.pop());
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean visit(ClassInstanceCreation node) {
    ITypeBinding type = Types.getTypeBinding(node.getType());
    ITypeBinding outerType = type.getDeclaringClass();
    String fullName = NameTable.getFullName(type);
    IMethodBinding method = Types.getMethodBinding(node);
    List<Expression> arguments = node.arguments();
    if (node.getExpression() != null && type.isMember() && arguments.size() > 0 &&
        !Types.getTypeBinding(arguments.get(0)).isEqualTo(outerType)) {
      // This is calling an untranslated "Outer.new Inner()" method,
      // so update its binding and arguments as if it had been translated.
      GeneratedMethodBinding newBinding = new GeneratedMethodBinding(method);
      newBinding.addParameter(0, outerType);
      method = newBinding;
      arguments = Lists.newArrayList(node.arguments());
      arguments.add(0, node.getExpression());
    }

    GlobalVariable objcClass = builder.global(String.format("\"OBJC_CLASS_$_%s\"",
        fullName), namedTypes.getClassT(), Linkage.EXTERNAL, false);
    GlobalVariable objcClassListRefs = builder.global(OBJC_CLASS_LIST_REFERENCES_NAME,
        objcClass, Linkage.INTERNAL, false);

    GlobalVariable objcMethVarName = builder.global(OBJC_METH_VAR_NAME_NAME,
        new StringValue(method.getName() + ':'), Linkage.INTERNAL, false);
    GlobalVariable objcSelRefs = builder.global(OBJC_SEL_REFERENCES_NAME,
        objcMethVarName, Linkage.INTERNAL, false);

    IValue objcClassListRefsLocal = builder.load(objcClassListRefs, null);
    IValue objcSelRefsLocal = builder.load(objcSelRefs, null);
    builder.bitcast(objcClassListRefsLocal, IntType.INT_8P, null);

    List<IValue> args = buildArguments(method, arguments);

    IType methType = new FunctionType(IntType.INT_8P, IntType.INT_8P,
        IntType.INT_8P, namedTypes.getOpaqueType()).pointerTo();
    builder.call(new BitCast(getObjcMsgSend(), methType), args, null);

    llvmUsed.add(new BitCast(objcClassListRefs, IntType.INT_8P));
    llvmUsed.add(objcMethVarName);
    llvmUsed.add(new BitCast(objcSelRefs, IntType.INT_8P));
    return false;
  }

  private GlobalVariable getObjcMsgSend() {
    if (objcMsgSend == null) {
      FunctionType funcType = new FunctionType(IntType.INT_8P,
          Lists.newArrayList(IntType.INT_8P, IntType.INT_8P), true);
      objcMsgSend = builder.functionDecl(OBJC_MSG_SEND, funcType,
          AttrKind.NON_LAZY_BIND);
    }
    return objcMsgSend;
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
      List<IType> paramTypes = Lists.newArrayList();
      List<IValue> argVals = Lists.newArrayList();
      for (Iterator<Expression> it = node.arguments().iterator(); it.hasNext();) {
        Expression expr = it.next();
        ITypeBinding typeBinding = Types.getTypeBinding(expr);
        final IType llvmType = javaRefToLLVM(typeBinding);
        typeStack.push(llvmType);

        expr.accept(this);
        if (it.hasNext()) {
          //", ";
          paramTypes.add(typeStack.pop());
        } else {
          typeStack.pop();
        }
        argVals.add(valueStack.pop());
      }
      GlobalVariable methVar = builder.functionDecl(methodName,
          new FunctionType(javaRefToLLVM(binding.getReturnType()), paramTypes,
              binding.isVarargs()));
      builder.call(methVar, argVals, null);
    } else {
      if (binding instanceof IOSMethodBinding) {
        //binding.getName();
      } else {
        //methodName;
      }
      List<IValue> args = buildArguments(binding, node.arguments());
//      builder.call(funcObjcLookupClass, "", args);
    }
    return false;
  }

  @Override
  public boolean visit(NumberLiteral node) {
    String token = node.getToken();
    ITypeBinding binding = Types.getTypeBinding(node);
    assert binding.isPrimitive();
    char kind = binding.getKey().charAt(0);  // Primitive types have single-character keys.

    IValue value = null;
    if (kind == 'D' || kind == 'F') {
      value = new DoubleValue(Double.valueOf(token));
    } else if (kind == 'J') {
      value = new LongType(Long.valueOf(token));
    } else if (kind == 'I') {
      value = new IntValue(Integer.valueOf(token));
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
    String s = node.getLiteralValue();

    GlobalVariable str = builder.constant(STR_NAME, new StringValue(s),
        Linkage.LINKER_PRIVATE, true);

    StructValue struct = new StructValue(new IValue[] {
       getConstStringClassRef(),
       new IntValue(1992),
       str,
       new LongValue(s.length())
    }, namedTypes.getStructNSConstString());

    GlobalVariable unamed = builder.constant(UNAMED_CF_STRING_NAME, struct,
        null, false);

    IValue bitcast = new BitCast(unamed, namedTypes.getOpaqueType());

    valueStack.push(bitcast);

    return false;
  }

  private GlobalVariable getConstStringClassRef() {
    if (constStringClassRef == null) {
      constStringClassRef = builder.global(CONST_STRING_CLASS_REF_NAME,
          new ArrayType(IntType.INT_32, 0), Linkage.EXTERNAL, false);
    }
    return constStringClassRef;
  }

  @Override
  public boolean visit(VariableDeclarationFragment node) {
    node.getName().accept(this);  // SimpleName
    String name = nameStack.pop();
    builder.alloca(typeStack.pop(), name, null);
//    valueStack.push(alloca);
//    namedValues.put(name, alloca);

    if (node.getInitializer() != null) {
      node.getInitializer().accept(this);
      builder.store(name, valueStack.pop());
    }
    return false;
  }

  @Override
  public boolean visit(VariableDeclarationStatement node) {
    @SuppressWarnings("unchecked")
    List<VariableDeclarationFragment> vars = node.fragments(); // safe by definition
    assert !vars.isEmpty();

    ITypeBinding binding = Types.getTypeBinding(vars.get(0));
    final IType llvmType = javaRefToLLVM(binding);
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

  private IType javaRefToLLVM(ITypeBinding binding) {
    IType type = NameTable.javaRefToLLVM(binding);
    if (type.equals(OpaqueType.INSTANCE)) {
      type = namedTypes.getOpaqueType();
    }
    return type;
  }

}
