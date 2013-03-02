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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class NamedTypes {

  private final IRBuilder irBuilder;

  public final Map<String, NamedType> namedTypes = Maps.newHashMap();

  public static final String OPAQUE_TYPE_NAME = "opq";
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

  public NamedTypes(IRBuilder irBuilder) {
    this.irBuilder = irBuilder;
  }

  public NamedType getOpaqueType() {
    NamedType opaque = namedTypes.get(NamedTypes.OPAQUE_TYPE_NAME);
    if (opaque == null) {
      opaque = new NamedType(NamedTypes.OPAQUE_TYPE_NAME,
          OpaqueType.INSTANCE);
      namedTypes.put(NamedTypes.OPAQUE_TYPE_NAME, opaque);
      irBuilder.namedType(opaque);
    }
    return opaque;
  }

  public IType getStructNSConstString() {
    NamedType structNSConstString = namedTypes.get(NamedTypes
        .NS_CONSTANT_STRING_NAME);
    if (structNSConstString == null) {
      IType[] types = new IType[] {
          IntType.INT_32.pointerTo(),
          IntType.INT_32,
          IntType.INT_8.pointerTo(),
          IntType.INT_64
      };
      structNSConstString = new NamedType(NamedTypes
          .NS_CONSTANT_STRING_NAME, new StructType(types));
      namedTypes.put(NamedTypes.NS_CONSTANT_STRING_NAME, structNSConstString);
      irBuilder.namedType(structNSConstString);
    }
    return structNSConstString;
  }

  public NamedType getClassT() {
    NamedType classT = namedTypes.get(NamedTypes.CLASS_T_NAME);
    if (classT == null) {
      OpaqueType opaque = new OpaqueType();
      IType[] types = new IType[] {
          opaque.pointerTo(),
          opaque.pointerTo(),
          getObjcCache().pointerTo(),
          new FunctionType(IntType.INT_8.pointerTo(), Lists.<IType>newArrayList(
              IntType.INT_8.pointerTo(),
              IntType.INT_8.pointerTo())).pointerTo().pointerTo(),
          getClassRoT().pointerTo()
      };
      classT = new NamedType(NamedTypes.CLASS_T_NAME,
          new StructType(types).refineAbstractTypeTo(opaque));
      namedTypes.put(NamedTypes.CLASS_T_NAME, classT);
      irBuilder.namedType(classT);
    }
    return classT;
  }

  public NamedType getObjcCache() {
    NamedType objcCache = namedTypes.get(NamedTypes.OBJC_CACHE_NAME);
    if (objcCache == null) {
      objcCache = new NamedType(NamedTypes.OBJC_CACHE_NAME,
          OpaqueType.INSTANCE);
      namedTypes.put(NamedTypes.OBJC_CACHE_NAME, objcCache);
      irBuilder.namedType(objcCache);
    }
    return objcCache;
  }

  public NamedType getClassRoT() {
    NamedType classRoT = namedTypes.get(NamedTypes.CLASS_RO_T_NAME);
    if (classRoT == null) {
      IType[] types = new IType[] {
          IntType.INT_32,
          IntType.INT_32,
          IntType.INT_32,
          IntType.INT_8.pointerTo(),
          IntType.INT_8.pointerTo(),
          getMethodListT().pointerTo(),
          getObjcProtocolList().pointerTo(),
          getIVarListT().pointerTo(),
          getPropListT().pointerTo()
      };
      classRoT = new NamedType(NamedTypes.CLASS_RO_T_NAME,
          new StructType(types));
      namedTypes.put(NamedTypes.CLASS_RO_T_NAME, classRoT);
      irBuilder.namedType(classRoT);
    }
    return classRoT;
  }

  public NamedType getMethodListT() {
    NamedType methodListT = namedTypes.get(NamedTypes.METHOD_LIST_T_NAME);
    if (methodListT == null) {
      IType[] types = new IType[] {
          IntType.INT_32,
          IntType.INT_32,
          new ArrayType(getObjcMethod(), 0)
      };
      methodListT = new NamedType(NamedTypes.METHOD_LIST_T_NAME,
          new StructType(types));
      namedTypes.put(NamedTypes.METHOD_LIST_T_NAME, methodListT);
      irBuilder.namedType(methodListT);
    }
    return methodListT;
  }

  public NamedType getObjcMethod() {
    NamedType objcMethod = namedTypes.get(NamedTypes.OBJC_METHOD_NAME);
    if (objcMethod == null) {
      IType[] types = new IType[] {
          IntType.INT_8.pointerTo(),
          IntType.INT_8.pointerTo(),
          IntType.INT_8.pointerTo()
      };
      objcMethod = new NamedType(NamedTypes.OBJC_METHOD_NAME,
          new StructType(types));
      namedTypes.put(NamedTypes.OBJC_METHOD_NAME, objcMethod);
      irBuilder.namedType(objcMethod);
    }
    return objcMethod;
  }

  public NamedType getObjcProtocolList() {
    NamedType objcProtocolList = namedTypes.get(NamedTypes
        .OBJC_PROTOCOL_LIST_NAME);
    if (objcProtocolList == null) {
      IType[] types = new IType[] {
          IntType.INT_64,
          getProtocolT()
      };
      objcProtocolList = new NamedType(NamedTypes.OBJC_PROTOCOL_LIST_NAME,
          new StructType(types));
      namedTypes.put(NamedTypes.OBJC_PROTOCOL_LIST_NAME, objcProtocolList);
      irBuilder.namedType(objcProtocolList);
    }
    return objcProtocolList;
  }

  public NamedType getProtocolT() {
    NamedType protocolT = namedTypes.get(NamedTypes.PROTOCOL_T_NAME);
    if (protocolT == null) {
      IType[] types = new IType[] {
        IntType.INT_8.pointerTo(),
        IntType.INT_8.pointerTo(),
        getObjcProtocolList().pointerTo(),
        getMethodListT().pointerTo(),
        getMethodListT().pointerTo(),
        getMethodListT().pointerTo(),
        getMethodListT().pointerTo(),
        getPropListT().pointerTo(),
        IntType.INT_32,
        IntType.INT_32,
        IntType.INT_8.pointerTo().pointerTo()
      };
      protocolT = new NamedType(NamedTypes.PROP_T_NAME,
          new StructType(types));
      namedTypes.put(NamedTypes.PROTOCOL_T_NAME, protocolT);
      irBuilder.namedType(protocolT);
    }
    return protocolT;
  }

  public NamedType getPropListT() {
    NamedType propListT = namedTypes.get(NamedTypes.PROP_LIST_T_NAME);
    if (propListT == null) {
      IType[] types = new IType[] {
          IntType.INT_32,
          IntType.INT_32,
          new ArrayType(getPropT(), 0)
      };
      propListT = new NamedType(NamedTypes.PROP_LIST_T_NAME,
          new StructType(types));
      namedTypes.put(NamedTypes.PROP_LIST_T_NAME, propListT);
      irBuilder.namedType(propListT);
    }
    return propListT;
  }

  public NamedType getPropT() {
    NamedType propT = namedTypes.get(NamedTypes.PROP_T_NAME);
    if (propT == null) {
      IType[] types = new IType[] {
          IntType.INT_8.pointerTo(),
          IntType.INT_8.pointerTo()
      };
      propT = new NamedType(NamedTypes.PROP_T_NAME, new StructType(types));
      namedTypes.put(NamedTypes.PROP_T_NAME, propT);
      irBuilder.namedType(propT);
    }
    return propT;
  }

  public NamedType getIVarListT() {
    NamedType ivarListT = namedTypes.get(NamedTypes.IVAR_LIST_T_NAME);
    if (ivarListT == null) {
      IType[] types = new IType[] {
          IntType.INT_32,
          IntType.INT_32,
          namedTypes.get(NamedTypes.IVAR_T_NAME)
      };
      ivarListT = new NamedType(NamedTypes.IVAR_LIST_T_NAME,
          new StructType(types));
      namedTypes.put(NamedTypes.IVAR_LIST_T_NAME, ivarListT);
      irBuilder.namedType(ivarListT);
    }
    return ivarListT;
  }

  public NamedType getIVarT() {
    NamedType ivarT = namedTypes.get(NamedTypes.IVAR_T_NAME);
    if (ivarT == null) {
      IType[] types = new IType[] {
          IntType.INT_64.pointerTo(),
          IntType.INT_8.pointerTo(),
          IntType.INT_8.pointerTo(),
          IntType.INT_32,
          IntType.INT_32
      };
      ivarT = new NamedType(NamedTypes.IVAR_T_NAME, new StructType(types));
      namedTypes.put(NamedTypes.IVAR_T_NAME, ivarT);
      irBuilder.namedType(ivarT);
    }
    return ivarT;
  }

  public NamedType getMessageRef() {
    NamedType messageRef = namedTypes.get(NamedTypes.MESSAGE_REF_NAME);
    if (messageRef == null) {
      IType[] types = new IType[] {
          IntType.INT_8.pointerTo(),
          IntType.INT_8.pointerTo()
      };
      messageRef = new NamedType(NamedTypes.MESSAGE_REF_NAME,
          new StructType(types));
      namedTypes.put(NamedTypes.MESSAGE_REF_NAME, messageRef);
      irBuilder.namedType(messageRef);
    }
    return messageRef;
  }

}
