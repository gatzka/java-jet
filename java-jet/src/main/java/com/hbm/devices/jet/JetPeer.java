package com.hbm.devices.jet;

public class JetPeer implements Peer {
	
	private JetConnection connection;
	
	public JetPeer(JetConnection connection) {
		this.connection = connection;
	}

	@Override
	public void connect(ConnectionCompleted connectionCompleted, int timeoutMs) {
		this.connection.connect(connectionCompleted, timeoutMs);
	}
}
