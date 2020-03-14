import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:pagarme_mpos_flutter/pagarme_mpos_flutter.dart';
import 'package:pagarme_mpos_flutter_example/device.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  String _platformVersion = 'Unknown';
  PagarmeMpos mpos = new PagarmeMpos();

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await mpos.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }
    
    // Device initialization
    Device device = new Device(
      deviceName: '...', // Device Name
      amount: 1000,
      paymentMethod: PaymentMethod.CreditCard,
      mpos: mpos
    );

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Mpos Example App'),
        ),
        body: Center(
            child: Padding(
          padding: EdgeInsets.all(20.0),
          child: Column(
            children: <Widget>[
              Text('Running on: $_platformVersion\n'),
              StreamBuilder(
                  stream: mpos.events,
                  builder: (context, snapshot) {
                    if (snapshot.hasData) {
                      return Text('Last mpos event: ${snapshot.data}');
                    }
                    return Text('No data');
                  }),
                  const RaisedButton(
          onPressed: null,
          child: Text(
            'Disabled Button',
            style: TextStyle(fontSize: 20)
          ),
        ),
            ],
          ),
        )),
      ),
    );
  }
}
