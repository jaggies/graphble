package net.hackdog.graphble;

import java.lang.String;

interface IBleServiceCallback {
    // These roughly follow methods in BluetoothGattCallback.
    void onCharacteristiChanged(String uuid, in byte[] data);
    void onConnectionStateChange(int status, int newState);
    void onServicesDiscovered(int status);
    void onCharacteristicRead(String uuid, int status);
    void onReadRemoteRssi(int rssi, int status);
    void onMtuChanged(int mtu, int status);
}
