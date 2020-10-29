@JS("shaka")
library shaka;

import 'package:js/js.dart';

@JS()
class polyfill{
  @JS()
  external static void installAll();
}