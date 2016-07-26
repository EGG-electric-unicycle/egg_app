package comegg_electric_unicycle.github.eggunicycle;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import android.content.Intent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.lang.String;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private BluetoothSocket tmp;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //UI
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;
    private ListView list;
    private Button findDevices, disconnect;
    private TextView connectedWith, connectedWithC, devicesEnable;

    boolean stopThread;
    Thread thread;
    byte buffer[];
    int bufferPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectedWith=(TextView) findViewById(R.id.connectedWith);
        connectedWithC=(TextView) findViewById(R.id.connectedWithC);
        devicesEnable=(TextView) findViewById(R.id.devicesEnable);
        findDevices = (Button) findViewById(R.id.buttonFind);
        disconnect= (Button) findViewById(R.id.buttonDisconnect);
        list = (ListView) findViewById(R.id.listDevices1);

        connectedWith.setVisibility(View.INVISIBLE);
        connectedWithC.setVisibility(View.INVISIBLE);
        devicesEnable.setVisibility(View.INVISIBLE);
        findDevices.setVisibility(View.INVISIBLE);
        disconnect.setVisibility(View.INVISIBLE);
        list.setVisibility(View.INVISIBLE);


        arrayList = new ArrayList<String>();

        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, arrayList);
        list.setAdapter(adapter);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        findDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findButtonClick();
           }
        });


        // Get local Bluetooth adapter


        //listener to show item click in list of enable devices found
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                // Cancel discovery because it's costly and we're about to connect
                mBluetoothAdapter.cancelDiscovery();


                // Get the device MAC address, which is the last 17 chars in the View
                String item = ((TextView) view).getText().toString();
                String address = item.substring(item.length() - 17);

                Toast.makeText(getBaseContext(), item, Toast.LENGTH_LONG).show();
                device = mBluetoothAdapter.getRemoteDevice(address);
                connectToDevice();

            }
        });
    }
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
    public void findButtonClick(){



        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        mBluetoothAdapter.startDiscovery();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        devicesEnable.setVisibility(View.VISIBLE);
        findDevices.setVisibility(View.INVISIBLE);
        list.setVisibility(View.VISIBLE);



    }

    public boolean connectToDevice(){
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_LONG).show();
            connected=false;
        }
        if (connected) {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_LONG).show();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_LONG).show();
            }

        }
        beginListenForData();
        return connected;
    }
    public boolean checkIfisConnected()
    {
        boolean isConnected=false;
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            isConnected=false;
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                    device=iterator;
                    isConnected=true;

                    break;

            }
        }
        return isConnected;

    }
  /*  @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }*/



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.ic_menu_bluetooth){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(checkIfisConnected())
            {
                connectToDevice();
                beginListenForData();



            }
            else{
                connectedWith.setVisibility(View.INVISIBLE);
                connectedWithC.setVisibility(View.INVISIBLE);
                devicesEnable.setVisibility(View.INVISIBLE);
                findDevices.setVisibility(View.VISIBLE);
                disconnect.setVisibility(View.INVISIBLE);
                list.setVisibility(View.INVISIBLE);

            }




        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void beginListenForData()
    {
        connectedWith.setVisibility(View.VISIBLE);
        connectedWithC.setVisibility(View.VISIBLE);
        connectedWithC.setText(device.getName());
        devicesEnable.setVisibility(View.INVISIBLE);
        findDevices.setVisibility(View.INVISIBLE);
        disconnect.setVisibility(View.VISIBLE);
        list.setVisibility(View.INVISIBLE);



        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run()
                                {
                                   devicesEnable.append(string);
                                }
                            });

                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }




    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice deviceFound = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);


                arrayList.add(deviceFound.getName() + "\n" + deviceFound.getAddress());
                adapter.notifyDataSetChanged();

            }
        }
    };




}
