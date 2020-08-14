@JS()
library hls.js;

import 'dart:html';

import 'package:js/js.dart';
@JS("Hls.isSupported")
external bool isSupported();
@JS()
class Hls{
  external factory Hls();
  @JS()
  external void loadSource(String videoSrc);
  @JS()
  external void attachMedia(VideoElement video);
  @JS()
  external on(String event,Function callback);
  @JS()
  external List get audioTracks;
  @JS()
  external int get audioTrack;
  set audioTrack(int  audioTrack)
  {
    this.audioTrack = audioTrack;

  }


}