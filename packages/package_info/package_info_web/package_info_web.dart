import 'dart:html';

import 'package:flutter/services.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';

import '../lib/package_info.dart';

class PackageInfoWeb{
  static void registerWith(Registrar registrar) {
    final MethodChannel channel = MethodChannel(
        'plugins.flutter.io/url_launcher',
        const StandardMethodCodec(),
        registrar.messenger);
    final PackageInfoWeb instance = PackageInfoWeb();
    channel.setMethodCallHandler(instance.handleMethodCall);
  }
  Future<dynamic> handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'getAll':
        return _fromPlatform();
      default:
        throw PlatformException(
            code: 'Unimplemented',
            details: "The package_info plugin for web doesn't implement "
                "the method '${call.method}'");
    }
  }

  Map<String,dynamic> getAll(){

  }

}