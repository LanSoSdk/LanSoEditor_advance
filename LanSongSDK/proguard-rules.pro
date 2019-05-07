# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile



#com.lansosdk.videoeitor包:
#AVDecoder
#AVEncoder
#LanSoEditor
#MediaInfo
#VideoEditor
#
#com.lansosdk.videoplayer包:
#VideoPlayer
#jar包的全部;


#-keep public class com.lansosdk.videoeitor.AVDecoder {
#  <fields>;
#  <methods>;
#}
#-keep public class com.lansosdk.videoeitor.AVEncoder {
#  <fields>;
#  <methods>;
#}
#-keep public class com.lansosdk.videoeitor.LanSoEditor {
#  <fields>;
#  <methods>;
#}
#-keep public class com.lansosdk.videoeitor.MediaInfo {
#  <fields>;
#  <methods>;
#}
#-keep public class com.lansosdk.videoeitor.VideoEditor {
#  <fields>;
#  <methods>;
#}
#-keep public class com.lansosdk.videoplayer.VideoPlayer {
#  <fields>;
#  <methods>;
#}
#-keep public class com.lansosdk.box..**{*;
#}
