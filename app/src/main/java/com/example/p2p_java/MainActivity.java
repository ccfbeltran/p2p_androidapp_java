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
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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

public class MainActivity extends Activity {


    Button btnOnOff , btnDiscover, btnSend;
    ListView listView;
    TextView read_msg_box, connectionStatus;
    EditText writeMsg;
    boolean is_owner=false;
    boolean is_client=false;
    boolean client_socket_activate=false;

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
        initialWork();
        exqListener();// ici ca serait le on start

    }
    
    public void disconnect(){
        if (mManager != null && mChannel !=null){
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null || (mManager != null && mChannel != null)){
                        unregisterReceiver(mReceiver); //Be sure to unregister the receiver when you no longer need it or the context is no longer valid.
                        client_socket_activate=false;

                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getApplicationContext(), "removeGroup onSuccess - ", Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onFailure(int reason) {
                                Toast.makeText(getApplicationContext(), "removeGroup onFailure -", Toast.LENGTH_SHORT).show();
                                mManager=null;
                                mChannel= null;

                            }
                        });
                        if(is_owner==false && is_client==true) {
                            is_owner = false;
                            is_owner = false;
                            if (clientClass.socket != null) {
                                try {
                                    clientClass.socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }



                        }
                        // close for the owner
                        if(is_owner==true && is_client==false) {
                            if (serverClass.serverSocket != null) {
                                try {
                                    serverClass.serverSocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            is_owner = false;
                            is_owner = false;



                        }

                    }
                }
            });
        }

    }

    public void reconnect(){
        if (mManager != null && mChannel !=null){
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null){

                      //  mManager.requestConnectionInfo(mChannel,connectionInfoListener);
                        if(is_owner==false && is_client==true){
                            Toast.makeText(getApplicationContext(),"CONNECTED TO A DEVICE and you ar the client",Toast.LENGTH_SHORT).show();
                            clientClass.reconnect_client();


                        }
                        if(is_owner==true && is_client==false){
                            Toast.makeText(getApplicationContext(),"CONNECTED TO A DEVICE and you ar the owner",Toast.LENGTH_SHORT).show();

                        }

                    }
                }
            });
        }
    }




    @Override
    protected void onRestart() {
        super.onRestart();
        reconnect();
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
// here is where we can see the action when the bouttons are pressed
    private void exqListener() {
        //here for the  on off wifi boutton
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

// here is the action  discover peers boutton
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

// here is the option for the listview where the devices appears.
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

// here is where the action for the send boutton is set
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = writeMsg.getText().toString();
                if(sendReceive!=null)
                sendReceive.write(msg.getBytes());
                else{
                    Toast.makeText(getApplicationContext(),"you are not connected to a device yet", Toast.LENGTH_SHORT).show();
                    if(is_owner || is_client )

                    mManager.requestConnectionInfo(mChannel,connectionInfoListener);
                }
            }
        });
    }
public void make_names(){



}
    //here we set all the  bouttons andvariables that we'll need
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
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);    //specifies the type of intents that the component would like to receive. // For instance, by declaring an intent filter for an activity,you make it possible for other apps to directly start your activity with a certain kind of intent
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }
    // we create the peerlistener. This variable will be useded to capture the names  of the devices
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
    // this fonction  will determine if the device is a host or a client and will do the procedures for communication and send message
    // the fonction start fo each class is overwrite
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress= wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner)
            {

                connectionStatus.setText("Host");
                serverClass = new ServerClass();
                serverClass.start();
                is_owner=true;
                is_client= false;

            }else if (wifiP2pInfo.groupFormed)
            {
                 connectionStatus.setText("Client");
                 clientClass = new ClientClass(groupOwnerAddress);
                 clientClass.start();
                is_owner=false;
                is_client= true;

            }


        }

    };


    @Override
    protected void onResume() {  // here all the variables are created and we start making the process
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);// if the action match with the filter, we will recive broadcasts until the app close






//ask permission for the possition and keep it asking until it as allowed
       if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 101);


        }


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    @Override
    protected void onPause() {

        super.onPause();

    // unregisterReceiver(mReceiver); //Be sure to unregister the receiver when you no longer need it or the context is no longer valid.




    }
// function to be sure what to do if the permissions are denied
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
// this is the serverclass where the server will be connected as a server and make the process


public class ServerClass extends Thread {

        Socket socket;
        ServerSocket serverSocket;

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
                if(!client_socket_activate) {
                    socket.connect(new InetSocketAddress(hostAdd, 8888));
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    client_socket_activate= true;
                }

            } catch (IOException e) {
                e.printStackTrace();
                throw new Error("Copying Failed");
            }

        }

        public void reconnect_client(){





        }
    }

private class SendReceive extends Thread {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public SendReceive(Socket skt) {
        socket = skt;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (socket != null) {
            try {
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void write(final byte[] bytes) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    outputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }
}





}
