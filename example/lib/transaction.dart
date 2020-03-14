import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

dynamic createTransaction(Map<String,String> payload) async {
  var client = http.Client();

  try {
    var response = await client.post(
      'https://api.pagar.me/1/transactions',
      headers: {
        HttpHeaders.acceptHeader: 'application/json',
        HttpHeaders.contentTypeHeader: 'application/json'
      },
      body: jsonEncode(payload)
    );

    return jsonDecode(response.body);
  }
  finally {
    client.close();
  } 
  
}