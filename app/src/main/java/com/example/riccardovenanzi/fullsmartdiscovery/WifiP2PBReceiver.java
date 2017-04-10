package com.example.riccardovenanzi.fullsmartdiscovery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

/**
 * Created by riccardovenanzi on 20/02/17.
 */

public class WifiP2PBReceiver extends BroadcastReceiver {

    private MainActivity myAct;
    private WifiP2pManager wifiManager;
    private WifiP2pManager.Channel canale;
    private boolean firstTime=true;

    public WifiP2PBReceiver(WifiP2pManager.Channel canale, WifiP2pManager wifiManager, MainActivity myAct) {
        super();
        this.canale = canale;
        this.wifiManager = wifiManager;
        this.myAct = myAct;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED && firstTime) {
                Toast.makeText(myAct, "Wi-Fi P2P is enabled", Toast.LENGTH_SHORT).show();
                firstTime=false;
            } else {
                Toast.makeText(myAct, "Wi-Fi P2P is not enabled", Toast.LENGTH_SHORT).show();

            }
        }

        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (wifiManager != null) {
                System.out.println("qui ci entrooooooooooooooooooooooooooooooo");
                wifiManager.requestPeers(canale, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        for(WifiP2pDevice peer : peers.getDeviceList())
                        {
                            myAct.getResultTextArea().append(peer.deviceName);
                        }
                    }
                });
            }
        }
    }
}
