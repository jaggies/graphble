package net.hackdog.graphble;

import net.hackdog.graphble.IBleServiceCallback;

interface IBleService {
    void startScan();
    void stopScan();
    void registerCallback(IBleServiceCallback cb);
    void unregisterCalllback(IBleServiceCallback cb);
}
