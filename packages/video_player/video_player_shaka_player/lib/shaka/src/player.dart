@JS("shaka")
library shaka;

import 'dart:html';

import 'package:js/js.dart';
class Player{
  @JS()
  external static bool isBrowserSupported();
  external factory Player(VideoElement element);

  @JS()
  external void load(String mediaUrl);

}