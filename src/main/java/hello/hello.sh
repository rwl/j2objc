#clang -o hello hello.m -I `gnustep-config --variable=GNUSTEP_SYSTEM_HEADERS` \
#	-L `gnustep-config --variable=GNUSTEP_SYSTEM_LIBRARIES` \
#	-lgnustep-base -fconstant-string-class=NSConstantString \
#	-D_NATIVE_OBJC_EXCEPTIONS -lobjc

#clang -o hello.ll hello.m -S -emit-llvm \
#	-I `gnustep-config --variable=GNUSTEP_SYSTEM_HEADERS` \
#       -L `gnustep-config --variable=GNUSTEP_SYSTEM_LIBRARIES` \
#        -lgnustep-base -fconstant-string-class=NSConstantString \
#        -D_NATIVE_OBJC_EXCEPTIONS -lobjc

clang `gnustep-config --objc-flags` -S -emit-llvm -O0 hello.m -o hello.ll
# -g -I /usr/include/GNUstep/

llvm-as-3.1 hello.ll
#llvm-extract-3.0 --func main hello.ll -o hello.bc
#llvm-dis-3.0 hello.bc

llc-3.1 hello.bc
gcc hello.s -o hello -lobjc -L `gnustep-config --variable=GNUSTEP_SYSTEM_LIBRARIES` -lgnustep-base

