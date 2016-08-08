package comegg_electric_unicycle.github.eggunicycle;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

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
import java.util.Vector;



public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private BluetoothDevice device;
    private BluetoothSocket socket;
   // private OutputStream outputStream;
    private InputStream inputStream;
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private boolean connected = true;
    //UI
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;
    private ListView list;
    private TextView  devicesEnable, speedView, voltageView, tripView, currentView, tempView, chargeView, chargeViewAdvanced, speedViewAdvanced, tripViewAdvanced;
    private RelativeLayout welcome, main, advanced, bluetooth, about ;

    boolean stopThread;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    int state=0;
    int speed, voltage, trip, current, temperature;
    double speed_, current_, voltage_, trip_, temperature_;
    boolean end = false;
    NavigationView navigationView;

    public Vector<Byte> serialData = new Vector<Byte>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //UI
        welcome= (RelativeLayout) findViewById(R.id.welcome);
        main= (RelativeLayout) findViewById(R.id.home);
        bluetooth = (RelativeLayout) findViewById(R.id.bluetooth);
        about = (RelativeLayout) findViewById(R.id.about);
        advanced = (RelativeLayout) findViewById(R.id.advanced);
        devicesEnable=(TextView) findViewById(R.id.devicesEnable);
        speedView = (TextView) findViewById(R.id.showSpeed);
        voltageView = (TextView) findViewById(R.id.showVoltage);
        tripView = (TextView) findViewById(R.id.showTrip);
        currentView = (TextView) findViewById(R.id.showCurrent);
        tempView = (TextView) findViewById(R.id.showTemp);
        chargeView=(TextView) findViewById(R.id.showCharge);
        chargeViewAdvanced = (TextView) findViewById(R.id.showChargeMore) ;
        speedViewAdvanced = (TextView) findViewById(R.id.showSpeedMore);
        tripViewAdvanced = (TextView) findViewById(R.id.showTripMore);
        list = (ListView) findViewById(R.id.listDevices1);

        welcome.setVisibility(View.VISIBLE);
        main.setVisibility(View.INVISIBLE);
        bluetooth.setVisibility(View.INVISIBLE);
        about.setVisibility(View.INVISIBLE);
        advanced.setVisibility(View.INVISIBLE);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //check if in memory exist some valid address saved in the past
        SharedPreferences memoryDevice  = getSharedPreferences("MyPrefsDevice", 0);
        String addressInMemory = memoryDevice.getString("deviceAdress", null);
        if (!(addressInMemory == null)) {
            try {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (!mBluetoothAdapter.isEnabled()) {  //check if bluetooth is on
                    Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableAdapter, 0);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                device = mBluetoothAdapter.getRemoteDevice(addressInMemory);   //set device address
                connectToDevice();
            }
            catch (InternalError a) {
                //if some error occurs go to bluetooth connection menu (manually connection)
                a.printStackTrace();
                onNavigationItemSelected(navigationView.getMenu().getItem(2));
            }
        }
        else {
            //if is the first time and any address is in memory go to bluetooth connection menu (manually connection)
            onNavigationItemSelected(navigationView.getMenu().getItem(2));
        }



     /*   disconnect.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                disconnectButtonClick();
            }
        });*/


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

                //Toast.makeText(getBaseContext(), item, Toast.LENGTH_LONG).show();
                device = mBluetoothAdapter.getRemoteDevice(address);
              //  runOnUiThread(new Runnable() {  public void run() {  findViewById(R.id.pbHeaderProgress).setVisibility(View.VISIBLE);}});
                connectToDevice();


            }
        });
    }
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        try {
            inputStream.close();
            socket.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        list.setAdapter(null);


        super.onDestroy();
    }
    public void showEnableDevices()
    {
        //set adapter to show all devices in the listView
        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.style_text_item, arrayList);
        list.setAdapter(adapter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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

    }

    public void disconnectButtonClick()
    {
        try {
            inputStream.close();
            socket.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        list.setAdapter(null);

    }

    public boolean connectToDevice(){

        connected = true;
        BluetoothSocket tmp = null;

        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = tmp;

                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket.connect();
                } catch (IOException e) {
                    //connection to device failed so close the socket

                    try {
                        connected = false;
                        socket.close();

                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }


        if (connected) {
          /*  try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_LONG).show();
            }*/
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                runOnUiThread(new Runnable() {  public void run() { Toast.makeText(getBaseContext(), "Failed", Toast.LENGTH_LONG).show();}});
            }

        }
        if (connected) {
            //connection to device with sucess. Address of device is saved in SharedPreferences
            SharedPreferences memoryDevice = getSharedPreferences("MyPrefsDevice", 0);
            SharedPreferences.Editor editor = memoryDevice.edit();
            editor.putString("deviceAdress", device.getAddress());
            editor.commit();

            //start data listening
            beginListenForData();


        } else if (!connected) {
            connectToDevice();
        }
        return connected;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.ic_menu_home){
            welcome.setVisibility(View.INVISIBLE);
            main.setVisibility(View.VISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.INVISIBLE);

        }
        if (id == R.id.ic_menu_advanced){
            welcome.setVisibility(View.INVISIBLE);
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.VISIBLE);

        }

        if (id == R.id.ic_menu_bluetooth){
            welcome.setVisibility(View.INVISIBLE);
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.VISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.INVISIBLE);
            showEnableDevices();
        }

        if (id == R.id.ic_menu_about){
            welcome.setVisibility(View.INVISIBLE);
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.VISIBLE);
            advanced.setVisibility(View.INVISIBLE);

        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void beginListenForData()
    {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                onNavigationItemSelected(navigationView.getMenu().getItem(0));}});
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
                            end=false;
                            final byte[] rawBytes = new byte[byteCount];

                            inputStream.read(rawBytes);

                            // store all received bytes on vector
                            for (int i = 0; i < byteCount; ) {
                                serialData.add(rawBytes[i]);
                                i++;
                            }
                        }

                        processData();

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

public void processData() {

    while  (!serialData.isEmpty())
    {
        Byte data1 = serialData.get(0);
        int data = (int) data1;
        serialData.removeElementAt(0);
        if (data < 0) data += 256;

        switch (state) {

            // start by looking for the START sequence of bytes: 0x18 0x5a 0x5a 0x5a 0x5a 0x55 0xaa
            case 0:
                if (data == 24) {
                    state++;
                } else state = 0;
                break;

            case 1:
                if (data == 90) {
                    state++;
                } else state = 0;
                break;

            case 2:
                if (data == 90) {
                    state++;
                } else state = 0;
                break;

            case 3:
                if (data == 90) {
                    state++;
                } else state = 0;
                break;

            case 4:
                if (data == 90) {
                    state++;
                } else state = 0;
                break;

            case 5:
                if (data == 85) {
                    state++;
                } else state = 0;
                break;

            case 6:
                if (data == 170) {
                    state++;
                } else state = 0;
                break;

            // next 2 bytes are voltage
            case 7:
                state++;
                voltage = (data<<8);
                break;

            case 8:
                state++;
                voltage|= data;
                voltage_ = voltage/100;
                break;

            // next 2 bytes are speed
            case 9:
                state++;
                speed = (data << 8);
                break;

            case 10:
                state++;
                speed |= data;
                if (speed>(65536/2))
                    speed= speed-65536;
                if (speed<0)
                    speed= speed*(-1); // necessary to set positive value of speed
                speed_=speed*0.036;
                break;

            // next 4 bytes are trip distance
            case 11:
                state++;
                trip = (data<< 24);
                break;

            case 12:
                state++;
                trip |= (data<< 16);
                break;

            case 13:
                state++;
                trip |= (data<< 8);
                break;

            case 14:
                state++;
                trip |= data;
                trip_ = trip/1000.0;
                break;

            // next 2 bytes are current
            case 15:
                state++;
                current = (data << 8);
                break;

            case 16:
                state++;
                current |= data;
                if (current>(65536/2))
                    current= current-65536;
                current_ = (current / 100.000);
                break;

            // next 2 bytes are temperature
            case 17:
                state++;
                temperature= (data<<8);
                break;

            case 18:
                state++;
                temperature|= data;

                if (temperature>(65536/2))
                    temperature=temperature-65536;
                temperature_ = (temperature/340.0)+36.53;
                break;

            case 19:
                if (data == 0) {
                    state++;
                } else state = 0;
                break;

            case 20:
                if (data == 0) {
                    state++;
                } else state = 0;
                break;

            case 21:
                if (data == 255) {
                    state++;
                } else state = 0;
                break;

            case 22:
                if (data == 248) {
                    state++;
                } else state = 0;
                break;

            case 23:
                if (data == 0) {
                    state = 0;
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            if (main.getVisibility()== View.VISIBLE) {
                                speedView.setText(String.format("%2.1f", speed_));
                                chargeView.setText(Integer.toString(calcChargePercent(voltage_)));
                                tripView.setText(String.format("%2.2f",trip_));
                            }

                            else {

                                speedViewAdvanced.setText(String.format("%2.1f", speed_));
                                voltageView.setText(String.format("%2.1f", voltage_));
                                tripViewAdvanced.setText(String.format("%2.2f", trip_));
                                currentView.setText(String.format("%2.2f", current_));
                                tempView.setText(String.format("%2.1f", temperature_));
                                chargeViewAdvanced.setText(Integer.toString(calcChargePercent(voltage_)));
                            }

                        }
                    });
                } else state = 0;
                break;

            default:
                state = 0;
                break;
        }
    }

}
    public int calcChargePercent(double v)
    {
        int percent=0;
        if (v>=0 && v<52.1)
            percent= 0;
        if (v>=52.1 && v<53.1)
            percent=10;
        if (v>=53.1 && v< 54.2)
            percent=20;
        if (v>=54.2 && v< 56.4)
            percent=30;
        if (v>=56.4 && v<57.9)
            percent = 40;
        if (v>=57.9 && v< 59.3)
            percent=50;
        if (v>=59.3 && v<60.7)
            percent=60;
        if (v>=60.7 && v<61.9)
            percent=70;
        if (v>=61.9 && v<63.6)
            percent=80;
        if (v>=63.6 && v<65.0)
            percent= 90;
         if (v>=65.0)
            percent=100;

        return percent;
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
            if (arrayList.isEmpty()) {
                Toast.makeText(getBaseContext(), "No devices found", Toast.LENGTH_LONG).show();
            }

        }
    };

}
