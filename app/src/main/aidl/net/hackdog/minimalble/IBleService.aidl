package net.hackdog.minimalble;

import net.hackdog.minimalble.IBleServiceCallback;

interface IBleService {
    void startScan();
    void stopScan();
    void registerCallback(IBleServiceCallback cb);
    void unregisterCalllback(IBleServiceCallback cb);
}
