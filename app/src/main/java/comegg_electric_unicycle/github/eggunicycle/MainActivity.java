package comegg_electric_unicycle.github.eggunicycle;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import android.content.Intent;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;

import java.util.ArrayList;
import java.lang.String;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int NOCONNECT = 0;
    private static final int CONNECT = 1;
    public static BluetoothDevice device;
    private Thread connectionThread;
    String addressInMemory;
    //UI
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;
    private ListView list;
    public static TextView  devicesEnable;
    public static ImageView connectState;
    public static RelativeLayout main, advanced, bluetooth, about, calibration ;

    public static boolean stopThread;
    int count;

    NavigationView navigationView;

    ElectricUnicycle SendEUCCommand = new ElectricUnicycle(MainActivity.this);
    Bluetooth Bluetooth = new Bluetooth(MainActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //UI
        main= (RelativeLayout) findViewById(R.id.home);
        bluetooth = (RelativeLayout) findViewById(R.id.bluetooth);
        about = (RelativeLayout) findViewById(R.id.about);
        advanced = (RelativeLayout) findViewById(R.id.advanced);
        calibration=(RelativeLayout) findViewById(R.id.calibration);
        devicesEnable=(TextView) findViewById(R.id.devicesEnable);

        list = (ListView) findViewById(R.id.listDevices1);
        connectState=(ImageView) findViewById(R.id.connectState);

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //listener to show item click in list of enable devices found
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                // Cancel discovery because it's costly and we're about to connect
                Bluetooth.cancelResearch();
                runOnUiThread(new Runnable() {
                    public void run() {
                        findViewById(R.id.pbHeaderProgress).setVisibility(View.VISIBLE);
                    }
                });

                // Get the device MAC address, which is the last 17 chars in the View
                String item = ((TextView) view).getText().toString();
                String address = item.substring(item.length() - 17);

                device= Bluetooth.getDevice(address);
                connectToDevice();

            }
        });

        // the action code for button beep
        final Button buttonBeep = (Button) findViewById(R.id.buttonBeep);
        buttonBeep.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendEUCCommand.beep();
            }
        });

        //the action code for button soft mode
        final Button buttonSoft = (Button) findViewById(R.id.soft);
        buttonSoft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendEUCCommand.setRideMode(ElectricUnicycle.rideModes.SOFT);
            }
        });
        //the action code for button confort mode
        final Button buttonComfort = (Button) findViewById(R.id.comfort);
        buttonComfort.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendEUCCommand.setRideMode(ElectricUnicycle.rideModes.COMFORT);
            }
        });
        //the action code for button madden mode
        final Button buttonMadden = (Button) findViewById(R.id.madden);
        buttonMadden.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendEUCCommand.setRideMode(ElectricUnicycle.rideModes.MADDEN);
            }
        });

        //the action code for button horizontal calibration
        final Button buttonCalibration = (Button) findViewById(R.id.horizontalCalib);
        buttonCalibration.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               SendEUCCommand.setHorizontalCalibration();
            }
        });

        //the action code for help button in calibration
        final ImageButton buttonHelpMode = (ImageButton) findViewById(R.id.detailsMode);
        buttonHelpMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                builder.setMessage(R.string.modeHelp)
                        .setTitle("Ride mode");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        //the action code for help button in calibration
        final ImageButton buttonHelpAlignment = (ImageButton) findViewById(R.id.detailsAlignment);
        buttonHelpAlignment.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                builder.setMessage(R.string.alignmentHelp)
                        .setTitle("Upright calibration");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });


    }
    protected void onStart(){

        //check if in memory exist some valid address saved in the past
        SharedPreferences memoryDevice  = getSharedPreferences("MyPrefsDevice", 0);
        addressInMemory = memoryDevice.getString("deviceAdress", null);
        if (!(addressInMemory == null)) {

            onNavigationItemSelected(navigationView.getMenu().getItem(0));
            try {
                Bluetooth.checkDevice();
                device= Bluetooth.getDevice(addressInMemory);
                count =0;
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

        super.onStart();

    }

    protected void onDestroy() {
        unregisterReceiver(mReceiver);

        Bluetooth.closeInputStream();
        Bluetooth.closeOutputStream();
        Bluetooth.disconnectSocket();

        list.setAdapter(null);
        super.onDestroy();
    }
    public void showEnableDevices()
    {
        //set adapter to show all devices in the listView
        arrayList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.style_text_item, arrayList);
        list.setAdapter(adapter);
       /* if (Bluetooth.isConnected)
        {
            arrayList.add(device.getName() + "\n" + device.getAddress());

            final View view = View.inflate(MainActivity.this, R.layout.style_text_item, null);
            view.setBackgroundColor(Color.BLUE);

            adapter.notifyDataSetChanged();

        }*/






        Bluetooth.checkDevice();
        Bluetooth.startResearch();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

    }

    public void disconnectButtonClick()
    {

        Bluetooth.closeInputStream();
        Bluetooth.closeOutputStream();
        Bluetooth.disconnectSocket();


        list.setAdapter(null);

    }

    private void connectToDevice(){

        this.runOnUiThread(new Runnable() {
            public void run() {
                connectState.setVisibility(View.INVISIBLE);
                findViewById(R.id.pbHeaderProgressConnect).setVisibility(View.VISIBLE);
            }
        });
       connectionThread = new Thread() {
           @Override
           public void run() {
               while(!Thread.currentThread().isInterrupted()) {
                   count++;


                   Bluetooth.connectSocket();

                   if (Bluetooth.isConnected) {
                       runOnUiThread(new Runnable() {
                           public void run() {
                               connectState.setVisibility(View.VISIBLE);
                               findViewById(R.id.pbHeaderProgressConnect).setVisibility(View.INVISIBLE);
                               connectState.setImageResource(R.drawable.ic_connect);
                           }
                       });


                       connectionThread.interrupt();

                       // finished thread
                       Message msg = Message.obtain();
                       msg.what = CONNECT;
                       handler.sendMessage(msg);


                   } else {
                       // finished thread

                       Message msg = Message.obtain();
                       msg.what = NOCONNECT;
                       handler.sendMessage(msg);
                   }
               }
           }
       };

       connectionThread.start();

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT:

                    //countView.setVisibility(View.INVISIBLE);
                    //connection to device with sucess. Address of device is saved in SharedPreferences
                    SharedPreferences memoryDevice = getSharedPreferences("MyPrefsDevice", 0);
                    SharedPreferences.Editor editor = memoryDevice.edit();
                    editor.putString("deviceAdress", device.getAddress());
                    editor.commit();

                    //start data listening
                    beginListenForData();

                    break;
                case NOCONNECT:
                    //connectToDevice();
                    break;
            }
        }
    };


    //@SuppressWarnings("StatementWithEmptyBody")
    //@Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.ic_menu_home){
            main.setVisibility(View.VISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.INVISIBLE);
            calibration.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle("Home");
            findViewById(R.id.pbHeaderProgressConnect).setVisibility(View.INVISIBLE);
            connectState.setVisibility(View.VISIBLE);
            if (Bluetooth.isConnected) {
                connectState.setImageResource(R.drawable.ic_connect);
            }
            else{
                connectState.setImageResource(R.drawable.ic_disconnect);
            }

        }
        if (id == R.id.ic_menu_advanced){
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.VISIBLE);
            calibration.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle("Advanced");

        }

        if (id == R.id.ic_menu_bluetooth){
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.VISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.INVISIBLE);
            calibration.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle("Connect Unicycle");
            showEnableDevices();
        }


        if (id == R.id.ic_menu_calibration){
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.INVISIBLE);
            calibration.setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle("Calibration");

        }

        if (id == R.id.ic_menu_about){
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.VISIBLE);
            advanced.setVisibility(View.INVISIBLE);
            calibration.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle("About");

        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void beginListenForData()
    {
        runOnUiThread(new Runnable() {
            public void run() {
                onNavigationItemSelected(navigationView.getMenu().getItem(0));}});

        Bluetooth.readData();
    }



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                findViewById(R.id.pbHeaderProgress).setVisibility(View.INVISIBLE);
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
