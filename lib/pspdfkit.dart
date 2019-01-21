import 'dart:async';

import 'package:flutter/services.dart';
import 'simple_permissions.dart';

class Pspdfkit {
  static const MethodChannel _channel = const MethodChannel('pspdfkit');

  static Future<dynamic> get frameworkVersion =>
      _channel.invokeMethod('frameworkVersion');

  static Future<void> setLicenseKey(String licenseKey) =>
    _channel.invokeMethod('setLicenseKey', <String, dynamic>{'licenseKey': licenseKey});

  static Future<void> present(String document) =>
    _channel.invokeMethod('present', <String, dynamic>{'document': document});

  static Future<void> openPdfDocument({String name, String path}) =>
    _channel.invokeMethod('openPdfDocument', <String, dynamic>{'uri': path, 'name': name });

  static Future<void> fillPdfForm({String name, Map<String,String> fields}) =>
    _channel.invokeMethod('fillPdfForm', <String, dynamic>{'name': name, 'fields': fields});

  static Future<void> flattenPdfForm({String name}) =>
    _channel.invokeMethod('flattenPdfForm', <String, dynamic>{'name': name});

   static Future<void> mergePdfs({String name, List<String> names}) =>
    _channel.invokeMethod('flattenPdfForm', <String, dynamic>{'name': name, 'names': names});

  static Future<bool> checkWriteExternalStoragePermission() =>
    SimplePermissions.checkPermission(Permission.WriteExternalStorage);

  static Future<PermissionStatus> requestWriteExternalStoragePermission() =>
    SimplePermissions.requestPermission(Permission.WriteExternalStorage);
    
  static Future<bool> openSettings() =>
    SimplePermissions.openSettings();
}
