package com.divertap.flutter.flutterappodeal;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.util.Log;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.RewardedVideoCallbacks;
import com.appodeal.ads.NativeAd;
import com.appodeal.ads.NativeCallbacks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import java.util.Random;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterAppodealPlugin
 */
public class FlutterAppodealPlugin implements MethodCallHandler, RewardedVideoCallbacks,
  NativeCallbacks
{

  private final Registrar registrar;
  private final MethodChannel channel;

  private int nLengthAds = 0;
  private int nLastIndexAdded = 0;
  private ArrayList<NativeAd> listNativeAd;
  private NativeAd nNativeAdCurrent;

  private ArrayList<ViewGroup> listViewNativeAd;

  public FlutterAppodealPlugin(Registrar registrar, MethodChannel channel) {
    this.registrar = registrar;
    this.channel = channel;
  }

  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_appodeal");
    FlutterAppodealPlugin plugin = new FlutterAppodealPlugin(registrar, channel);
    channel.setMethodCallHandler(plugin);
    Appodeal.setRewardedVideoCallbacks(plugin);
    Appodeal.setNativeCallbacks(plugin);
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    Activity activity = registrar.activity();
    if (activity == null) {
      result.error("no_activity", "flutler_appodeal plugin requires a foreground activity", null);
      return;
    }
    switch (call.method){
      case "initialize": {
        String appKey = call.argument("appKey");
        List<Integer> types = call.argument("types");
        int type = Appodeal.NONE;
        for (int type2 : types) {
          type = type | this.appodealAdType(type2);
        }
        Appodeal.initialize(activity, appKey, type);
        result.success(Boolean.TRUE);
        break;
      }
      case "showInterstitial":{
        Appodeal.show(activity, Appodeal.INTERSTITIAL);
        result.success(Boolean.TRUE);
        break;
      }
      case "showRewardedVideo":{
        Appodeal.show(activity, Appodeal.REWARDED_VIDEO);
        result.success(Boolean.TRUE);
        break;
      }
      case "isLoaded":{
        int type = call.argument("type");
        int adType = this.appodealAdType(type);
        result.success(Appodeal.isLoaded(adType));
        break;
      }
      case "initNativeAd":{
        nLengthAds = call.argument("lengthAds");
        Log.d("initNativeAd", "Length setted: " + nLengthAds);
        listNativeAd = new ArrayList();
        listViewNativeAd = new ArrayList();

        Appodeal.cache(activity, Appodeal.NATIVE);
        result.success(Boolean.TRUE);
        break;
      }
      case "onClickNativeAd":{
        int indexView = call.argument("indexViewClick");
        if (indexView < listViewNativeAd.size()) {
          Log.d("OnClickNativeAd", listNativeAd.get(indexView).getTitle());
          listViewNativeAd.get(indexView).performClick();
        }
        break;
      }
      default:{
        result.notImplemented();
      }
    }
  }

  private int createViewNativeAd() {
    int index = listNativeAd.size();
    if (listNativeAd.size() < nLengthAds){
      listNativeAd.add(nNativeAdCurrent);
      listViewNativeAd.add(new LinearLayout(registrar.activity()));

      listNativeAd.get(index)
        .registerViewForInteraction(listViewNativeAd.get(index));
    }
//    else {
//      if (nLastIndexAdded == nLengthAds) {
//        nLastIndexAdded = 0;
//      }
//      index = nLastIndexAdded;
//      listNativeAd.set(nLastIndexAdded, nNativeAdCurrent);
//      listViewNativeAd.set(nLastIndexAdded, new LinearLayout(registrar.activity()));
//      nLastIndexAdded++;
//    }
    return index;
  }

  private int appodealAdType(int innerType) {
    switch (innerType) {
      case 0:
        return Appodeal.INTERSTITIAL;
      case 1:
        return Appodeal.NON_SKIPPABLE_VIDEO;
      case 2:
        return Appodeal.BANNER;
      case 3:
        return Appodeal.NATIVE;
      case 4:
        return Appodeal.REWARDED_VIDEO;
      case 5:
        return Appodeal.MREC;
      case 6:
        return Appodeal.NON_SKIPPABLE_VIDEO;
    }
    return Appodeal.INTERSTITIAL;
  }

  private Map<String, Object> argumentsMap(Object... args) {
    Map<String, Object> arguments = new HashMap();
    for (int i = 0; i < args.length; i += 2) arguments.put(args[i].toString(), args[i + 1]);
    return arguments;
  }

  // Appodeal Rewarded Video Callbacks
  @Override
  public void onRewardedVideoLoaded() {
    channel.invokeMethod("onRewardedVideoLoaded", argumentsMap());
  }

  @Override
  public void onRewardedVideoFailedToLoad() {
    channel.invokeMethod("onRewardedVideoFailedToLoad", argumentsMap());
  }

  @Override
  public void onRewardedVideoShown() {
    channel.invokeMethod("onRewardedVideoPresent", argumentsMap());
  }

  @Override
  public void onRewardedVideoFinished(int i, String s) {
    channel.invokeMethod("onRewardedVideoFinished", argumentsMap());
  }

  @Override
  public void onRewardedVideoClosed(boolean b) {
    channel.invokeMethod("onRewardedVideoWillDismiss", argumentsMap());
  }

  @Override
  public void onNativeLoaded() {
    Log.d("Appodeal", "onNativeLoaded");
    nNativeAdCurrent = Appodeal.getNativeAds(1).get(0);
    if (nNativeAdCurrent != null && nLengthAds > 0) {
      int index = createViewNativeAd();
      if (index != listNativeAd.size()) {
        channel.invokeMethod("onNativeLoaded", argumentsMap(
          "index", index, "title", nNativeAdCurrent.getTitle(),
          "description", nNativeAdCurrent.getDescription(),
          "rating", nNativeAdCurrent.getRating(),
          "callToAction", nNativeAdCurrent.getCallToAction(),
          "imageUrl", nNativeAdCurrent.getMainImageUrl()
        ));
      }
    }
  }
  @Override
  public void onNativeFailedToLoad() {
    Log.d("Appodeal", "onNativeFailedToLoad");
    channel.invokeMethod("onNativeFailedToLoad", argumentsMap());
  }
  @Override
  public void onNativeShown(NativeAd nativeAd) {
    Log.d("Appodeal", "onNativeShown");
    channel.invokeMethod("onNativeShown", argumentsMap());
  }
  @Override
  public void onNativeClicked(NativeAd nativeAd) {
    Log.d("Appodeal", "onNativeClicked");
    channel.invokeMethod("onNativeClicked", argumentsMap());
  }
}
