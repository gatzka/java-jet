package com.hbm.devices.jet;

public interface Peer {
	public void connect(ConnectionCompleted connectionCompleted, int timeoutMs);
}
