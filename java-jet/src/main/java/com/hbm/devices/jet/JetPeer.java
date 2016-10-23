package com.hbm.devices.jet;

public class JetPeer implements Peer {
	
	private JetConnection connection;
	
	public JetPeer(JetConnection connection) {
		this.connection = connection;
	}

	@Override
	public void connect(int timeoutMs) {
		this.connection.connect(timeoutMs);
	}
}
