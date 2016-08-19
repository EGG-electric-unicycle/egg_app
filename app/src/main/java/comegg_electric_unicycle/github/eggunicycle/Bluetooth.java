package comegg_electric_unicycle.github.eggunicycle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Vector;



/**
 * Created by paleixo on 17-08-2016.
 */
public class Bluetooth {



    Activity myMainActivity; //variable to set the context of main activity
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket;

    private static OutputStream outputStream;
    private static InputStream inputStream;
    public static boolean isConnected = false;


    public Bluetooth(Activity mainActivity) { //constructor
        super();
        this.myMainActivity=mainActivity;
    }

    public void cancelResearch(){
        mBluetoothAdapter.cancelDiscovery();
    }
    public void startResearch(){
        mBluetoothAdapter.startDiscovery();
    }
    public void closeInputStream(){
        try {
            inputStream.close();
        }catch (IOException a){}
    }
    public void closeOutputStream(){
        try {
            outputStream.close();
        }catch (IOException a){}
    }


    public BluetoothDevice getDevice(String address){
        return MainActivity.device = mBluetoothAdapter.getRemoteDevice(address);

    }

    public void checkDevice(){

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) { //check if mobile device have a Bluetooth module
            Toast.makeText(myMainActivity.getApplicationContext(), "Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {  //check if Bluetooth is on. If it is off, ask user if want turn on.
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            myMainActivity.startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void connectSocket(){
        BluetoothSocket tmp = null;


        try {
            tmp = MainActivity.device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = tmp;

        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            socket.connect();
            isConnected = true;
        } catch (IOException e) {
            //connection to device failed so close the socket
            disconnectSocket();

        }
        if (isConnected) {
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                myMainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(myMainActivity.getApplicationContext(), "Failed: socket.getInputStream();", Toast.LENGTH_LONG).show();
                    }
                });
            }

            try {
               outputStream = socket.getOutputStream();
            } catch (IOException e) {
                myMainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(myMainActivity.getApplicationContext(), "Failed: socket.getOutputStream();", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }


    }

    public void disconnectSocket()
    {
        try {
            isConnected = false;
            socket.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }

    }



    public void readData(){
        final Vector<Byte> serialData = new Vector<Byte>();
        MainActivity.stopThread = false;
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !MainActivity.stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            final byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);

                            // store all received bytes on vector
                            for (int i = 0; i < byteCount; ) {
                                serialData.add(rawBytes[i]);
                                i++;
                            }
                        }

                        ElectricUnicycle.processData(serialData);
                    }
                    catch (IOException ex)
                    {
                        MainActivity.stopThread = true;
                    }
                }
            }
        });

        thread.start();

    }

    public static void writeByte(int _byte) {
        if (isConnected) {
            try {
                outputStream.write((byte) _byte);
            }
            catch (IOException a) {
            }
        }
    }

}
