package net.hackdog.minimalble;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_ENABLE_BT = 101;
    private static final String TAG = MainActivity.class.getSimpleName();
    final int CHANNEL_IDS[] = {R.id.channel0, R.id.channel1, R.id.channel2, R.id.channel3 };
    private GraphView mChannels[] = new GraphView[CHANNEL_IDS.length];
    BleService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for (int i = 0; i < CHANNEL_IDS.length; i++) {
            mChannels[i] = (GraphView) findViewById(CHANNEL_IDS[i]);
            mChannels[i].setLabel("Channel " + i);
            mChannels[i].setValue("off");
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setVisibility(View.GONE);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        fab.setVisibility(View.GONE);

        if (savedInstanceState == null) {
            checkPermissions();
        } else {
            startService();
        }
    }

    void startService() {
        Log.v(TAG, "Starting service");
        Intent intent = new Intent(this, BleService.class);
        if (!bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.w(TAG, "Failed to start service");
        }
    }

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.v(TAG, "Service connected, yay!");
            BleService.LocalBinder binder = (BleService.LocalBinder) service;
            mService = binder.getService();
            mService.registerCallback(mBleServiceCallback);
            mService.startScan();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.v(TAG, "Service disconnected, boo!");
            mService = null;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (mService != null) {
            mService.unregisterCallback(mBleServiceCallback);
            mService.stopScan();
//            unbindService(mConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mService != null) {
            mService.startScan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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

    public void onRequestPermissionsResult(
            int requestCode, String permissions[], int[] grantResults) {
        boolean granted = true; // assume true until we find otherwise
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        boolean isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                        Log.v(TAG, "Permission " + (isGranted ? "granted" : "denied")
                                + permissions[i]);
                        if (!isGranted) granted = false;
                    }
                    if (granted) {
                        startService();
                    }
                }
            }
        }
    }

    private void checkPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH)
                || ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // show permission info dialog TODO
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS);
        }
    }

    BleServiceCallback mBleServiceCallback = new BleServiceCallback() {
        @Override
        public void onCharacteristicChanged(UUID uuid, byte[] data) {
            mChannels[0].setData(data);
        }

        @Override
        public void onCharacteristicRead(UUID uuid, byte[] data) {

        }
    };
}
