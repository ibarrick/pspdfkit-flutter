import 'dart:async';

import 'package:flutter/services.dart';
import 'simple_permissions.dart';

class Pspdfkit {
  static const MethodChannel _channel = const MethodChannel('pspdfkit');
  static const BasicMessageChannel messageChannel = const BasicMessageChannel("pspdfkit_messages", JSONMessageCodec());

  static Future<dynamic> get frameworkVersion =>
      _channel.invokeMethod('frameworkVersion');

  static Future<void> setLicenseKey(String licenseKey) =>
    _channel.invokeMethod('setLicenseKey', <String, dynamic>{'licenseKey': licenseKey});

  static Future<void> present(String document) =>
    _channel.invokeMethod('present', <String, dynamic>{'document': document});

  static Future<void> openPdfDocument({String name, String path}) =>
    _channel.invokeMethod('openPdfDocument', <String, dynamic>{'uri': path, 'name': name });

  static Future<void> fillPdfForm({String name, Map<String, dynamic> fields}) =>
    _channel.invokeMethod('fillPdfForm', <String, dynamic>{'name': name, 'fields': fields});

  static Future<void> flattenPdfForm(String name) =>
    _channel.invokeMethod('flattenPdfForm', <String, dynamic>{'name': name});

  static Future<void> printDocument({String uri}) =>
    _channel.invokeMethod('printDocument', <String, dynamic>{'uri': uri});

   static Future<void> mergePdfs({String outputPath, List<String> names}) =>
    _channel.invokeMethod('mergePdfs', <String, dynamic>{'outputPath': outputPath, 'names': names});

   static Future<bool> checkSignature({String name, String fieldName}) async {
     final bool ret = await _channel.invokeMethod('checkSignature',
         <String, dynamic>{'name': name, 'fieldName': fieldName});
     return ret;
   }

   static Future<void> savePdf({String name, String outputPath}) =>
    _channel.invokeMethod("savePdf", <String, dynamic>{'name': name, 'outputPath': outputPath});

   static Future<bool> checkPdf(name) async {
     final bool ret = await _channel.invokeMethod("checkPdf", <String, dynamic>{'name': name});
     return ret;
   }

   static Future<bool> flattenSignatures(String name) async {
     final bool ret = await _channel.invokeMethod(
         "flattenSignatures", <String, dynamic>{'name': name});
     return ret;
   }

   static Future<void> renameEstimateFields(String name) =>
    _channel.invokeMethod("renameEstimateFields", <String, dynamic> {'name': name});

   static Future<int> getPageCount(String name) async {
     final int ret = await _channel.invokeMethod("getPageCount", <String, dynamic>{'name': name});
     return ret;
   }

  static Future<void> clearFiles() =>
    _channel.invokeMethod("clearFiles");

  static Future<bool> checkWriteExternalStoragePermission() =>
    SimplePermissions.checkPermission(Permission.WriteExternalStorage);

  static Future<PermissionStatus> requestWriteExternalStoragePermission() =>
    SimplePermissions.requestPermission(Permission.WriteExternalStorage);
    
  static Future<bool> openSettings() =>
    SimplePermissions.openSettings();
}
