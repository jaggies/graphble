package net.hackdog.minimalble;

import android.bluetooth.BluetoothGattCallback;

import java.util.UUID;
/**
 * Created by jmiller on 8/1/16.
 */
public interface BleServiceCallback {
    public void onCharacteristicChanged(UUID charUuid, byte[] data);
    public void onCharacteristicRead(UUID charUuid, byte[] data);
}
