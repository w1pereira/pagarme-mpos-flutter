import 'dart:async';

import 'package:flutter/services.dart';

class PagarmeMpos {

  MethodChannel _channel = const MethodChannel('pagarme_mpos_flutter');
  EventChannel _eventChannel = const EventChannel('mpos_stream');
  Stream<dynamic> _mposEventStream;

  final Map<PaymentMethod, List<String>> cardBrandsByPaymentMethod = {
    PaymentMethod.CreditCard: [
      'american express',
      'amex',
      'aura',
      'china union pay',
      'diners',
      'discover',
      'elo',
      'hiper',
      'hipercard',
      'jcb',
      'mastercard',
      'rupay',
      'visa',
    ],
    PaymentMethod.DebitCard: [
      'china union pay',
      'dankort',
      'elo',
      'hiper',
      'jcb',
      'laser',
      'maestro',
      'mastercard',
      'rupay',
      'solo',
      'switch',
      'visa',
    ],
  };

  Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  Future<bool> createMpos(String deviceName, String encryptionKey) async {
    final bool result = await _channel.invokeMethod('createMpos',
        {'deviceName': deviceName, 'encryptionKey': encryptionKey});
    return result;
  }

  Future<bool> initialize() async {
    final bool result = await _channel.invokeMethod('initialize');
    return result;
  }

  Future<bool> downloadEmvTablesToDevice(bool forceUpdate) async {
    final bool result = await _channel.invokeMethod(
        'downloadEmvTablesToDevice', {'forceUpdate': forceUpdate});
    return result;
  }

  Future<bool> payAmount(int amount, PaymentMethod paymentMethod) async {
    List<String> cardBrandList = cardBrandsByPaymentMethod[paymentMethod];
    final bool result = await _channel.invokeMethod('payAmount', {
      'amount': amount,
      'cardBrandList': cardBrandList,
      'paymentMethod': getPaymentMethodIdentifier(paymentMethod)
    });
    return result;
  }

  Future<bool> close(String message) async {
    final bool result =
        await _channel.invokeMethod('close', { 'message': message });
    return result;
  }

  Future<bool> closeConnection() async {
    final bool result = await _channel.invokeMethod('closeConnection');
    return result;
  }

  Future<bool> finishTransaction(
      bool connected, int responseCode, String emvData) async {
    final bool result = await _channel.invokeMethod('finishTransaction', {
      'connected': connected,
      'responseCode': responseCode,
      'emvData': emvData
    });
    return result;
  }

  Future<bool> openConnection(bool secure) async {
    final bool result =
        await _channel.invokeMethod('openConnection', {'secure': secure});
    return result;
  }

  Future<bool> displayText(String message) async {
    final bool result =
        await _channel.invokeMethod('displayText', {'message': message});
    return result;
  }

  Stream<dynamic> get events {
    if (_mposEventStream == null) {
      _mposEventStream =
          _eventChannel.receiveBroadcastStream().map<dynamic>((value) => value);
    }
    return _mposEventStream;
  }

  int getPaymentMethodIdentifier(PaymentMethod paymentMethod) {
    switch (paymentMethod) {
      case PaymentMethod.CreditCard:
        return 1;
      case PaymentMethod.DebitCard:
        return 2;
      default:
        return null;
    }
  }

}

enum PaymentMethod { CreditCard, DebitCard }