package com.hbm.devices.jet;

import java.util.Observable;

public abstract class JetConnection extends Observable {
    abstract void connect(final ConnectionCompleted completed, int timeoutMs);
    abstract void disconnect();
    abstract void sendMessage(String message);
}
