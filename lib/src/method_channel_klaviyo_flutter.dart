import 'dart:developer';

import 'package:flutter/services.dart';

import 'klaviyo_flutter_platform_interface.dart';
import 'klaviyo_profile.dart';

const MethodChannel _channel = MethodChannel('klaviyo.saedk.dev/klaviyo');

/// An implementation of [KlaviyoFlutterPlatform] that uses method channels.
class MethodChannelKlaviyoFlutter extends KlaviyoFlutterPlatform {
  void Function(Map content)? _onNotification;
  void Function(Map content)? _onAppLaunch;

  @override
  Future<void> initialize(String apiKey) async {
    await _channel.invokeMethod('initialize', {
      'apiKey': apiKey,
    });
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onNotification') {
        _onNotification?.call(call.arguments);
      } else if (call.method == 'onAppLaunch') {
        _onAppLaunch?.call(call.arguments);
      }
    });
  }

  /// Assign new identifiers and attributes to the currently tracked profile once.
  @override
  Future<String> updateProfile(KlaviyoProfile profileModel) async {
    final resultMap = await _channel.invokeMethod<String>(
      'updateProfile',
      profileModel.toJson(),
    );

    return resultMap.toString();
  }

  @override
  Future<String> logEvent(String name, [Map<String, dynamic>? metaData]) async {
    final resultMap = await _channel
        .invokeMethod('logEvent', {'name': name, 'metaData': metaData});
    log('logEvent result: $resultMap');

    return resultMap.toString();
  }

  @override
  Future<void> sendTokenToKlaviyo(String token) async {
    assert(token.isNotEmpty);
    log('Start sending token to Klaviyo');
    await _channel.invokeMethod('sendTokenToKlaviyo', {'token': token});
  }

  @override
  Future<bool> handlePush(Map<String, dynamic> message) async {
    if (!message.values.every((item) => item is String)) {
      throw ArgumentError(
          'Klaviyo push messages can only have string values');
    }

    final result =
        await _channel.invokeMethod<bool>('handlePush', {'message': message});
    return result ?? false;
  }

  @override
  Future<String?> getExternalId() {
    return _channel.invokeMethod('getExternalId');
  }

  @override
  Future<void> resetProfile() => _channel.invokeMethod('resetProfile');

  @override
  Future<void> setEmail(String email) =>
      _channel.invokeMethod('setEmail', {'email': email});

  @override
  Future<String?> getEmail() => _channel.invokeMethod('getEmail');

  @override
  Future<void> setPhoneNumber(String phoneNumber) =>
      _channel.invokeMethod('setPhoneNumber', {'phoneNumber': phoneNumber});

  @override
  Future<String?> getPhoneNumber() => _channel.invokeMethod('getPhoneNumber');

  @override
  Future<void> setNotificationListener(void Function(Map content) onNotification) async {
    _onNotification = onNotification;
  }

  @override
  Future<void> setOnAppLaunchListener(void Function(Map content) onAppLaunch) async {
    _onAppLaunch = onAppLaunch;
  }
}
