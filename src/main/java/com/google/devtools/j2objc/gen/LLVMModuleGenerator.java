package com.google.devtools.j2objc.gen;

import java.util.List;
import java.util.Map;


import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.llvm.BasicBlock;
import org.llvm.Builder;
import org.llvm.Module;
import org.llvm.TypeRef;
import org.llvm.Value;
import org.llvm.binding.LLVMLibrary.LLVMAttribute;
import org.llvm.binding.LLVMLibrary.LLVMVisibility;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.devtools.j2objc.J2ObjC.Language;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.NameTable;

/**
 * Generates LLVM byte-code (.ll) files from compilation units.
 *
 * @author Richard Lincoln
 */
public class LLVMModuleGenerator extends ObjectiveCSourceFileGenerator {

  /**
   * The LLVM construct that contains all of the functions and global
   * variables in a piece of code.
   */
  private final Module mod;

  /**
   * Helper object that makes it easy to generate LLVM instructions.
   */
  private final Builder irBuilder;

  /**
   * Keeps track of which values are defined in the current scope and what
   * their LLVM representation is. Method parameters will be in this map when
   * generating code for their method body.
   */
  private final Map<String, Value> namedValues = Maps.newHashMap();

  /**
   * Static Single Assignment (SSA) registers
   */
//  private final List<Value> values = Lists.newArrayList();

  /**
   * Suffix for LLVM byte-code file
   */
  private final String suffix;

  /**
   * Generate an LLVM module file for the specified compilation unit.
   */
  public static void generate(String fileName, Language language,
      String source, CompilationUnit unit) {
    LLVMModuleGenerator moduleGenerator = new LLVMModuleGenerator(fileName,
        language, source, unit);
    moduleGenerator.generate(unit);
    moduleGenerator.mod.dumpModule();
  }

  public LLVMModuleGenerator(String sourceFileName, Language language,
      String source, CompilationUnit unit) {
    super(sourceFileName, source, unit, false);
    mod = Module.createWithName(sourceFileName);
    irBuilder = Builder.createBuilder();
    suffix = language.getSuffix();
  }

  @Override
  protected String getSuffix() {
    return suffix;
  }

  public void generate(CompilationUnit unit) {
    @SuppressWarnings("unchecked")
    List<AbstractTypeDeclaration> types = unit.types(); // safe by definition

    for (AbstractTypeDeclaration type : types) {
      newline();
      generate(type);
    }
    save(unit);
  }

  public void generate(TypeDeclaration node) {
    if (!node.isInterface()) {
      String typeName = NameTable.getFullName(node);
      List<FieldDeclaration> fields = Lists.newArrayList(node.getFields());
//      printStaticReferencesMethod(fields);
//      printStaticVars(Lists.newArrayList(node.getFields()), /* isInterface */ false);
//      printProperties(node.getFields());
      printMethods(node);
//      printObjCTypeMethod(node);

      // Generate main method, if declared.
      MethodDeclaration main = null;
      for (MethodDeclaration m : node.getMethods()) {
        if (isMainMethod(m)) {
          main = m;
          break;
        }
      }
      if (main != null) {
        printMainMethod(main, typeName);
      }
    }
  }

  public void generate(EnumDeclaration node) {

  }

  @Override
  protected void generate(AnnotationTypeDeclaration node) {
    // No implementation for annotations.
  }

  private void printMethods(TypeDeclaration node) {
    printMethods(Lists.newArrayList(node.getMethods()));
  }

  private void printMainMethod(MethodDeclaration m, String typeName) {
    TypeRef ty_i32 = TypeRef.intType(32);
    TypeRef ty_i8pp = TypeRef.intType(8).pointerType().pointerType();
    TypeRef ty_func = TypeRef.functionType(ty_i32, ty_i32, ty_i8pp);

    Value f_main = mod.addFunction("main", ty_func.type());
    f_main.addFunctionAttr(LLVMAttribute.LLVMNoUnwindAttribute);
    f_main.addFunctionAttr(LLVMAttribute.LLVMUWTable);

//    Value fuz = Value.constString("fuz", 3, false);
//    mod.addGlobal( , ".objc_str");
//    TypeRef ty_int = TypeRef.int32Type();
//    Value g = mod.addGlobal(ty_int, "globl");
//    g.setVisibility(LLVMVisibility.LLVMHiddenVisibility);
//
//    TypeRef ty_opaque = TypeRef.structTypeNamed("");
//    mod.addGlobal(ty_opaque, "opg");

    Value argc = f_main.getParam(0);
    argc.setValueName("argc");
    Value argv = f_main.getParam(1);
    argv.setValueName("argv");

    BasicBlock bb = f_main.appendBasicBlock("entrypoint");

    irBuilder.positionBuilderAtEnd(bb);

    Value i1 = irBuilder.buildAlloca(ty_i32.type(), "i1");
    Value i2 = irBuilder.buildAlloca(ty_i32.type(), "i2");
    Value i3 = irBuilder.buildAlloca(ty_i32.type(), "i3");

    irBuilder.buildStore(ty_i32.constInt(0, true), i1);
    irBuilder.buildStore(argc, i2);
    irBuilder.buildStore(argv, i3);

    if (m != null) {
      @SuppressWarnings("unchecked")
      List<SingleVariableDeclaration> params = m.parameters();
      assert params.size() == 1;  // Previously checked in isMainMethod().
      printMethodBody(m, true);
    }

    irBuilder.buildRet(ty_i32.constInt(0, true));
  }

  private void printMethodBody(MethodDeclaration m, boolean isFunction) throws AssertionError {
    for (Object stmt : m.getBody().statements()) {
      if (stmt instanceof Statement) {
        generateStatement((Statement) stmt, isFunction);
      } else {
        throw new AssertionError("unexpected AST type: " + stmt.getClass());
      }
    }
  }

  @Override
  protected void printStaticConstructorDeclaration(MethodDeclaration m) {

  }

  private void generateStatement(Statement stmt, boolean asFunction) {
    SSAGenerator.generate(stmt, mod, irBuilder, namedValues, asFunction);
  }

  @Override
  protected String methodDeclaration(MethodDeclaration m) {
    assert !m.isConstructor();
    boolean isStatic = Modifier.isStatic(m.getModifiers());
    IMethodBinding binding = Types.getMethodBinding(m);
    String methodName = NameTable.getName(binding);

    String baseDeclaration = String.format("%c (%s)%s", isStatic ? '+' : '-',
        NameTable.javaRefToObjC(m.getReturnType2()), methodName);

    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> params = m.parameters(); // safe by definition
//    parametersDeclaration(Types.getOriginalMethodBinding(binding), params, baseDeclaration, sb);
    return "";
  }

}
