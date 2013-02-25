/*
#include <stdio.h>

int main(int n, char** argv) {
  printf("Hello world\n");
  return 0;
}
*/

#import <Foundation/Foundation.h>
#import <Foundation/NSObjCRuntime.h>
// #import <Foundation/NSAutoreleasePool.h>
// #import <Foundation/NSObject.h>
// #import <Foundation/NSString.h>

int main(int argc, char** argv) {
//int main (void) {
//  NSString *str = [NSString stringWithFormat:@"%@", "foo"];
//  NSString *str2 = [NSString stringWithFormat:@"bar"];
//  [NSObject init];
/*
  NSString *str = @"foo";
  NSString *str2 = @"bar";
  str = @"baz";
*/

//  NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
    NSLog(@"%@", @"Hello World!");
//    NSMakeRect(0,0,10,10);
//  [pool drain];
  return 0;
}
