package comegg_electric_unicycle.github.eggunicycle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;


public class SplashScreen extends AppCompatActivity {
    // private static int SPLASH_TIME_OUT = 3000;
    String device;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Show the splash screen
        setContentView(R.layout.splash_screen);
        new PrefetchData().execute();
    }

    private class PrefetchData extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // before making http calls

        }

        @Override
        protected Void doInBackground(Void... arg0) {

            SharedPreferences memoryDevice  = getSharedPreferences("MyPrefsDevice", 0);
            device = memoryDevice.getString("deviceAdress", null);



            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // After completing http call
            // will close this activity and lauch main activity
            Intent i = new Intent(SplashScreen.this, MainActivity.class);
            i.putExtra("device", device);
            startActivity(i);

            // close this activity
            finish();
        }

    }
}