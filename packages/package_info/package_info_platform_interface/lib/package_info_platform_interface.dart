import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'method_channel_package_info.dart';
// ignore: public_member_api_docs
// ignore: public_member_api_docs
abstract class PackageInfoPlatform extends PlatformInterface{
  // ignore: public_member_api_docs
  PackageInfoPlatform() : super(token: _token);

  static final Object _token = Object();
  static PackageInfoPlatform _instance = MethodChannelPackageInfo();

  // ignore: public_member_api_docs
  static PackageInfoPlatform get instance => _instance;
  // ignore: public_member_api_docs
  static set instance(PackageInfoPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }
  // ignore: public_member_api_docs
  Future<void> fromPlatform()  {
    throw UnimplementedError('fromPlatform() has not been implemented');

  }
}