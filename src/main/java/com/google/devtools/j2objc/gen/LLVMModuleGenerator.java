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
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.github.rwl.irbuilder.IRBuilder;
import com.github.rwl.irbuilder.enums.AttrKind;
import com.github.rwl.irbuilder.enums.Linkage;
import com.github.rwl.irbuilder.types.ArrayType;
import com.github.rwl.irbuilder.types.FunctionType;
import com.github.rwl.irbuilder.types.IType;
import com.github.rwl.irbuilder.types.IntType;
import com.github.rwl.irbuilder.values.ArrayValue;
import com.github.rwl.irbuilder.values.GlobalVariable;
import com.github.rwl.irbuilder.values.IValue;
import com.github.rwl.irbuilder.values.IntValue;
import com.github.rwl.irbuilder.values.LocalVariable;
import com.github.rwl.irbuilder.values.MetadataNode;
import com.github.rwl.irbuilder.values.MetadataString;
import com.github.rwl.irbuilder.values.NullValue;
import com.github.rwl.irbuilder.values.StringValue;
import com.github.rwl.irbuilder.values.StructValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.devtools.j2objc.J2ObjC;
import com.google.devtools.j2objc.J2ObjC.Language;
import com.google.devtools.j2objc.types.Types;
import com.google.devtools.j2objc.util.NameTable;
import com.google.devtools.j2objc.util.J2ObjCIRBuilder;

/**
 * Generates LLVM byte-code (.ll) files from compilation units.
 *
 * @author Richard Lincoln
 */
public class LLVMModuleGenerator extends ObjectiveCSourceFileGenerator {

  /**
   * Helper object that makes it easy to generate LLVM instructions.
   */
  private final J2ObjCIRBuilder irBuilder;

  /**
   * Keeps track of which values are defined in the current scope and what
   * their LLVM representation is.
   */
  private final Map<String, IValue> namedValues = Maps.newHashMap();

  private final List<IValue> llvmUsed = Lists.newArrayList();

  /**
   * Suffix for LLVM byte-code file
   */
  private final String suffix;

  private static final String OBJC_EMPTY_CACHE = "_objc_empty_cache";
  private static final String OBJC_EMPTY_VTABLE = "_objc_empty_vtable";

  private GlobalVariable objcEmptyCache = null;
  private GlobalVariable objcEmptyVTable = null;

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
    irBuilder = new J2ObjCIRBuilder(sourceFileName);
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

    if (llvmUsed.size() > 0) {
      irBuilder.global("llvm.used", new ArrayValue(new ArrayType(IntType
          .INT_8.pointerTo(), llvmUsed.size()), llvmUsed), Linkage.APPENDING,
          false, "llvm.metadata");
    }

    MetadataNode ver = irBuilder.metadataNode(new IntValue(1),
        new MetadataString("Objective-C Version"), new IntValue(2));
    MetadataNode imgVer = irBuilder.metadataNode(new IntValue(1),
        new MetadataString("Objective-C Image Info Version"), new IntValue(0));
    MetadataNode imgInfo = irBuilder.metadataNode(new IntValue(1),
        new MetadataString("Objective-C Image Info Section"),
        new MetadataString("__DATA, __objc_imageinfo, regular, no_dead_strip"));
    MetadataNode gc = irBuilder.metadataNode(new IntValue(4),
        new MetadataString("Objective-C Garbage Collection"), new IntValue(0));

    irBuilder.namedMetadata("llvm.module.flags", ver, imgVer, imgInfo, gc);

    save(unit);
  }

  @Override
  public void generate(TypeDeclaration node) {
    if (!node.isInterface()) {
      String superName = NameTable.getSuperClassName(node);

      GlobalVariable superMetaclassGlobal = irBuilder.global(String.format("\"OBJC_METACLASS_$_%s\"", superName),
          irBuilder.getClassT(), Linkage.EXTERNAL, false, null);

      String typeName = NameTable.getFullName(node);
      GlobalVariable classNameGlobal = irBuilder.global("\"\01L_OBJC_CLASS_NAME_\"",
          new StringValue(typeName), Linkage.INTERNAL, false,
          "__TEXT,__objc_classname,cstring_literals");

      GlobalVariable metaclassROGlobal = irBuilder.global(String.format("\"\01l_OBJC_METACLASS_RO_$_%s\"", typeName),
          new StructValue(new IValue[] {
              new IntValue(1),
              new IntValue(40),
              new IntValue(40),
              new NullValue(IntType.INT_8P),
              classNameGlobal,
              new NullValue(irBuilder.getMethodListT()),
              new NullValue(irBuilder.getObjcProtocolList()),
              new NullValue(irBuilder.getIVarListT()),
              new NullValue(irBuilder.getPropListT())
          }, irBuilder.getClassRoT()), Linkage.INTERNAL, false, "__DATA, __objc_const");

      GlobalVariable metaclassGlobal = irBuilder.global(String.format("\"OBJC_METACLASS_$_%s\"", typeName),
          new StructValue(new IValue[] {
              superMetaclassGlobal,
              superMetaclassGlobal,
              getObjcEmptyCache(),
              getObjcEmptyVTable(),
              metaclassROGlobal
          }, irBuilder.getClassT()), null, false, "__DATA, __objc_data");

      GlobalVariable superClassGlobal = irBuilder.global(String.format("\"OBJC_CLASS_$_%s\"", superName),
          irBuilder.getClassT(), Linkage.EXTERNAL, false, null);


      @SuppressWarnings("unchecked")
      List<Type> interfaces = node.superInterfaceTypes(); // safe by definition

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
    } else {
    }
  }

  private GlobalVariable getObjcEmptyCache() {
    if (objcEmptyCache == null) {
      objcEmptyCache = irBuilder.global(OBJC_EMPTY_CACHE,
          irBuilder.getObjcCache(), Linkage.EXTERNAL, false, null);
    }
    return objcEmptyCache;
  }

  private GlobalVariable getObjcEmptyVTable() {
    if (objcEmptyVTable == null) {
      IType funcType = new FunctionType(IntType.INT_8P,
          IntType.INT_8P, IntType.INT_8P).pointerTo();
      objcEmptyVTable = irBuilder.global(OBJC_EMPTY_VTABLE,
          funcType, Linkage.EXTERNAL, false, null);
    }
    return objcEmptyVTable;
  }

  @Override
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
    LocalVariable[] argVars = irBuilder.beginFunction("main",
        new FunctionType(IntType.INT_32, IntType.INT_32, IntType.INT_8PP),
        ImmutableList.<String>builder()
          .add("argc")
          .add("argv")
          .build(),
        ImmutableList.<AttrKind>builder()
          //.add(AttrKind.NO_UNWIND)
          .add(AttrKind.UWTABLE)
          .add(AttrKind.STACK_PROTECT)
          .build());
    LocalVariable i1 = irBuilder.alloca(IntType.INT_32, null, null);
    LocalVariable i2 = irBuilder.alloca(IntType.INT_32, null, null);
    LocalVariable i3 = irBuilder.alloca(IntType.INT_8PP, null, null);
    irBuilder.store(i1, new IntValue(0))
        .store(i2, argVars[0])
        .store(i3, argVars[1]);

    if (m != null) {
      @SuppressWarnings("unchecked")
      List<SingleVariableDeclaration> params = m.parameters();
      assert params.size() == 1;  // Previously checked in isMainMethod().
      printMethodBody(m, true);
    }

    irBuilder.endFunction(new IntValue(0));
  }

  private void printMethodBody(MethodDeclaration m, boolean isFunction)
      throws AssertionError {
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
    SSAGenerator.generate(stmt, irBuilder, namedValues, llvmUsed, asFunction);
  }

  @Override
  protected String methodDeclaration(MethodDeclaration m) {
    assert !m.isConstructor();
    boolean isStatic = Modifier.isStatic(m.getModifiers());
    IMethodBinding binding = Types.getMethodBinding(m);
    String methodName = NameTable.getName(binding);

    GlobalVariable methVarNameGlobal = irBuilder.global("\"\01L_OBJC_METH_VAR_NAME_\"",
        new StringValue(methodName), Linkage.INTERNAL, false,
        "__TEXT,__objc_methname,cstring_literals");

    GlobalVariable methVarTypeGlobal = buildMethodTypeEncoding(m);

//    String baseDeclaration = String.format("%c (%s)%s", isStatic ? '+' : '-',
//        NameTable.javaRefToObjC(m.getReturnType2()), methodName);

    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> params = m.parameters(); // safe by definition
//    parametersDeclaration(Types.getOriginalMethodBinding(binding), params, baseDeclaration, sb);
    return "";
  }

  private GlobalVariable buildMethodTypeEncoding(MethodDeclaration m) {
    int bytes = 16;

    @SuppressWarnings("unchecked")
    List<SingleVariableDeclaration> params = m.parameters(); // safe by definition
    String paramEnc = "";
    for (SingleVariableDeclaration param : params) {
      paramEnc += NameTable.typeEncoding(param.getType());
      int size = NameTable.encodingSize(param.getType());
      paramEnc += String.valueOf(size);
      bytes += size;
    }

    String encoding = String.format("%s%d@0:8%s",
        NameTable.typeEncoding(m.getReturnType2()), bytes, paramEnc);

    return irBuilder.global("\"\\01L_OBJC_METH_VAR_TYPE_\"", new StringValue(encoding),
        Linkage.INTERNAL, false, "__TEXT,__objc_methtype,cstring_literals");
  }

}
