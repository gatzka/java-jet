package com.hbm.devices.jet;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

public class WebsocketJetConnection implements JetConnection {

    private String url;
    private WebSocket ws;
    private SSLContext context;

    public WebsocketJetConnection(String url, SSLContext sslContext) {
        this(url);
        this.context = sslContext;
    }
    public WebsocketJetConnection(String url) {
        this.url = url;
    }

    @Override
    public void connect(int timeoutMs) {
        try {
            WebSocketFactory factory = new WebSocketFactory();
            if (context != null) {
                factory.setSSLContext(context);
            }
            ws = factory.createSocket(url, timeoutMs);
            ws.addProtocol("jet");
            ws.connect();
        } catch (WebSocketException ex) {
            Logger.getLogger(WebsocketJetConnection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WebsocketJetConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
