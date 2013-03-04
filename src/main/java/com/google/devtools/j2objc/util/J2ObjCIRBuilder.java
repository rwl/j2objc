package com.google.devtools.j2objc.util;

import java.util.Map;

import com.github.rwl.irbuilder.IRBuilder;
import com.github.rwl.irbuilder.types.ArrayType;
import com.github.rwl.irbuilder.types.FunctionType;
import com.github.rwl.irbuilder.types.IType;
import com.github.rwl.irbuilder.types.IntType;
import com.github.rwl.irbuilder.types.NamedType;
import com.github.rwl.irbuilder.types.OpaqueType;
import com.github.rwl.irbuilder.types.StructType;
import com.github.rwl.irbuilder.types.VoidType;
import com.google.common.collect.Maps;

public class J2ObjCIRBuilder extends IRBuilder {

  public static final String CLASS_T_NAME = "struct._class_t";
  public static final String OBJC_CACHE_NAME = "struct._objc_cache";
  public static final String CLASS_RO_T_NAME = "struct._class_ro_t";
  public static final String METHOD_LIST_T_NAME = "struct.__method_list_t";
  public static final String OBJC_METHOD_NAME = "struct._objc_method";
  public static final String OBJC_PROTOCOL_LIST_NAME = "struct._objc_protocol_list";
  public static final String PROTOCOL_T_NAME = "struct._protocol_t";
  public static final String PROP_LIST_T_NAME = "struct._prop_list_t";
  public static final String PROP_T_NAME = "struct._prop_t";
  public static final String IVAR_LIST_T_NAME = "struct._ivar_list_t";
  public static final String IVAR_T_NAME = "struct._ivar_t";
  public static final String NS_CONSTANT_STRING_NAME = "struct.NSConstantString";
  public static final String MESSAGE_REF_NAME = "struct._message_ref_t";

  public final Map<String, NamedType> namedTypes = Maps.newHashMap();

  public final String opaqueTypeName;

  public J2ObjCIRBuilder(String moduleID) {
    super(moduleID);
    NamedType opaque = namedType(null, OpaqueType.INSTANCE);
    opaqueTypeName = opaque.getName();
    namedTypes.put(opaqueTypeName, opaque);
  }

  public NamedType getOpaqueType() {
    return namedTypes.get(opaqueTypeName);
  }

  public IType getStructNSConstString() {
    NamedType structNSConstString = namedTypes.get(J2ObjCIRBuilder
        .NS_CONSTANT_STRING_NAME);
    if (structNSConstString == null) {
      IType[] types = new IType[] {
          IntType.INT_32.pointerTo(),
          IntType.INT_32,
          IntType.INT_8P,
          IntType.INT_64
      };
      structNSConstString = new NamedType(J2ObjCIRBuilder
          .NS_CONSTANT_STRING_NAME, new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.NS_CONSTANT_STRING_NAME, structNSConstString);
      namedType(structNSConstString);
    }
    return structNSConstString;
  }

  public NamedType getClassT() {
    NamedType classT = namedTypes.get(J2ObjCIRBuilder.CLASS_T_NAME);
    if (classT == null) {
      OpaqueType opaque = new OpaqueType();
      IType int8p = IntType.INT_8P;
      IType[] types = new IType[] {
          opaque.pointerTo(),
          opaque.pointerTo(),
          getObjcCache().pointerTo(),
          new FunctionType(IntType.INT_8P, int8p,
              int8p).pointerTo().pointerTo(),
          getClassRoT().pointerTo()
      };
      StructType structType = new StructType(types);
      classT = new NamedType(J2ObjCIRBuilder.CLASS_T_NAME, structType);
      structType.refineAbstractTypeTo(opaque, classT);
      namedTypes.put(J2ObjCIRBuilder.CLASS_T_NAME, classT);
      namedType(classT);
    }
    return classT;
  }

  public NamedType getObjcCache() {
    NamedType objcCache = namedTypes.get(J2ObjCIRBuilder.OBJC_CACHE_NAME);
    if (objcCache == null) {
      objcCache = new NamedType(J2ObjCIRBuilder.OBJC_CACHE_NAME,
          OpaqueType.INSTANCE);
      namedTypes.put(J2ObjCIRBuilder.OBJC_CACHE_NAME, objcCache);
      namedType(objcCache);
    }
    return objcCache;
  }

  public NamedType getClassRoT() {
    NamedType classRoT = namedTypes.get(J2ObjCIRBuilder.CLASS_RO_T_NAME);
    if (classRoT == null) {
      IType[] types = new IType[] {
          IntType.INT_32,
          IntType.INT_32,
          IntType.INT_32,
          IntType.INT_8P,
          IntType.INT_8P,
          getMethodListT().pointerTo(),
          getObjcProtocolList().pointerTo(),
          getIVarListT().pointerTo(),
          IntType.INT_8P,
          getPropListT().pointerTo()
      };
      classRoT = new NamedType(J2ObjCIRBuilder.CLASS_RO_T_NAME,
          new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.CLASS_RO_T_NAME, classRoT);
      namedType(classRoT);
    }
    return classRoT;
  }

  public NamedType getMethodListT() {
    NamedType methodListT = namedTypes.get(J2ObjCIRBuilder.METHOD_LIST_T_NAME);
    if (methodListT == null) {
      IType[] types = new IType[] {
          IntType.INT_32,
          IntType.INT_32,
          new ArrayType(getObjcMethod(), 0)
      };
      methodListT = new NamedType(J2ObjCIRBuilder.METHOD_LIST_T_NAME,
          new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.METHOD_LIST_T_NAME, methodListT);
      namedType(methodListT);
    }
    return methodListT;
  }

  public NamedType getObjcMethod() {
    NamedType objcMethod = namedTypes.get(J2ObjCIRBuilder.OBJC_METHOD_NAME);
    if (objcMethod == null) {
      IType[] types = new IType[] {
          IntType.INT_8P,
          IntType.INT_8P,
          IntType.INT_8P
      };
      objcMethod = new NamedType(J2ObjCIRBuilder.OBJC_METHOD_NAME,
          new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.OBJC_METHOD_NAME, objcMethod);
      namedType(objcMethod);
    }
    return objcMethod;
  }

  public NamedType getObjcProtocolList() {
    NamedType objcProtocolList = namedTypes.get(J2ObjCIRBuilder
        .OBJC_PROTOCOL_LIST_NAME);
    if (objcProtocolList == null) {
      IType[] types = new IType[] {
          IntType.INT_64,
          VoidType.INSTANCE  // recursion hack
      };
      objcProtocolList = new NamedType(J2ObjCIRBuilder.OBJC_PROTOCOL_LIST_NAME,
          new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.OBJC_PROTOCOL_LIST_NAME, objcProtocolList);
      types[1] = new ArrayType(getProtocolT().pointerTo(), 0);
      namedType(objcProtocolList);
    }
    return objcProtocolList;
  }

  public NamedType getProtocolT() {
    NamedType protocolT = namedTypes.get(J2ObjCIRBuilder.PROTOCOL_T_NAME);
    if (protocolT == null) {
      IType[] types = new IType[] {
        IntType.INT_8P,
        IntType.INT_8P,
        VoidType.INSTANCE,  // recursion hack
        getMethodListT().pointerTo(),
        getMethodListT().pointerTo(),
        getMethodListT().pointerTo(),
        getMethodListT().pointerTo(),
        getPropListT().pointerTo(),
        IntType.INT_32,
        IntType.INT_32,
        IntType.INT_8P.pointerTo()
      };
      protocolT = new NamedType(J2ObjCIRBuilder.PROTOCOL_T_NAME,
          new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.PROTOCOL_T_NAME, protocolT);
      types[2] = getObjcProtocolList().pointerTo();
      namedType(protocolT);
    }
    return protocolT;
  }

  public NamedType getPropListT() {
    NamedType propListT = namedTypes.get(J2ObjCIRBuilder.PROP_LIST_T_NAME);
    if (propListT == null) {
      IType[] types = new IType[] {
          IntType.INT_32,
          IntType.INT_32,
          new ArrayType(getPropT(), 0)
      };
      propListT = new NamedType(J2ObjCIRBuilder.PROP_LIST_T_NAME,
          new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.PROP_LIST_T_NAME, propListT);
      namedType(propListT);
    }
    return propListT;
  }

  public NamedType getPropT() {
    NamedType propT = namedTypes.get(J2ObjCIRBuilder.PROP_T_NAME);
    if (propT == null) {
      IType[] types = new IType[] {
          IntType.INT_8P,
          IntType.INT_8P
      };
      propT = new NamedType(J2ObjCIRBuilder.PROP_T_NAME, new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.PROP_T_NAME, propT);
      namedType(propT);
    }
    return propT;
  }

  public NamedType getIVarListT() {
    NamedType ivarListT = namedTypes.get(J2ObjCIRBuilder.IVAR_LIST_T_NAME);
    if (ivarListT == null) {
      IType[] types = new IType[] {
          IntType.INT_32,
          IntType.INT_32,
          new ArrayType(getIVarT(), 0)
      };
      ivarListT = new NamedType(J2ObjCIRBuilder.IVAR_LIST_T_NAME,
          new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.IVAR_LIST_T_NAME, ivarListT);
      namedType(ivarListT);
    }
    return ivarListT;
  }

  public NamedType getIVarT() {
    NamedType ivarT = namedTypes.get(J2ObjCIRBuilder.IVAR_T_NAME);
    if (ivarT == null) {
      IType[] types = new IType[] {
          IntType.INT_64.pointerTo(),
          IntType.INT_8P,
          IntType.INT_8P,
          IntType.INT_32,
          IntType.INT_32
      };
      ivarT = new NamedType(J2ObjCIRBuilder.IVAR_T_NAME, new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.IVAR_T_NAME, ivarT);
      namedType(ivarT);
    }
    return ivarT;
  }

  public NamedType getMessageRef() {
    NamedType messageRef = namedTypes.get(J2ObjCIRBuilder.MESSAGE_REF_NAME);
    if (messageRef == null) {
      IType[] types = new IType[] {
          IntType.INT_8P,
          IntType.INT_8P
      };
      messageRef = new NamedType(J2ObjCIRBuilder.MESSAGE_REF_NAME,
          new StructType(types));
      namedTypes.put(J2ObjCIRBuilder.MESSAGE_REF_NAME, messageRef);
      namedType(messageRef);
    }
    return messageRef;
  }

}
