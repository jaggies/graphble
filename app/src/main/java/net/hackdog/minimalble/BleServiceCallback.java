package net.hackdog.minimalble;

import android.bluetooth.BluetoothGattCallback;

import java.util.UUID;
/**
 * Created by jmiller on 8/1/16.
 */
public interface BleServiceCallback {
    // Connection states
    public static final int STATE_CONNECTING = 0;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_DISCONNECTED = 2;
    public static final int STATE_SCANNING = 3;
    public static final int STATE_SCAN_COMPLETE = 4;
    public static final int STATE_SCAN_FAILED = 5;

    public void onCharacteristicChanged(UUID charUuid, byte[] data);
    public void onCharacteristicRead(UUID charUuid, byte[] data);
    public void onConnectionStateChanged(int state);
}
