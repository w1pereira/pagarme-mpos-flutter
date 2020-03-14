import 'dart:convert';

import 'package:pagarme_mpos_flutter/pagarme_mpos_flutter.dart';
import 'transaction.dart' as transaction;

class Device {
  // DO NOT KEEP YOUR SECRETS IN PLAIN TEXT.
  //
  // Please, note this was meant to be a simple example not a real world application, and
  // the following object just helps keeping secrets together.
  //
  // This is NOT a pattern to be followed, NEVER DO THIS.
  //
  // In a real world app, consider using keychains or compile-time obfuscators (eg.: cocoapods-keys)
  // to store secrets.
  String apiKey = '';
  String encryptionKey = '';

  PagarmeMpos mpos;
  String transactionStatus;
  int amount;
  PaymentMethod paymentMethod;

  String deviceName;

  Device({this.deviceName, this.amount, this.paymentMethod, this.mpos}) {
    enableListeners();
    mpos.createMpos(this.deviceName, this.encryptionKey);

    mpos.events.listen((data) => {print(data)});
    mpos.openConnection(true);
  }

  void enableListeners() {
    mpos.events.listen((data) => this.addListeners(data));
  }

  void addListeners(data) {
    if (data != null) {
      if (data['method'] == 'onBluetoothConnected') {
        mpos.initialize();
        return;
      }

      if (data['method'] == 'onBluetoothDisconnected') {
        setTransactionStatus('Lost bluetooth connection...');
        return;
      }

      if (data['method'] == 'onBluetoothErrored') {
        setTransactionStatus('An error ocurred ${data['value']}');
        return;
      }

      if (data['method'] == 'onReceiveInitialization') {
        mpos.downloadEmvTablesToDevice(false);
        setTransactionStatus('Checking for emv table updates...');
        mpos.displayText('CHECKING UPDATES...');
        return;
      }

      if (data['method'] == 'onReceiveNotification') {
        print('[${this.deviceName}] Sent notification: ${data['value']}');
        return;
      }

      if (data['method'] == 'onReceiveTableUpdated') {
        setTransactionStatus('Emv tables are up to date. Insert card...');
        mpos.payAmount(this.amount, this.paymentMethod);
        return;
      }

      if (data['method'] == 'onReceiveCardHash') {
        mpos.displayText('PROCESSING...');
        setTransactionStatus('Received card hash. Creating transaction...');
        createTransaction(data['value']);
        return;
      }

      if (data['method'] == 'onReceiveError') {
        setTransactionStatus(null);
        print('ERROR: An error ocurred: ' + data['value']);
        mpos.close('ERROR - ' + data['value']);
      }

      if (data['method'] == 'onReceiveClose') {
        // TBD
        setTransactionStatus('onReceiveClose');
      }

      if (data['method'] == 'onReceiveOperationCancelled') {
        // TBD
        setTransactionStatus('onReceiveOperationCancelled');
      }

      if (data['method'] == 'onReceiveOperationCompleted') {
        // TBD
        setTransactionStatus('onReceiveOperationCompleted');
      }

      if (data['method'] == 'onReceiveFinishTransaction') {
        setTransactionStatus('onReceiveFinishTransaction');
        //mpos.displayText("TRANSAÇÃO CONCLUÍDA")
        mpos.close("RETIRE O CARTAO");
      }
    }
  }

  void createTransaction(String jsonResult) async {
    dynamic result = json.decode(jsonResult);
    try {
      dynamic mposTransaction = await transaction.createTransaction({
        'amount': amount.toString(),
        'api_key': this.apiKey,
        'card_hash': result['cardHash']
      });
      onTransactionSuccess(
          mposTransaction, result['shouldFinishTransaction'] == 'true');
    } catch (error) {
      onTransactionError(result['shouldFinishTransaction'] == 'true');
      setTransactionStatus(null);
    }
  }

  void setTransactionStatus(String status) {
    this.transactionStatus = status;
    print(status);
  }

  void onTransactionSuccess(transaction, bool shouldFinishTransaction) {
    if (shouldFinishTransaction) {
      mpos.finishTransaction(
          true,
          int.parse(transaction['acquirer_response_code']),
          transaction['card_emv_response']);
    } else {
      mpos.close('PAYMENT ACCEPTED');
      print('Success. Payment accepted.');
    }
  }

  void onTransactionError(bool shouldFinishTransaction) {
    if (shouldFinishTransaction) {
      mpos.finishTransaction(false, 0, null);
    } else {
      mpos.close('PAYMENT REFUSED');
      print('Failure. Payment refused.');
    }

    setTransactionStatus(null);
  }
}
