package com.example.p2p_java;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.InetAddresses;
import android.Manifest;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    Button btnOnOff , btnDiscover, btnSend;
    ListView listView;
    TextView read_msg_box, connectionStatus;
    EditText writeMsg;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();  // the list of devices
    String[] deviceNameArray;     // where we gonna save the names of the devices to show on the listview
    WifiP2pDevice[] deviceArray; //  we use this array to connect a device
    static final int MESSAGE_READ=1;
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;


    @Override
    protected void onCreate(Bundle savedInstanceState) {  //premiere methode par ou ca passe
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();                                    // ici ca serait le on start
        exqListener();
    }


    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
           switch(msg.what){


            case MESSAGE_READ:
                byte[] readBuff= (byte[]) msg.obj;
                String tempMsg=new String(readBuff,0,msg.arg1);
                        read_msg_box.setText(tempMsg);
                        break;
           }
            return true;
        }

    });

    private void exqListener() {
btnOnOff.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        if(wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(false);
            btnOnOff.setText("ON");

        }
        else{
            wifiManager.setWifiEnabled(true);
            btnOnOff.setText("OFF");


        }
    }
});
            btnDiscover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            connectionStatus.setText("Discovery Started");
                        }

                        @Override
                        public void onFailure(int reason) {
                            connectionStatus.setText("Discovery Started Failed");
                        }
                    });
                }
            });

////////////////////////////
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    final WifiP2pDevice device= deviceArray[position];
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress= device.deviceAddress;
                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(),"Connected to "+ device.deviceName,Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_SHORT).show();
                            return;

                        }
                    });
                }
            });
            ////////////////////////////////////////////////
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = writeMsg.getText().toString();
                sendReceive.write(msg.getBytes());
            }
        });
    }

    private void initialWork() {
        btnOnOff = (Button) findViewById(R.id.onOff);
        btnDiscover= (Button) findViewById(R.id.discover);
        btnSend= (Button) findViewById(R.id.sendButton);
        listView= (ListView) findViewById(R.id.peerListView);
        read_msg_box=(TextView) findViewById(R.id.readMsg);
        connectionStatus= (TextView)findViewById(R.id.connectionStatus);
        writeMsg = (EditText) findViewById(R.id.writeMsg);

        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager= (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE); // this provide the API for managin wi-fi p2p connectivity
        mChannel= mManager.initialize(this,getMainLooper(),null); // a channel that connects the application to the wifi p2p framework

        mReceiver= new WiFiDirectBroadcastReceiver(mManager,mChannel, this); // to take action between the posibles cases
        mIntentFilter = new IntentFilter();                                            // it tells the os how to communicate with the different components (activities,services,broadcaste receives)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);          //********* HERE WE ADD INTENT ACTION TO MATCH ON THE FUTURE*************
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);          //An intent filter is an expression in an app's manifest file that
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);    //specifies the type of intents that the component would like to receive.
                                                                                        // For instance, by declaring an intent filter for an activity,
                                                                                        //you make it possible for other apps to directly start your activity with a certain kind of intent
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);    ////////////////////////



    }
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)){
                peers.clear();
                //
                peers.addAll(peerList.getDeviceList());
                deviceNameArray= new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index=0;

                for(WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }
                ArrayAdapter<String> adapter=new ArrayAdapter <String> (getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);


                }
            if(peers.size()==0){
                Toast.makeText(getApplicationContext(),"No Device Found", Toast.LENGTH_SHORT).show();
                return;
            }


            }


    };
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress= wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner)
            {

                connectionStatus.setText("Host");
                serverClass = new ServerClass();
                serverClass.start();
            }else if (wifiP2pInfo.groupFormed)
            {
                 connectionStatus.setText("Client");
                 clientClass = new ClientClass(groupOwnerAddress);
                 clientClass.start();
            }


        }
    };


    @Override
    protected void onResume() {  // ici toutes les variables sont crées et on commence à faire les calculs
        super.onResume();
        // Permission has already been granted
        registerReceiver(mReceiver,mIntentFilter);// if the action match with the filter, we will recive broadcasts until the app close
///////////////// permission position///////////////////////////
       if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 101);


        }
////////////////permision position///////////////////////////////
        super.onResume();
        // Permission has already been granted
        registerReceiver(mReceiver,mIntentFilter);// if the action match with the filter, we will recive broadcasts until the app close
    }

    @Override
    protected void onPause() {

        super.onPause();

        unregisterReceiver(mReceiver); //Be sure to unregister the receiver when you no longer need it or the context is no longer valid.

    }
// function to be sure what to do if te permision es
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //display somethin if the permision is  accepted


                } else {
                    // ici on doit display que la permission  est necessaire.

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

public class ServerClass extends Thread {

        Socket socket;
        ServerSocket serverSocket;
// Here we create a server class
    @Override
    public void run() {
        super.run();
        try {
            serverSocket=new ServerSocket(8888);
            socket= serverSocket.accept();
            sendReceive= new SendReceive(socket);
            sendReceive.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        public SendReceive(Socket skt){
            socket=skt;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while(socket!=null)
        {
            try {
                bytes=inputStream.read(buffer);
                if(bytes>0){
                    handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public void write(byte[] bytes){
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

    public class ClientClass extends Thread{

        Socket socket;
        String  hostAdd;

        public ClientClass(InetAddress hostAddress)
        {
            hostAdd=hostAddress.getHostAddress();
            socket=new Socket();


        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd,8888),500);
                sendReceive= new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

}
