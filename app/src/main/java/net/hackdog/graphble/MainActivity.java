package net.hackdog.graphble;

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
import android.widget.Button;

import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final boolean DEBUG = false;
    private static final String MATCHBOX = "MatchBox";
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_ENABLE_BT = 101;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CHANNEL_IDS[] =
            { R.id.channel0, R.id.channel1, R.id.channel2, R.id.channel3 };
    private final static HashMap<String, Integer> mChannelMap;
    private Intent SERVICE_INTENT;
    private GraphView mChannels[] = new GraphView[CHANNEL_IDS.length];
    private Button mScanButton;
    BleService mService;

    static {
        mChannelMap = new HashMap<>();
        mChannelMap.put("6ff90100-b2be-4f02-bbd8-e795ca3ca70c", 0);
        mChannelMap.put("6ff90101-b2be-4f02-bbd8-e795ca3ca70c", 1);
        mChannelMap.put("6ff90102-b2be-4f02-bbd8-e795ca3ca70c", 2);
        mChannelMap.put("6ff90103-b2be-4f02-bbd8-e795ca3ca70c", 3);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for (int i = 0; i < CHANNEL_IDS.length; i++) {
            mChannels[i] = (GraphView) findViewById(CHANNEL_IDS[i]);
            mChannels[i].setLabel("Channel " + i);
            mChannels[i].setValue("off");
        }

        mScanButton = (Button) findViewById(R.id.scan_button);
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

        SERVICE_INTENT = new Intent(this, BleService.class);

        if (savedInstanceState == null) {
            // Check permissions before starting
            checkPermissions();
        } else {
            startService();
        }
    }

    void startService() {
        if (DEBUG) Log.v(TAG, "Starting service");
        // Keep the service running during device rotations
        startService(SERVICE_INTENT);

        // Bind to it
        if (!bindService(SERVICE_INTENT, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.w(TAG, "Failed to start service");
        }
    }

    // TODO: stop service when we leave the activity

    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if (DEBUG) Log.v(TAG, "Service connected, yay!");
            BleService.LocalBinder binder = (BleService.LocalBinder) service;
            mService = binder.getService();
            mService.registerCallback(mBleServiceCallback);
            maybeStartScanning();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (DEBUG) Log.v(TAG, "Service disconnected, boo!");
            mService = null;
        }
    };

    private void maybeStartScanning() {
        if (!mService.isConnected() && !mService.isScanning()) {
            mService.startScan(MATCHBOX);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mService != null) {
            mService.unregisterCallback(mBleServiceCallback);
            if (mService.isScanning()) {
                mService.stopScan();
            }
            unbindService(mConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();
    }

    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            // Don't leave the service running
            //stopService(SERVICE_INTENT);
            //finish();
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
                        if (DEBUG) Log.v(TAG, "Permission " + (isGranted ? "granted" : "denied")
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
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSIONS);
        }
    }

    final int HISTORY = 1000;
    byte[][] mData = {null, null, null, null};
    int mIndex[] = {0,0,0,0};

    BleServiceCallback mBleServiceCallback = new BleServiceCallback() {
        @Override
        public void onCharacteristicChanged(UUID uuid, byte[] data) {
            String sUuid = uuid.toString();
            if (mChannelMap.containsKey(sUuid)) {
                int chan = mChannelMap.get(sUuid);
                if (mData[chan] == null) {
                    mData[chan] = new byte[HISTORY];
                }
                for (int i = 2; i < data.length; i++) {
                    mData[chan][(mIndex[chan]++)%HISTORY] = data[i];
//                    if (mIndex[chan] >= HISTORY) {
//                        mIndex[chan] = 0;
//                    }
                }
                mChannels[chan].setData(mData[chan]);
            } else {
                // probably settings
                Log.w(TAG, "not handling uuid " + uuid.toString());
            }
        }

        @Override
        public void onCharacteristicRead(UUID uuid, byte[] data) {

        }

        @Override
        public void onConnectionStateChanged(int state) {
            final String msg;
            final boolean enabled;

            switch (state) {
                case STATE_CONNECTING:
                    msg = "Connecting...";
                    enabled = false;
                    break;
                case STATE_CONNECTED:
                    msg = "Connected";
                    enabled = false;
                    break;
                case STATE_DISCONNECTED:
                    msg = "Disconnected";
                    enabled = true;
                    break;
                case STATE_SCANNING:
                    msg = "Scanning...";
                    enabled = false;
                    break;
                case STATE_SCAN_COMPLETE:
                    msg = "Complete";
                    enabled = false;
                    break;
                case STATE_SCAN_FAILED:
                    msg = "Scan";
                    enabled = true;
                    break;
                default:
                    msg = "";
                    enabled = true;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mScanButton.setText(msg);
                    mScanButton.setEnabled(enabled);
                }
            });
        }
    };
}
