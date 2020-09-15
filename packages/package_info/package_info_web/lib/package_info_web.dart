import 'dart:async';
import 'dart:convert';
import 'dart:html';

import 'package:flutter/services.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';
import 'package:http/http.dart';

// ignore: public_member_api_docs
class PackageInfoPlugin {
  static void registerWith(Registrar registrar) {
    final MethodChannel channel = MethodChannel(
        'plugins.flutter.io/package_info',
        const StandardMethodCodec(),
        registrar.messenger);
    final PackageInfoPlugin instance = PackageInfoPlugin();
    channel.setMethodCallHandler(instance.handleMethodCall);
  }

  Future<dynamic> handleMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'getAll':
        return getAll();
      default:
        throw PlatformException(
            code: 'Unimplemented',
            details: "The package_info plugin for web doesn't implement "
                "the method '${call.method}'");
    }
  }

  Future<Map<String, dynamic>> getAll() async {
    String url = window.location.protocol +
        "//" +
        window.location.hostname +
        ":" +
        window.location.port +
        "/version.json";
    print(url);

    final response = await get(url);
    if (response.statusCode == 200) {
      final versionMap = jsonDecode(response.body);
      return {
        "appName": versionMap['app_name'],
        "version": versionMap['version'],
        "buildNumber": versionMap['build_number']
      };
    } else {
      return {};
    }
  }
}
