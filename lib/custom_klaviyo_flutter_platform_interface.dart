import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'custom_klaviyo_flutter_method_channel.dart';

abstract class CustomKlaviyoFlutterPlatform extends PlatformInterface {
  /// Constructs a CustomKlaviyoFlutterPlatform.
  CustomKlaviyoFlutterPlatform() : super(token: _token);

  static final Object _token = Object();

  static CustomKlaviyoFlutterPlatform _instance = MethodChannelCustomKlaviyoFlutter();

  /// The default instance of [CustomKlaviyoFlutterPlatform] to use.
  ///
  /// Defaults to [MethodChannelCustomKlaviyoFlutter].
  static CustomKlaviyoFlutterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [CustomKlaviyoFlutterPlatform] when
  /// they register themselves.
  static set instance(CustomKlaviyoFlutterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
