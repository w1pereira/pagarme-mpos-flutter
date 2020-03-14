import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pagarme_mpos_flutter/pagarme_mpos_flutter.dart' as mpos;

void main() {
  const MethodChannel channel = MethodChannel('pagarme_mpos_flutter');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await mpos.platformVersion, '42');
  });
}
