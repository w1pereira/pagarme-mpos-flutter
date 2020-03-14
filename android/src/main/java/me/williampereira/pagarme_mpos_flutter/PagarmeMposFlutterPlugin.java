package me.williampereira.pagarme_mpos_flutter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.flutter.app.FlutterActivity;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import me.pagar.mposandroid.EmvApplication;
import me.pagar.mposandroid.Mpos;
import me.pagar.mposandroid.MposListener;
import me.pagar.mposandroid.MposPaymentResult;

/** PagarmeMposFlutterPlugin */
public class PagarmeMposFlutterPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {

  private Mpos mpos = null;
  private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
  private final Context context;
  private EventChannel.EventSink eventSink;
  private Handler uiThreadHandler = new Handler(Looper.getMainLooper());

  public PagarmeMposFlutterPlugin() {
    this.context = null;
  }

  private PagarmeMposFlutterPlugin(Context context) {
    this.context = context;
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    PagarmeMposFlutterPlugin plugin = new PagarmeMposFlutterPlugin(flutterPluginBinding.getApplicationContext());

    final MethodChannel channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "pagarme_mpos_flutter");
    channel.setMethodCallHandler(plugin);

    final EventChannel eventChannel = new EventChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "mpos_stream");
    eventChannel.setStreamHandler(plugin);
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  public static void registerWith(Registrar registrar) {
    PagarmeMposFlutterPlugin plugin = new PagarmeMposFlutterPlugin(registrar.activity().getApplicationContext());

    final MethodChannel channel = new MethodChannel(registrar.messenger(), "pagarme_mpos_flutter");
    channel.setMethodCallHandler(new PagarmeMposFlutterPlugin(registrar.activity().getApplicationContext()));

    final EventChannel eventChannel = new EventChannel(registrar.messenger(), "mpos_stream");
    eventChannel.setStreamHandler(plugin);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
      return;
    }

    if (call.method.equals("createMpos")) {
      try {
        this.CreateMpos((String) call.argument("deviceName"),
                (String) call.argument("encryptionKey"));
        result.success(true);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return;
    }

    if (call.method.equals("initialize")) {
      this.Initialize();
      result.success(true);
      return;
    }

    if (call.method.equals("downloadEmvTablesToDevice")) {
      try {
        this.DownloadEmvTablesToDevice((boolean) call.argument("forceUpdate"));
        result.success(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }

    if (call.method.equals("payAmount")) {
      try {
        this.PayAmount((Integer) call.argument("amount"), (ArrayList<String>)
                call.argument("cardBrandList"), (Integer) call.argument("paymentMethod"));
        result.success(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }

    if (call.method.equals("close")) {
      this.Close((String) call.argument("message"));
      result.success(true);
      return;
    }

    if (call.method.equals("closeConnection")) {
      this.CloseConnection();
      result.success(true);
      return;
    }

    if(call.method.equals("finishTransaction")) {
      try {
        this.FinishTransaction((Boolean) call.argument("connected"),
          (Integer) call.argument("responseCode"), (String) call.argument("emvData"));
        result.success(true);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }

    if(call.method.equals("openConnection")) {
      this.OpenConnection((Boolean) call.argument("secure"));
      result.success(true);
      return;
    }

    if(call.method.equals("displayText")) {
      this.DisplayText((String) call.argument("message"));
      result.success(true);
      return;
    }

    result.notImplemented();
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
  }

  private void CreateMpos(String deviceName, String encryptionKey) throws IOException {
    List<BluetoothDevice> pairedDevices = new ArrayList<>(bluetoothAdapter.getBondedDevices());
    BluetoothDevice bluetoothDevice = findByAddress(deviceName, pairedDevices);
    mpos = new Mpos(bluetoothDevice, encryptionKey, context);
    this.setUpListeners();
  }

  private void Initialize() {
    mpos.initialize();
  }

  private void DownloadEmvTablesToDevice(Boolean forceUpdate)  throws Exception {
    try {
      mpos.downloadEMVTablesToDevice(forceUpdate);
    }
    catch (Exception e) {
      throw new Exception("Error in table update: " + e.getMessage());
    }
  }

  private void Close(String message) {
    mpos.close(message);
  }

  private void CloseConnection() {
    mpos.closeConnection();
  }

  private void FinishTransaction(Boolean connected, Integer responseCode, String emvData) {
    mpos.finishTransaction(connected, responseCode, emvData);
  }

  private void OpenConnection(Boolean secure) {
    mpos.openConnection(secure);
  }

  private void DisplayText(String message) {
    mpos.displayText(message);
  }

  private void PayAmount(Integer amount, ArrayList<String> cardBrandList, Integer paymentMethod) throws Exception {
    mpos.payAmount(amount, toEmvApplicationsList(cardBrandList, paymentMethod), paymentMethod);
  }

  static private HashMap<String, String> toResultMap(String cardHash, MposPaymentResult result) {
    HashMap<String, String> resultDart = new HashMap<>();
    resultDart.put("cardFirstDigits", result.cardFirstDigits);
    resultDart.put("cardLastDigits", result.cardLastDigits);
    resultDart.put("cardBrand", result.cardBrand);
    resultDart.put("localTransactionId", result.localTransactionId);
    resultDart.put("paymentMethod", String.valueOf(result.paymentMethod));
    resultDart.put("isOnline", String.valueOf(result.isOnline));
    resultDart.put("shouldFinishTransaction", String.valueOf(result.shouldFinishTransaction));
    resultDart.put("cardHash", cardHash);

    return resultDart;
  }

  static private List<EmvApplication> toEmvApplicationsList(ArrayList<String> cardBrandList, int paymentMethod) throws Exception {
    List<EmvApplication> emvApplicationsList = new ArrayList<>();
    if (cardBrandList != null && cardBrandList.size() > 0) {
      for(int i = cardBrandList.size() - 1; i >= 0; i--) {
        String cardBrand = cardBrandList.get(i);
        emvApplicationsList.add(new EmvApplication(paymentMethod, cardBrand));
      }
      return emvApplicationsList;
    }
    return null;
  }

  static private BluetoothDevice findByAddress(String deviceName, List<BluetoothDevice> deviceList) {
    for (BluetoothDevice device: deviceList) {
      if(device.getName().equals(deviceName)) return device;
    }
    return null;
  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    this.eventSink = events;
  }

  @Override
  public void onCancel(Object arguments) {
    this.eventSink = null;
  }

  private HashMap<String, String> getEvent(String methodName, String value)
  {
    HashMap<String, String> event = new HashMap<>();

    event.put("method", methodName);
    event.put("value", value);

    return event;
  }

  private void setUpListeners() {
    mpos.addListener(new MposListener() {

      public void bluetoothConnected() {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onBluetoothConnected", null));
          }
        }));
      }

      public void bluetoothDisconnected() {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onBluetoothDisconnected", null));
          }
        }));
      }

      public void bluetoothErrored(final int error) {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onBluetoothErrored", String.valueOf(error)));
          }
        }));
      }

      public void receiveInitialization() {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onReceiveInitialization", null));
          }
        }));
      }

      public void receiveNotification(final String notification) {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onReceiveNotification", notification));
          }
        }));
      }

      public void receiveTableUpdated(final boolean loaded) {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onReceiveTableUpdated", String.valueOf(loaded)));
          }
        }));
      }

      public void receiveFinishTransaction() {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onReceiveFinishTransaction", null));
          }
        }));
      }

      public void receiveClose() {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onReceiveClose", null));
          }
        }));
      }

      public void receiveCardHash(String cardHash, MposPaymentResult result) {
        Gson gson = new Gson();
        String resultJson = gson.toJson(toResultMap(cardHash, result));

        final HashMap<String, String> receiveCardHashEvent = new HashMap<>();
        receiveCardHashEvent.put("method", "onReceiveCardHash");
        receiveCardHashEvent.put("value", resultJson);

        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(receiveCardHashEvent);
          }
        }));
      }

      public void receiveError(final int error) {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onReceiveError", String.valueOf(error)));
          }
        }));
      }

      public void receiveOperationCancelled() {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onReceiveOperationCancelled", null));
          }
        }));
      }

      public void receiveOperationCompleted() {
        uiThreadHandler.post((new Runnable() {
          @Override
          public void run() {
            eventSink.success(getEvent("onReceiveOperationCompleted", null));
          }
        }));
      }

    });
  }
}
