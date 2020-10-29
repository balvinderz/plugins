@JS("shaka.ui")
library shaka;

import 'dart:html';

import 'package:js/js.dart';
import 'package:video_player_shaka_player/shaka/src/player.dart';
@JS()
class Overlay
{
  external factory Overlay(Player player,Element element,VideoElement videoElement);

}