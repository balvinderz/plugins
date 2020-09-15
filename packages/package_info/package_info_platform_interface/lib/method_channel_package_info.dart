import 'package:flutter/services.dart';
import 'package:meta/meta.dart';

import 'package_info_platform_interface.dart';

// ignore: public_member_api_docs
class MethodChannelPackageInfo extends PackageInfoPlatform
{
  @visibleForTesting
  // ignore: public_member_api_docs
  MethodChannel channel = MethodChannel('plugins.flutter.io/package_info');

  Future<void> fromPlatform() async{
    return (await channel.invokeMapMethod<String,dynamic>('getAll'));

  }
}