package com.google.devtools.j2objc.gen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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

import com.github.rwl.irbuilder.IRBuilder;
import com.github.rwl.irbuilder.enums.AttrKind;
import com.github.rwl.irbuilder.types.ArrayType;
import com.github.rwl.irbuilder.types.FunctionType;
import com.github.rwl.irbuilder.types.IType;
import com.github.rwl.irbuilder.types.IntType;
import com.github.rwl.irbuilder.types.NamedType;
import com.github.rwl.irbuilder.types.OpaqueType;
import com.github.rwl.irbuilder.types.StructType;
import com.github.rwl.irbuilder.values.IValue;
import com.github.rwl.irbuilder.values.IntValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.devtools.j2objc.J2ObjC;
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
   * Helper object that makes it easy to generate LLVM instructions.
   */
  private final IRBuilder irBuilder;

//  private final Map<String, NamedType> namedTypes = Maps.newHashMap();

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
    System.out.println(moduleGenerator.irBuilder.build());
  }

  public LLVMModuleGenerator(String sourceFileName, Language language,
      String source, CompilationUnit unit) {
    super(sourceFileName, source, unit, false);
    irBuilder = new IRBuilder(sourceFileName);
    suffix = language.getSuffix();
  }

  @Override
  protected String getSuffix() {
    return suffix;
  }

  @Override
  protected void save(String path) {
    try {
      File outputFile = new File(getOutputDirectory(), path);
      File dir = outputFile.getParentFile();
      if (dir != null && !dir.exists()) {
        if (!dir.mkdirs()) {
          J2ObjC.warning("cannot create output directory: " + getOutputDirectory());
        }
      }
      String source = irBuilder.build();

      // Make sure file ends with a new-line.
      if (!source.endsWith("\n")) {
        source += '\n';
      }

      Files.write(source, outputFile, Charset.defaultCharset());
    } catch (IOException e) {
      J2ObjC.error(e.getMessage());
    } finally {
      reset();
    }
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
      //printStaticReferencesMethod(fields);
      //printStaticVars(Lists.newArrayList(node.getFields()), /* isInterface */ false);
      //printProperties(node.getFields());
      printMethods(node);
      //printObjCTypeMethod(node);

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
    IType int8pp = IntType.INT_8.pointerTo().pointerTo();
    irBuilder.beginFunction(IntType.INT_32, "main",
        ImmutableList.<IType>builder()
          .add(IntType.INT_32)
          .add(int8pp)
          .build(),
        ImmutableList.<String>builder()
          .add("argc")
          .add("argv")
          .build(),
        ImmutableList.<AttrKind>builder()
          //.add(AttrKind.NO_UNWIND)
          .add(AttrKind.UWTABLE)
          .add(AttrKind.STACK_PROTECT)
          .build(), false)
        .alloca(IntType.INT_32, "i1", null)
        .alloca(IntType.INT_32, "i2", null)
        .alloca(int8pp, "i3", null)
        .store("i1", new IntValue(0))
        .store("i2", "argc")
        .store("i3", "argv");

    if (m != null) {
      @SuppressWarnings("unchecked")
      List<SingleVariableDeclaration> params = m.parameters();
      assert params.size() == 1;  // Previously checked in isMainMethod().
      printMethodBody(m, true);
    }

    irBuilder.endFunction(new IntValue(0));
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
    SSAGenerator.generate(stmt, irBuilder, null/*, namedTypes*/, asFunction);
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
