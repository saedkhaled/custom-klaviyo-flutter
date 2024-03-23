library custom_klaviyo_flutter;

import 'dart:async';

import 'package:custom_klaviyo_flutter/src/klaviyo_flutter_platform_interface.dart';
import 'package:custom_klaviyo_flutter/src/klaviyo_profile.dart';

export 'custom_klaviyo_flutter.dart';
export 'src/klaviyo_profile.dart';

class Klaviyo {
  /// private constructor to not allow the object creation from outside.
  Klaviyo._();

  static final Klaviyo _instance = Klaviyo._();

  bool _initialized = false;

  /// get the instance of the [Klaviyo].
  static Klaviyo get instance => _instance;

  /// Function to initialize the Klaviyo SDK.
  ///
  /// First, you'll need to get your Klaviyo [apiKey] public API key for your Klaviyo account.
  ///
  /// You can get these from Klaviyo settings:
  /// * [public API key](https://www.klaviyo.com/settings/account/api-keys)
  ///
  /// Then, initialize Klaviyo in main method.
  Future<void> initialize(String apiKey) async {
    await KlaviyoFlutterPlatform.instance.initialize(apiKey);
    _initialized = true;
  }

  /// To log events in Klaviyo that record what users do in your app and when they do it.
  /// For example, you can record when user opened a specific screen in your app.
  /// You can also pass [metaData] about the event.
  Future<String> logEvent(String name, [Map<String, dynamic>? metaData]) {
    return KlaviyoFlutterPlatform.instance.logEvent(name, metaData);
  }

  /// The [token] to send to the Klaviyo to receive the notifications.
  ///
  /// For the Android, this [token] must be a FCM (Firebase cloud messaging) token.
  /// For the iOS, this [token] must be a APNS token.
  Future<void> sendTokenToKlaviyo(String token) {
    return KlaviyoFlutterPlatform.instance.sendTokenToKlaviyo(token);
  }

  /// Assign new identifiers and attributes to the currently tracked profile.
  /// If a profile has already been identified it will be overwritten by calling [resetProfile].
  ///
  /// The SDK keeps track of current profile details to
  /// build analytics requests with profile identifiers
  ///
  /// @param [profileMap] A map-like object representing properties of the new user
  /// @return Returns Future<String> success when called on Android or iOS
  ///
  /// All profile attributes recognised by the Klaviyo APIs [com.klaviyo.analytics.model.ProfileKey]
  Future<String> updateProfile(KlaviyoProfile profileModel) async {
    return KlaviyoFlutterPlatform.instance.updateProfile(profileModel);
  }

  /// Check if the push [message] is for Klaviyo and handle that push.
  Future<bool> handlePush(Map<String, dynamic> message) async {
    return KlaviyoFlutterPlatform.instance.handlePush(message);
  }

  /// Check if the Klaviyo already initialized
  bool get isInitialized => _initialized;

  /// Check if the push [message] is for Klaviyo
  bool isKlaviyoPush(Map<String, dynamic> message) => message.containsKey('_k');

  /// @return The external ID of the currently tracked profile, if set
  Future<String?> getExternalId() =>
      KlaviyoFlutterPlatform.instance.getExternalId();

  /// Clears all stored profile identifiers (e.g. email or phone) and starts a new tracked profile
  ///
  /// NOTE: if a push token was registered to the current profile, you will need to
  /// call `setPushToken` again to associate this device to a new profile
  ///
  /// This should be called whenever an active user in your app is removed
  /// (e.g. after a logout)
  Future<void> resetProfile() => KlaviyoFlutterPlatform.instance.resetProfile();

  /// Assigns an email address to the currently tracked Klaviyo profile
  ///
  /// The SDK keeps track of current profile details to
  /// build analytics requests with profile identifiers
  ///
  /// This should be called whenever the active user in your app changes
  /// (e.g. after a fresh login)
  ///
  /// @param [email] Email address for active user
  Future<void> setEmail(String email) =>
      KlaviyoFlutterPlatform.instance.setEmail(email);

  /// @return The email of the currently tracked profile, if set
  Future<String?> getEmail() => KlaviyoFlutterPlatform.instance.getEmail();

  /// Assigns a phone number to the currently tracked Klaviyo profile
  ///
  /// NOTE: Phone number format is not validated, but should conform to Klaviyo formatting
  /// see (documentation)[https://help.klaviyo.com/hc/en-us/articles/360046055671-Accepted-phone-number-formats-for-SMS-in-Klaviyo]
  ///
  /// The SDK keeps track of current profile details to
  /// build analytics requests with profile identifiers
  ///
  /// This should be called whenever the active user in your app changes
  /// (e.g. after a fresh login)
  ///
  /// @param [phoneNumber] Phone number for active user
  Future<void> setPhoneNumber(String phoneNumber) =>
      KlaviyoFlutterPlatform.instance.setPhoneNumber(phoneNumber);

  /// @return The phone number of the currently tracked profile, if set
  Future<String?> getPhoneNumber() =>
      KlaviyoFlutterPlatform.instance.getPhoneNumber();

  /// Set the notification listener to listen to the notification events
/// @param [onNotification] The callback to listen to the notification events
  Future<void> setNotificationListener(void Function(Map content) onNotification) =>
      KlaviyoFlutterPlatform.instance.setNotificationListener(onNotification);

  /// Set the on app launch listener to listen to the app launch events
/// @param [onAppLaunch] The callback to listen to the app launch events
  Future<void> setOnAppLaunchListener(void Function() onAppLaunch) =>
      KlaviyoFlutterPlatform.instance.setOnAppLaunchListener(onAppLaunch);
}