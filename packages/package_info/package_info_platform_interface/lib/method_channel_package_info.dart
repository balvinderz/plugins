import 'package:flutter/services.dart';

import 'package_info_platform_interface.dart';

const MethodChannel _channel = MethodChannel('plugins.flutter.io/package_info');
/// An implementation of [PackageInfoPlatform] that uses method channels.
class MethodChannelPackageInfo extends PackageInfoPlatform
{
  @override
  Future<Map<String,dynamic>> getAll() {
    return _channel.invokeMapMethod<String, dynamic>('getAll');
  }

}