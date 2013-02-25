
llc-3.1 Main.bc

#lli-3.1 --load=/usr/lib/gcc/x86_64-linux-gnu/4.6/libobjc.so --load=/usr/lib/libgnustep-base.so Main.bc

gcc Main.s -o Main -g -lobjc -L `gnustep-config --variable=GNUSTEP_SYSTEM_LIBRARIES` -lgnustep-base
