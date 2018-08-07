import 'dart:async';
import 'package:flutter/services.dart';

enum AppodealAdType {
  AppodealAdTypeInterstitial,
  AppodealAdTypeSkippableVideo,
  AppodealAdTypeBanner,
  AppodealAdTypeNativeAd,
  AppodealAdTypeRewardedVideo,
  AppodealAdTypeMREC,
  AppodealAdTypeNonSkippableVideo,
}

enum RewardedVideoAdEvent {
  loaded,
  failedToLoad,
  present,
  willDismiss,
  finish,
}

enum NativeAdEvent {
  loaded,
  failedToLoad,
  shown,
  clicked,
}

typedef void RewardedVideoAdListener(RewardedVideoAdEvent event,
    {String rewardType, int rewardAmount});

typedef void NativeAdListener(NativeAdEvent event,{
  int index, String title, String description,
  double rating, String callToAction, String imageUrl
});

typedef void GetNativeAdListener({
  int index, String title, String description,
  double rating, String callToAction, String imageUrl
});

class FlutterAppodeal {

  bool shouldCallListener;
  bool enableNativeListener;
  bool enableNativeAd;

  final MethodChannel _channel;

  /// Called when the status of the video ad changes.
  RewardedVideoAdListener videoListener;

  /// Called when status of NativeAd change
  NativeAdListener nativeAdListener;

  GetNativeAdListener getNativeAdListener;

  static const Map<String, RewardedVideoAdEvent> _methodToRewardedVideoAdEvent =
  const <String, RewardedVideoAdEvent>{
    'onRewardedVideoLoaded': RewardedVideoAdEvent.loaded,
    'onRewardedVideoFailedToLoad': RewardedVideoAdEvent.failedToLoad,
    'onRewardedVideoPresent': RewardedVideoAdEvent.present,
    'onRewardedVideoWillDismiss': RewardedVideoAdEvent.willDismiss,
    'onRewardedVideoFinished': RewardedVideoAdEvent.finish,
  };

  static const Map<String, NativeAdEvent> _methodToNativeAdEvent =
  const <String, NativeAdEvent>{
    'onNativeLoaded': NativeAdEvent.loaded,
    'onNativeFailedToLoad': NativeAdEvent.failedToLoad,
    'onNativeShown': NativeAdEvent.shown,
    'onNativeClicked': NativeAdEvent.clicked,
  };

  static final FlutterAppodeal _instance = new FlutterAppodeal.private(
    const MethodChannel('flutter_appodeal'),
  );

  FlutterAppodeal.private(MethodChannel channel) : _channel = channel {
    _channel.setMethodCallHandler(_handleMethodRewardedVideo);
    _channel.setMethodCallHandler(_handleMethodNativeAd);
  }

  static FlutterAppodeal get instance => _instance;

  Future initialize(
    String appKey,
    List<AppodealAdType> types,
  ) async {
    shouldCallListener = false;
    List<int> itypes = new List<int>();
    for (final type in types) {
      itypes.add(type.index);
    }
    _channel.invokeMethod('initialize', <String, dynamic>{
      'appKey': appKey,
      'types': itypes,
    });
  }

  /*
    Shows an Interstitial in the root view controller or main activity
   */
  Future showInterstitial() async {
    shouldCallListener = false;
    _channel.invokeMethod('showInterstitial');
  }

  /*
    Shows an Rewarded Video in the root view controller or main activity
   */
  Future showRewardedVideo() async {
    shouldCallListener = true;
    _channel.invokeMethod('showRewardedVideo');
  }

  /*
  Shows an Native Ads
   */
  Future initNativeAd(int lengthAds) async{
    enableNativeListener = true;
    _channel.invokeMethod('initNativeAd', <String, dynamic>{
      'lengthAds': lengthAds
    });
  }

  Future getNativeAd() async{
    enableNativeAd = true;
    _channel.invokeMethod('getNativeAd');
  }

  Future nativeAdClick(int indexViewClick) async{
    enableNativeListener = true;
    _channel.invokeMethod('onClickNativeAd', <String, dynamic>{
      'indexViewClick': indexViewClick
    });
  }

  Future<bool> isLoaded(AppodealAdType type) async {
    shouldCallListener = false;
    final bool result = await _channel
        .invokeMethod('isLoaded', <String, dynamic>{'type': type.index});
    return result;
  }

  Future<dynamic> _handleMethodRewardedVideo(MethodCall call) {
    final Map<dynamic, dynamic> argumentsMap = call.arguments;
    final RewardedVideoAdEvent rewardedEvent =
    _methodToRewardedVideoAdEvent[call.method];
    if (rewardedEvent != null && shouldCallListener) {
      if (this.videoListener != null) {
        if (rewardedEvent == RewardedVideoAdEvent.finish && argumentsMap != null) {
          this.videoListener(rewardedEvent,
              rewardType: argumentsMap['rewardType'],
              rewardAmount: argumentsMap['rewardAmount']);
        } else {
          this.videoListener(rewardedEvent);
        }
      }
    }

    return new Future<Null>(null);
  }

  Future<dynamic> _handleMethodNativeAd(MethodCall call) {
    final Map<dynamic, dynamic> argumentsMap = call.arguments;
    final NativeAdEvent nativeAdEvent = _methodToNativeAdEvent[call.method];
    if (nativeAdEvent != null && enableNativeListener) {
      if (this.nativeAdListener != null) {
        if (argumentsMap != null) {
          this.nativeAdListener(nativeAdEvent,
              index: argumentsMap["index"], title: argumentsMap["title"],
              description: argumentsMap["description"], rating: argumentsMap["rating"],
              callToAction: argumentsMap["callToAction"], imageUrl: argumentsMap["imageUrl"]
          );
        }else{
          this.nativeAdListener(nativeAdEvent);
        }
      }
    }else if (call.method == "getNativeAd" && enableNativeAd) {
      if (this.getNativeAdListener != null) {
        if (argumentsMap != null) {
          this.getNativeAdListener(
              index: argumentsMap["index"], title: argumentsMap["title"],
              description: argumentsMap["description"], rating: argumentsMap["rating"],
              callToAction: argumentsMap["callToAction"], imageUrl: argumentsMap["imageUrl"]
          );
        }else{
          this.getNativeAdListener();
        }
      }
    }else{
      print("No Apply: " + call.method);
    }

    return new Future<Null>(null);
  }
}
