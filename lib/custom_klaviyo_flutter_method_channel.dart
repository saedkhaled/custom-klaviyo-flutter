import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'custom_klaviyo_flutter_platform_interface.dart';

/// An implementation of [CustomKlaviyoFlutterPlatform] that uses method channels.
class MethodChannelCustomKlaviyoFlutter extends CustomKlaviyoFlutterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('custom_klaviyo_flutter');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
