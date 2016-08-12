/**
 * Created by casainho on 12-08-2016.
 */

package comegg_electric_unicycle.github.eggunicycle;

import java.io.IOException;
import java.io.OutputStream;

public class EUCCommands {
    public enum rideModes {SOFT, COMFORT, MADDEN};

    private boolean EUCIsConnected() {
            return((MainActivity.isConnected == true) ? true : false);
        }

    private OutputStream EUCOutputStream() {
        return MainActivity.outputStream;
    }

    public void EUCSendCommand(int _byte) {
        if (EUCIsConnected() == true) {
            try {
                EUCOutputStream().write((byte) _byte);
            }
            catch (IOException a) {
            }
        }
    }

    public void beep() {
        if (EUCIsConnected() == true) {
            EUCSendCommand('b');
        }
    }

    public void setRideMode(rideModes rideMode) {
        if (rideMode == rideModes.SOFT) {
            EUCSendCommand('s');
            EUCSendCommand('b');
        }else if (rideMode == rideModes.COMFORT) {
            EUCSendCommand('f');
            EUCSendCommand('b');
        }else if (rideMode == rideModes.MADDEN) {
            EUCSendCommand('h');
            EUCSendCommand('b');
        }
    }

    public void setHorizontalCalibration() {
        EUCSendCommand(',');
        EUCSendCommand('c');
        EUCSendCommand('y');
    }
}
