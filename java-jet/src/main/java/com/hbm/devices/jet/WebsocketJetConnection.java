package com.hbm.devices.jet;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketCloseCode;
import com.neovisionaries.ws.client.WebSocketFactory;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

public class WebsocketJetConnection implements JetConnection {

    private final String url;
    private WebSocket ws;
    private SSLContext context;
    private ConnectionCompleted completed;

    public WebsocketJetConnection(final String url, final SSLContext sslContext) {
        this(url);
        this.context = sslContext;
    }
    
    public WebsocketJetConnection(final String url) {
        this.url = url;
    }

    @Override
    public void connect(final ConnectionCompleted completed, int timeoutMs) {
        this.completed = completed;
        try {
            WebSocketFactory factory = new WebSocketFactory();

            if (context != null) {
                factory.setSSLContext(context);
            }
            ws = factory.createSocket(url, timeoutMs);
            WebsocketCallbackListener listener = new WebsocketCallbackListener(this);
            ws.addListener(listener);
            ws.addProtocol("jet");
            ws.connectAsynchronously();
        } catch (IOException ex) {
            Logger.getLogger(WebsocketJetConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void disconnect() {
        ws.disconnect(WebSocketCloseCode.AWAY, "Closing Java-Jet-Peer");
    }
    
    void connectCompleted(boolean success) {
        this.completed.completed(success);
    }
}
