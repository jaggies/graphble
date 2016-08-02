package net.hackdog.minimalble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_ENABLE_BT = 101;
    private final UUID CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final UUID CLIENT_USER_DESC = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
    private static final String TAG = MainActivity.class.getSimpleName();
    final int CHANNEL_IDS[] = {R.id.channel0, R.id.channel1, R.id.channel2, R.id.channel3 };
    private GraphView mChannels[] = new GraphView[CHANNEL_IDS.length];
    private BluetoothAdapter mBluetoothAdapter;

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
            startScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
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

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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
                        startScan();
                    }
                }
            }
        }
    }

    private void startScan() {
        if (mBluetoothAdapter == null) {
            BluetoothManager service = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = service.getAdapter();
        }
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            } else {
                ScanSettings filters;
                List<ScanFilter> filts = new ArrayList<>();
                filts.add(new ScanFilter.Builder().setDeviceName("MatchBox").build());
                ScanSettings setings = new ScanSettings.Builder()
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build();
                mBluetoothAdapter.getBluetoothLeScanner().startScan(filts, setings, mBleCallback);
            }
        }
    }

    private void stopScan() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mBleCallback);
        }
    }

    private BluetoothGatt mBluetoothGatt;


    public boolean setCharacteristicNotification(
            BluetoothDevice device, BluetoothGatt gatt,
            UUID serviceUuid, UUID characteristicUuid, boolean enable) {
        BluetoothGattCharacteristic characteristic = gatt.getService(serviceUuid).getCharacteristic(characteristicUuid);
        gatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHAR_CONFIG);
        descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : new byte[] { 0x00, 0x00 });
        return gatt.writeDescriptor(descriptor); //descriptor write operation successfully started?
    }

    ScanCallback mBleCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.v(TAG, "onScanResult(type=" + callbackType + ", result=" + result);
            switch (callbackType) {
                case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                case ScanSettings.CALLBACK_TYPE_MATCH_LOST: {
                    BluetoothDevice device = result.getDevice();
                    int rssi = result.getRssi();
                    List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();
                    String name = result.getScanRecord().getDeviceName();
                    Log.v(TAG, "Found " + name + ", rssi=" + rssi);
                    mBluetoothGatt = device.connectGatt(MainActivity.this, true, mGattCallback);
                }
                break;
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.v(TAG, "onBatchScanResults(res=" + results + ")");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.v(TAG, "onScanFailed: " + errorCode);
        }
    };

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

    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public boolean mConnected;

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                boolean started = mBluetoothGatt.discoverServices();
                Log.i(TAG, "Gatt connected; attempting service discovery, started = " + started);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnected = false;
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattCharacteristic> chrs = new ArrayList<>();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "onServicesDiscovered(status=" + status + ")");
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    Log.v(TAG, "\tService:" + service.getUuid());
                    List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                    for (BluetoothGattCharacteristic chr : chars) {
                        chrs.add(chr);
                        Log.v(TAG, "\t\tCharacteristic:" + chr.getUuid()
                                + " writetype:" + chr.getWriteType()
                                + " properties:" + Integer.toHexString(chr.getProperties())
                                + " userDesc:" + chr.getDescriptor(CLIENT_USER_DESC)); // TODO
                    }
                }
            }
            for (BluetoothGattCharacteristic chr : chrs) {
                if (0 != (chr.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                    enableNotification(gatt, chr, true);
                }
            }
        }

        List<Runnable> workQueue = new ArrayList<>();

        private void enableNotification(final BluetoothGatt gatt, BluetoothGattCharacteristic chr,
                                        boolean enable) {
            gatt.setCharacteristicNotification(chr, enable);
            final BluetoothGattDescriptor desc = chr.getDescriptor(CLIENT_CHAR_CONFIG);
            if (desc != null) {
                // Total hack to work around android bug.
                // See https://code.google.com/p/android/issues/detail?id=150933
                //chr.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                desc.setValue(enable ?
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    // Assume descriptor is busy and do it later.
                    Log.v(TAG, "delay write GATT descriptor for + "
                            + desc.getCharacteristic().getUuid());
                    synchronized (workQueue) {
                        workQueue.add(new Runnable() {
                            @Override
                            public void run() {
                                if (!gatt.writeDescriptor(desc)) {
                                    Log.v(TAG, "Couldn't write desc for chr "
                                            + desc.getCharacteristic().getUuid());
                                }
                            }
                        });
                    }
                } else {
                    Log.v(TAG, "wrote GATT descriptor for + " + desc.getCharacteristic().getUuid());
                }
            } else {
                Log.v(TAG, "No descriptor for UUID " + chr.getUuid());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.v(TAG, "onCharacteristicRead(uuid= " + characteristic.getUuid() + ", value="
                    + characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //Log.v(TAG, "onCharacteristicChanged(): " + characteristic.getValue().toString());
            byte[] data = characteristic.getValue();
            final byte samples[] = Arrays.copyOf(data, data.length);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mChannels[0].setData(samples);
                }
            });
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorRead() : " + descriptor.getCharacteristic().getUuid());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.v(TAG, "onDescriptorWrite " + descriptor.getCharacteristic().getUuid()
                    + " status:" + status);
            synchronized (workQueue) {
                if (workQueue.size() > 0) {
                    Runnable runnable = workQueue.get(workQueue.size() - 1);
                    workQueue.remove(runnable);
                    runnable.run();
                }
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.v(TAG, "onReadRemoteRssi(rssi=" + rssi + ", status=" + status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };
}
