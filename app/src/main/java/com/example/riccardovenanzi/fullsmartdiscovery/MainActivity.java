package com.example.riccardovenanzi.fullsmartdiscovery;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.util.StringTokenizer;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private Switch swBle;
    private Switch swWifi;
    private Button discoveryButton;
    private CheckBox cbBle, cbWifi, cbSmart;
    private EditText etResults;
    private Switch swMqtt;
    private final static int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 1;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bleAdapter;
    private boolean bleDiscovery = false;
    private boolean wifiDiscovery = false;
    private BluetoothLeAdvertiser advertiser;
    private AdvertiseSettings settings;
    private AdvertiseData data;
    private AdvertiseCallback callback;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private BroadcastReceiver mReceiver;

    private IntentFilter myIntentFilter;

    private ScanCallback mLeScanCallback;
    private BluetoothLeScanner scanner;

    private LocationManager locationManager;
    private MqttAndroidClient client;
    private MqttConnectOptions mqttConnectOptions;

    private boolean granted = true;
    private boolean mqttGranted = true;


    private String mqttTopic = "Prova";
    private int mqttQos = 1;
    private Location ownLocation;
    private LocationManager myLM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swBle = (Switch) findViewById(R.id.bleSwitch);
        swWifi = (Switch) findViewById(R.id.wifip2pswitch2);
        discoveryButton = (Button) findViewById(R.id.discoveryButton);
        cbBle = (CheckBox) findViewById(R.id.bleCB);
        cbBle.setOnCheckedChangeListener(this);
        cbWifi = (CheckBox) findViewById(R.id.wifiCB);
        cbWifi.setOnCheckedChangeListener(this);
        cbWifi.setEnabled(false);
        swMqtt = (Switch) findViewById(R.id.swMqtt);
        cbSmart = (CheckBox) findViewById(R.id.smartCB);
        cbSmart.setChecked(false);
        cbSmart.setOnCheckedChangeListener(this);

        etResults = (EditText) findViewById(R.id.resultArea);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bluetoothManager.getAdapter();

        myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        myIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        myIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        myIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        mManager = (WifiP2pManager) this.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(MainActivity.this, getMainLooper(), null);
        mReceiver = new WifiP2PBReceiver(mChannel, mManager, MainActivity.this);


        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://137.204.57.34:1883", clientId);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    //reconnected

                    Toast.makeText(MainActivity.this, "MQTT Client: Connected", Toast.LENGTH_SHORT).show();
                } else {

                }

            }

            @Override
            public void connectionLost(Throwable cause) {

                Toast.makeText(MainActivity.this, "MQTT Client: Connection Lost", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Toast.makeText(MainActivity.this, "Topic: " + topic + "\nMessage: " + message, Toast.LENGTH_LONG).show();
                StringTokenizer st = new StringTokenizer(message.toString());
                double lat = Double.parseDouble(st.nextToken());

                double lon = Double.parseDouble(st.nextToken());

                Location l = new Location("");
                l.setLatitude(lat);
                l.setLongitude(lon);
                int distance = (int) ownLocation.distanceTo(l);
                Toast.makeText(MainActivity.this, "distance: "+ownLocation.distanceTo(l), Toast.LENGTH_LONG).show();
                
                SelectSmartModeOption(distance);

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        int asd = 4;


        final WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            swWifi.setEnabled(true);
            swMqtt.setEnabled(true);
        } else {
            swWifi.setEnabled(false);
            swMqtt.setEnabled(false);
            Toast.makeText(MainActivity.this, "For Enabling WifiP2P and Smart Mode, wifi is required", Toast.LENGTH_LONG).show();
        }


        swMqtt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
                    System.out.println("-----------------------------------------------------------");
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WAKE_LOCK},
                            2);

                }
                if (b && mqttGranted) {
                    try {
                        client.connect(mqttConnectOptions, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                                disconnectedBufferOptions.setBufferEnabled(true);
                                disconnectedBufferOptions.setBufferSize(100);
                                disconnectedBufferOptions.setPersistBuffer(false);
                                disconnectedBufferOptions.setDeleteOldestMessages(false);
                                client.setBufferOpts(disconnectedBufferOptions);
                                Toast.makeText(MainActivity.this, "MQTT Client: Connected", Toast.LENGTH_SHORT).show();


                                try {
                                    IMqttToken subToken = client.subscribe(mqttTopic, 1);
                                    subToken.setActionCallback(new IMqttActionListener() {
                                        @Override
                                        public void onSuccess(IMqttToken asyncActionToken) {
                                            // The message was published
                                            Toast.makeText(MainActivity.this, "MQTT Client: Subscribed", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onFailure(IMqttToken asyncActionToken,
                                                              Throwable exception) {
                                            // The subscription could not be performed, maybe the user was not
                                            // authorized to subscribe on the specified topic e.g. using wildcards

                                        }
                                    });

                                } catch (MqttException e) {
                                    e.printStackTrace();
                                }

                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                            }
                        });
                        //  token = client.connect();


                        System.out.println(" sssss sssss ssss ssss ssssss sssss sss -----------------> " + client.isConnected());


                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }


            }
        });

        scanner = bleAdapter.getBluetoothLeScanner();
        mLeScanCallback = new ScanCallback() {


            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                System.out.println("----------------- Ho Trovato Qualocsa ---------------------");

                BluetoothDevice device = result.getDevice();

                etResults.append("" + device.getName() + " " + device.getUuids() + " rssi: " + result.getRssi() + "\n");
            }
        };


        swWifi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {

                    cbWifi.setEnabled(true);

                } else {
                    cbWifi.setChecked(false);
                    cbWifi.setEnabled(false);
                }
            }
        });


        if (bleAdapter.isEnabled()) {
            swBle.setChecked(true);
            cbBle.setEnabled(true);

        } else {
            swBle.setChecked(false);
            cbBle.setEnabled(false);
        }

        if (!bleDiscovery && !wifiDiscovery) {
            discoveryButton.setEnabled(false);
        }

        swBle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    bleAdapter.enable();

                } else
                    bleAdapter.disable();

                cbBle.setEnabled(b);
            }
        });

        discoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bleDiscovery)
                    DiscoveryBLE();
                if (wifiDiscovery)
                    DiscoveryWifi();

            }
        });

        myLM = (LocationManager) getSystemService(LOCATION_SERVICE);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            return;
        }
        else{
            ownLocation = myLM.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }


    }

    private void SelectSmartModeOption(int distance) {
    }

    public EditText getResultTextArea() {
        return this.etResults;
    }

    private void DiscoveryWifi() {
        //discovery wifip2p
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(MainActivity.this, "succes", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "fail", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*inserire qui la richiesta del gps per la location ed il setting dello smart mode per il ble*/
    private void DiscoveryBLE() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);
        } else {
            //PrepareAdvertiser();
            //advertiser.startAdvertising(settings, data, callback);
        }

        if (granted)
            scanner.startScan(mLeScanCallback);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {


        switch (compoundButton.getId()) {
            case (R.id.bleCB): {
                bleDiscovery = b;

            }
            break;
            case (R.id.wifiCB): {
                wifiDiscovery = b;
            }
            break;
        }


        discoveryButton.setEnabled(bleDiscovery || wifiDiscovery);
    }

    private void PrepareAdvertiser() {
        settings = PrepareAdvertiseSettings();

        data = PrepareAdvertiseData();

        callback = PrepareAdvertiseCallback();

        // advertiser.startAdvertising(settings, data, callback);
    }


    private AdvertiseSettings PrepareAdvertiseSettings() {
        AdvertiseSettings ads = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();
        return ads;
    }

    private AdvertiseData PrepareAdvertiseData() {
        ParcelUuid pUuid = new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid)));

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                //.addServiceUuid( pUuid )
                .addServiceData(pUuid, "Data".getBytes(Charset.forName("UTF-8")))
                .build();


        return data;
    }

    private AdvertiseCallback PrepareAdvertiseCallback() {
        AdvertiseCallback adc = new AdvertiseCallback() {

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Toast.makeText(MainActivity.this, "Failure -> " + errorCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();

            }
        };
        return adc;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {


        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_LOCATION) {
            if (grantResults.length == 0 || (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED)) {
                // advertiser.startAdvertising(settings, data, callback);
                Toast.makeText(MainActivity.this, "Without granting location access permission, BLE discovery will not be performed", Toast.LENGTH_LONG).show();
                granted = false;
            } else {


                    //advertiser = bleAdapter.getBluetoothLeAdvertiser();
                    Toast.makeText( MainActivity.this, "scanning..", Toast.LENGTH_LONG ).show();

                    scanner.startScan( mLeScanCallback);



            }

        }

        if(requestCode == 2)
        {
            if(grantResults.length!=0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                swMqtt.setEnabled(true);
                swMqtt.setChecked(true);
                mqttGranted = true;

            }
            else
            {
                mqttGranted = false;
                swMqtt.setChecked(false);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new WifiP2PBReceiver(mChannel, mManager,this);
        registerReceiver(mReceiver, myIntentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);

    }
}
