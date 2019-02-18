#import "PspdfkitPlugin.h"
#import <pspdfkit_flutter/pspdfkit_flutter-Swift.h>

@implementation PspdfkitPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftPspdfkitFlutterPlugin registerWithRegistrar:registrar];
}
@end
