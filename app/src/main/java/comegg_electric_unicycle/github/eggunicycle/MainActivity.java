package comegg_electric_unicycle.github.eggunicycle;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
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
    private String addressInMemory;
    //UI
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;
    private ListView list;
    private Spinner alert;
    private TextView alertValue;
    private SeekBar seekBarValue;
    private SharedPreferences memoryDevice;
    private SharedPreferences.Editor editor;
    private boolean firstInit= true;
    public static String selection;
    public static String preferenceValue;

    public static TextView  devicesEnable;
    public static TextView text22;
    public static ImageView connectState;
    public static RelativeLayout main, advanced, bluetooth, about, settings ;

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
        settings=(RelativeLayout) findViewById(R.id.settings);
        devicesEnable=(TextView) findViewById(R.id.devicesEnable);

        memoryDevice = getSharedPreferences("MyPrefsDevice", 0);
        editor= memoryDevice.edit();
        alert = (Spinner) findViewById(R.id.spinner1);
        alertValue=(TextView) findViewById(R.id.value);
        seekBarValue = (SeekBar) findViewById(R.id.seekBar);
        list = (ListView) findViewById(R.id.listDevices1);
        connectState=(ImageView) findViewById(R.id.connectState);

        final TextView textAuxValue = (TextView) findViewById(R.id.valueAux);
        final TextView textUpDown = (TextView) findViewById(R.id.isEqualTo);

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getWindow().setStatusBarColor(Color.BLACK);
        }

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        //listener to show item click in list of enable devices found
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                if (Bluetooth.isConnected) { // if a device is connected is necessary clean all dependencies
                    Bluetooth.closeInputStream();
                    Bluetooth.closeOutputStream();
                    Bluetooth.disconnectSocket();
                }

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

        alert.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                if (firstInit) {
                    //read values saved in SharedPreferences
                    selection = memoryDevice.getString("parameter", null);
                    preferenceValue = memoryDevice.getString("parameterValue", null);

                    if (selection != null && preferenceValue != null) {
                        alert.setSelection(getIndex(alert , selection));
                        seekBarValue.setProgress(Integer.parseInt(preferenceValue));

                    }
                    else
                        selection = alert.getSelectedItem().toString();   //default value

                    firstInit=false;
                }
                else {
                    selection = alert.getSelectedItem().toString();   //selected value by user
                }
                if (selection.equals("Velocity")) {
                    seekBarValue.setMax(40);
                    textAuxValue.setText("km/h");
                    textUpDown.setText("Is up to");
                } else if (selection.equals("Battery level")) {
                    seekBarValue.setMax(100);
                    textAuxValue.setText("%");
                    textUpDown.setText("Is down to");
                } else if (selection.equals("Temperature")) {
                    seekBarValue.setMax(65);
                    textAuxValue.setText("ºC");
                    textUpDown.setText("Is up to");
                }
                    //Preferences are saved in SharedPreferences
                editor.putString("parameter", selection);
                editor.commit();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
            //private method of your class
            private int getIndex(Spinner spinner, String myString)
            {
                int index = 0;

                for (int i=0;i<spinner.getCount();i++){
                    if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                        index = i;
                        break;
                    }
                }
                return index;
            }

        });


        seekBarValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                if (selection.equals("Battery level")) {
                    int stepSize = 10;
                    progress = ((int)Math.round(progress/stepSize))*stepSize;
                    seekBar.setProgress(progress);
                }
                alertValue.setText(String.valueOf(progress));
                preferenceValue = String.valueOf(progress);
                //Preferences are saved in SharedPreferences
                editor.putString("parameterValue", String.valueOf(progress));
                editor.commit();


            }
        });

        // the action code for button beep
        final Button buttonBeep = (Button) findViewById(R.id.buttonBeep);
        buttonBeep.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        buttonBeep.getBackground().setColorFilter(getResources().getColor(R.color.colorIconPressed), PorterDuff.Mode.SRC_ATOP);
                        buttonBeep.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonBeep.getBackground().setColorFilter(getResources().getColor(R.color.colorIcon), PorterDuff.Mode.SRC_ATOP);
                        SendEUCCommand.beep();
                        buttonBeep.invalidate();
                        break;
                    }
                }
                return true;
            }
            /*public void onClick(View v) {
               SendEUCCommand.beep();
            }*/
        });

        //the action code for button soft mode
        final Button buttonSoft = (Button) findViewById(R.id.soft);
        buttonSoft.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        buttonSoft.getBackground().setColorFilter(getResources().getColor(R.color.colorIconPressed), PorterDuff.Mode.SRC_ATOP);
                        buttonSoft.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonSoft.getBackground().setColorFilter(getResources().getColor(R.color.colorIcon), PorterDuff.Mode.SRC_ATOP);
                        SendEUCCommand.setRideMode(ElectricUnicycle.rideModes.SOFT);
                        buttonSoft.invalidate();
                        break;
                    }
                }
                return true;
            }
            /*public void onClick(View v) {
               SendEUCCommand.beep();
            }*/
        });
        //the action code for button confort mode
        final Button buttonComfort = (Button) findViewById(R.id.comfort);
        buttonComfort.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        buttonComfort.getBackground().setColorFilter(getResources().getColor(R.color.colorIconPressed), PorterDuff.Mode.SRC_ATOP);
                        buttonComfort.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonComfort.getBackground().setColorFilter(getResources().getColor(R.color.colorIcon), PorterDuff.Mode.SRC_ATOP);
                        SendEUCCommand.setRideMode(ElectricUnicycle.rideModes.COMFORT);
                        buttonComfort.invalidate();
                        break;
                    }
                }
                return true;
            }
            /*public void onClick(View v) {
               SendEUCCommand.beep();
            }*/
        });
        //the action code for button madden mode
        final Button buttonMadden = (Button) findViewById(R.id.madden);
        buttonMadden.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        buttonMadden.getBackground().setColorFilter(getResources().getColor(R.color.colorIconPressed), PorterDuff.Mode.SRC_ATOP);
                        buttonMadden.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonMadden.getBackground().setColorFilter(getResources().getColor(R.color.colorIcon), PorterDuff.Mode.SRC_ATOP);
                        SendEUCCommand.setRideMode(ElectricUnicycle.rideModes.MADDEN);
                        buttonMadden.invalidate();
                        break;
                    }
                }
                return true;
            }
            /*public void onClick(View v) {
               SendEUCCommand.beep();
            }*/
        });
        //the action code for button horizontal calibration
        final Button buttonCalibration = (Button) findViewById(R.id.horizontalCalib);
        buttonCalibration.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        buttonCalibration.getBackground().setColorFilter(getResources().getColor(R.color.colorIconPressed), PorterDuff.Mode.SRC_ATOP);
                        buttonCalibration.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonCalibration.getBackground().setColorFilter(getResources().getColor(R.color.colorIcon), PorterDuff.Mode.SRC_ATOP);
                        SendEUCCommand.setHorizontalCalibration();
                        buttonCalibration.invalidate();
                        break;
                    }
                }
                return true;
            }
            /*public void onClick(View v) {
               SendEUCCommand.beep();
            }*/
        });


        //the action code for help button in settings
        final ImageButton buttonHelpMode = (ImageButton) findViewById(R.id.detailsMode);
        buttonHelpMode.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        buttonHelpMode.getDrawable().setColorFilter(getResources().getColor(R.color.colorIconPressed), PorterDuff.Mode.SRC_ATOP);
                        buttonHelpMode.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonHelpMode.getDrawable().setColorFilter(getResources().getColor(R.color.colorIcon), PorterDuff.Mode.SRC_ATOP);
                        builder.setMessage(R.string.modeHelp)
                                .setTitle("Ride mode");
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        buttonHelpMode.invalidate();
                        break;
                    }
                }
                return true;
            }
            /*public void onClick(View v) {
               SendEUCCommand.beep();
            }*/
        });

        //the action code for help button in settings
        final ImageButton buttonHelpAlignment = (ImageButton) findViewById(R.id.detailsAlignment);
        buttonHelpAlignment.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        buttonHelpAlignment.getDrawable().setColorFilter(getResources().getColor(R.color.colorIconPressed), PorterDuff.Mode.SRC_ATOP);
                        buttonHelpAlignment.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonHelpAlignment.getDrawable().setColorFilter(getResources().getColor(R.color.colorIcon), PorterDuff.Mode.SRC_ATOP);
                        builder.setMessage(R.string.alignmentHelp)
                                .setTitle("Upright calibration");
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        buttonHelpAlignment.invalidate();
                        break;
                    }
                }
                return true;
            }
            /*public void onClick(View v) {
               SendEUCCommand.beep();
            }*/
        });

        //the action code for help button in settings
        final ImageButton buttonHelpAlert = (ImageButton) findViewById(R.id.detailsAlert);
        buttonHelpAlert.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        buttonHelpAlert.getDrawable().setColorFilter(getResources().getColor(R.color.colorIconPressed), PorterDuff.Mode.SRC_ATOP);
                        buttonHelpAlert.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        buttonHelpAlert.getDrawable().setColorFilter(getResources().getColor(R.color.colorIcon), PorterDuff.Mode.SRC_ATOP);
                        builder.setMessage(R.string.alertHelp)
                                .setTitle("Alert");
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        buttonHelpAlert.invalidate();
                        break;
                    }
                }
                return true;
            }
            /*public void onClick(View v) {
               SendEUCCommand.beep();
            }*/
        });



        //check if in memory exist some valid address saved in the past
        // SharedPreferences memoryDevice  = getSharedPreferences("MyPrefsDevice", 0);
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


    }
    protected void onStart(){
        super.onStart();
    }
    protected void onResume(){
     super.onResume();
    }

    protected void onPause(){
        super.onPause();
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
        if (Bluetooth.isConnected)  //show information to user (name and address of device connected)
        {
            findViewById(R.id.connectedWith).setVisibility(View.VISIBLE);
            TextView textName = (TextView)findViewById(R.id.withName);
            textName.setText(device.getName());
            TextView textAddress = (TextView)findViewById(R.id.withAddress);
            textAddress.setText(device.getAddress());
        }
        else {
            findViewById(R.id.connectedWith).setVisibility(View.GONE);
        }


        Bluetooth.checkDevice();
        Bluetooth.startResearch();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

    }



    private void connectToDevice(){

        this.runOnUiThread(new Runnable() {
            public void run() {
                connectState.setVisibility(View.INVISIBLE);
                findViewById(R.id.pbHeaderProgressConnect).setVisibility(View.VISIBLE);
            }
        });
       this.connectionThread = new Thread() {
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


                       IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                       registerReceiver(mReceiver, filter2);
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
            settings.setVisibility(View.INVISIBLE);
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
            settings.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle("Advanced");

        }

        if (id == R.id.ic_menu_bluetooth){
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.VISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.INVISIBLE);
            settings.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle("Connect Unicycle");
            showEnableDevices();
        }


        if (id == R.id.ic_menu_settings){
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.INVISIBLE);
            advanced.setVisibility(View.INVISIBLE);
            settings.setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle("Settings");

        }

        if (id == R.id.ic_menu_about){
            main.setVisibility(View.INVISIBLE);
            bluetooth.setVisibility(View.INVISIBLE);
            about.setVisibility(View.VISIBLE);
            advanced.setVisibility(View.INVISIBLE);
            settings.setVisibility(View.INVISIBLE);
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
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                connectionThread.interrupt();

                connectionThread = null;

                Bluetooth.checkDevice();
               // device= Bluetooth.getDevice(addressInMemory);
                connectToDevice();
            }


            if (arrayList.isEmpty()) {
                Toast.makeText(getBaseContext(), "No devices found", Toast.LENGTH_LONG).show();
            }

        }

    };

}
