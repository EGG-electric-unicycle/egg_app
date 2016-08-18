/**
 * Created by casainho on 12-08-2016.
 */

package comegg_electric_unicycle.github.eggunicycle;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import java.util.Vector;

public class ElectricUnicycle {

    public static Activity myMainActivity;
    public static double speed_, current_, voltage_, trip_, temperature_;
    public static int state=0;
    public static int speed=0, voltage=0, trip=0,current=0, temperature=0;
    public enum rideModes {SOFT, COMFORT, MADDEN};
    public static TextView speedView, voltageView ,tripView ,currentView ,tempView ,chargeView, chargeViewAdvanced,speedViewAdvanced ,tripViewAdvanced;
    
    public ElectricUnicycle(Activity mainActivity) { //constructor
        super();
        this.myMainActivity=mainActivity;
    }

    public void SendCommand(int _byte) {
        Bluetooth.writeByte((byte) _byte);
    }

    // make a small delay between each command
    public void CommandDelay() {
        try {
            Thread.sleep(500);
        } catch(InterruptedException ex) {

        }
    }

    public void beep() {
        SendCommand('b');
    }

    public void setRideMode(rideModes rideMode) {
        if (rideMode == rideModes.SOFT) {
            SendCommand('s'); CommandDelay();
            SendCommand('b');
        } else if (rideMode == rideModes.COMFORT) {
            SendCommand('f'); CommandDelay();
            SendCommand('b');
        } else if (rideMode == rideModes.MADDEN) {
            SendCommand('h'); CommandDelay();
            SendCommand('b');
        }
    }

    public void setHorizontalCalibration() {
        SendCommand(','); CommandDelay();
        SendCommand('c'); CommandDelay();
        SendCommand('y'); CommandDelay();
        SendCommand('c'); CommandDelay();
        SendCommand('y'); CommandDelay();
        SendCommand('c'); CommandDelay();
        SendCommand('y');
    }

    public static void processData(Vector<Byte> dataVector) {
        speedView=  (TextView) myMainActivity.findViewById(R.id.showSpeed);
        voltageView = (TextView) myMainActivity.findViewById(R.id.showVoltage);
        tripView = (TextView) myMainActivity.findViewById(R.id.showTrip);
        currentView = (TextView) myMainActivity.findViewById(R.id.showCurrent);
        tempView = (TextView) myMainActivity.findViewById(R.id.showTemp);
        chargeView=(TextView) myMainActivity.findViewById(R.id.showCharge);
        chargeViewAdvanced = (TextView) myMainActivity.findViewById(R.id.showChargeMore) ;
        speedViewAdvanced = (TextView) myMainActivity.findViewById(R.id.showSpeedMore);
        tripViewAdvanced = (TextView) myMainActivity.findViewById(R.id.showTripMore);
        
        while  (!dataVector.isEmpty())
        {
            Byte data1 = dataVector.get(0);
            int data = (int) data1;
            dataVector.removeElementAt(0);
            if (data < 0) data += 256; // need to convert data from "int" to "byte" type

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
                    voltage = (data <<8);
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
                    if (speed > (65536/2)) // convert data from "int" to "unsigned int"
                        speed = speed - 65536;
                    if (speed < 0)
                        speed= speed * (-1); // necessary to set positive value of speed when it is negative
                    speed_ = speed * 0.036;
                    break;

                // next 4 bytes are trip distance
                case 11:
                    state++;
                    trip = (data << 24);
                    break;

                case 12:
                    state++;
                    trip |= (data << 16);
                    break;

                case 13:
                    state++;
                    trip |= (data << 8);
                    break;

                case 14:
                    state++;
                    trip |= data;
                    trip_ = trip/1000.0;
                    break;

                // next 2 bytes are good current
                case 15:
                    state++;
                    current = (data << 8);
                    break;

                case 16:
                    state++;
                    current |= data;
                    if (current > (65536 / 2))  // convert data from "int" to "unsigned int"
                        current = current - 65536;
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

                    if (temperature > (65536 / 2))  // convert data from "int" to "unsigned int"
                        temperature = temperature - 65536;
                    temperature_ = (temperature/340.0) + 36.53;
                    break;

                // unknown
                case 19:
                    if (data == 0) {
                        state++;
                    } else state = 0;
                    break;

                // unknown
                case 20:
                    state++; // I have seen data to equal 0, 1 and 2. In one 30B4 board,
                    // this value is always 0 while on the other can be 1 or 2 at least.
                    break;

                // END sequence
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
                        // here we consider that we got a complete data sequence so we can process it
                        state = 0;
                        myMainActivity.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                if (MainActivity.main.getVisibility()== View.VISIBLE) {
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

    // returns the battery charge state
    public static int calcChargePercent(double v)
    {
        if (v>=0 && v<52.1)     return 0;
        if (v>=52.1 && v<53.1)  return 10;
        if (v>=53.1 && v< 54.2) return 20;
        if (v>=54.2 && v< 56.4) return 30;
        if (v>=56.4 && v<57.9)  return 40;
        if (v>=57.9 && v< 59.3) return 50;
        if (v>=59.3 && v<60.7)  return 60;
        if (v>=60.7 && v<61.9)  return 70;
        if (v>=61.9 && v<63.6)  return 80;
        if (v>=63.6 && v<65.0)  return 90;
        if (v>=65.0)            return 100;

        return 0; // should not get here
    }
}
