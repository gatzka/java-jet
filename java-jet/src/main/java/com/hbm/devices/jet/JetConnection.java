package com.hbm.devices.jet;

public interface JetConnection {
    void connect(final ConnectionCompleted completed, int timeoutMs);
    void disconnect();
}
