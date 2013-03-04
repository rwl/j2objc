llvm-as Main.ll

llc Main.bc

clang -framework Foundation Main.s -o main
