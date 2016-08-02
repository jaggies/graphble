package net.hackdog.minimalble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BleService extends Service {
    private static final boolean DEBUG = MainActivity.DEBUG;
    private final UUID CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final UUID CLIENT_USER_DESC = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");

    private static final String TAG = BleService.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BleServiceCallback mBleServiceCallback;
    private LocalBinder mBinder = new LocalBinder();
    private boolean mIsConnected;
    private boolean mIsScanning;

    public class LocalBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.v(TAG, "Service started!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public boolean isScanning() {
        return mIsScanning;
    }

    public void startScan(String name) {
        if (mBluetoothAdapter == null) {
            BluetoothManager service = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = service.getAdapter();
        }
        if (mBluetoothAdapter != null) {
            if (!mBluetoothAdapter.isEnabled()) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(enableBtIntent); // can't get result
                    Log.w(TAG, "Bluetooth not enabled!!");
                }
            } else {
                ScanSettings filters;
                List<ScanFilter> filts = new ArrayList<>();
                filts.add(new ScanFilter.Builder()
                        .setDeviceName(name).build());
                ScanSettings setings = new ScanSettings.Builder()
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build();
                mIsScanning = true;
                mBluetoothAdapter.getBluetoothLeScanner().startScan(filts, setings, mScanCallback);
            }
        }
    }

    public void stopScan() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
        }
        mIsScanning = false;
    }

    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (DEBUG) Log.v(TAG, "onScanResult(type=" + callbackType + ", result=" + result);
            switch (callbackType) {
                case ScanSettings.CALLBACK_TYPE_ALL_MATCHES:
                case ScanSettings.CALLBACK_TYPE_FIRST_MATCH:
                case ScanSettings.CALLBACK_TYPE_MATCH_LOST: {
                    BluetoothDevice device = result.getDevice();
                    int rssi = result.getRssi();
                    List<ParcelUuid> serviceUuids = result.getScanRecord().getServiceUuids();
                    String name = result.getScanRecord().getDeviceName();
                    if (DEBUG) Log.v(TAG, "Found " + name + ", rssi=" + rssi);
                    mBluetoothGatt = device.connectGatt(BleService.this, true, mGattCallback);
                }
                break;
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (DEBUG) Log.v(TAG, "onBatchScanResults(res=" + results + ")");
            mIsScanning = false;
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            if (DEBUG) Log.v(TAG, "onScanFailed: " + errorCode);
            mIsScanning = false;
        }
    };

    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mIsConnected = true;
                boolean started = mBluetoothGatt.discoverServices();
                Log.i(TAG, "Gatt connected; attempting service discovery, started = " + started);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mIsConnected = false;
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattCharacteristic> chrs = new ArrayList<>();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (DEBUG) Log.v(TAG, "onServicesDiscovered(status=" + status + ")");
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    if (DEBUG) Log.v(TAG, "\tService:" + service.getUuid());
                    List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
                    for (BluetoothGattCharacteristic chr : chars) {
                        chrs.add(chr);
                        if (DEBUG) Log.v(TAG, "\t\tCharacteristic:" + chr.getUuid()
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
                    if (DEBUG) Log.v(TAG, "delay write GATT descriptor for + "
                            + desc.getCharacteristic().getUuid());
                    synchronized (workQueue) {
                        workQueue.add(new Runnable() {
                            @Override
                            public void run() {
                                if (!gatt.writeDescriptor(desc)) {
                                    if (DEBUG) Log.v(TAG, "Couldn't write desc for chr "
                                            + desc.getCharacteristic().getUuid());
                                }
                            }
                        });
                    }
                } else {
                    if (DEBUG) Log.v(TAG, "wrote GATT descriptor for + " + desc.getCharacteristic().getUuid());
                }
            } else {
                if (DEBUG) Log.v(TAG, "No descriptor for UUID " + chr.getUuid());
            }
        }

        @Override
        public void onCharacteristicRead(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (mBleServiceCallback != null) {
                mBleServiceCallback.onCharacteristicRead(
                        characteristic.getUuid(), characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (mBleServiceCallback != null) {
                mBleServiceCallback.onCharacteristicChanged(
                        characteristic.getUuid(), characteristic.getValue());
            }
        }

        @Override
        public void onDescriptorRead(
                BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            if (DEBUG) Log.v(TAG, "onDescriptorRead() : " + descriptor.getCharacteristic().getUuid());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (DEBUG) Log.v(TAG, "onDescriptorWrite " + descriptor.getCharacteristic().getUuid()
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
            if (DEBUG) Log.v(TAG, "onReadRemoteRssi(rssi=" + rssi + ", status=" + status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    public void unregisterCallback(BleServiceCallback cb) {
        Log.v(TAG, "callback unregistered " + cb + " service = " + this);
        mBleServiceCallback = null;
    }

    public void registerCallback(BleServiceCallback cb) {
        // TODO: Maybe have multiple callbacks
        Log.v(TAG, "callback registered " + cb + " service = " + this);
        mBleServiceCallback = cb;
    }
}
