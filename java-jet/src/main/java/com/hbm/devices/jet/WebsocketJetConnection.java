/*
 * The MIT License
 *
 * Copyright 2016 Hottinger Baldwin Messtechnik GmbH.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.hbm.devices.jet;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketCloseCode;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

public class WebsocketJetConnection extends JetConnection {

    private final String url;
    private WebSocket ws;
    private SSLContext context;
    private ConnectionCompleted completed;
    private boolean connected;

    public WebsocketJetConnection(final String url, final SSLContext sslContext) {
        this(url);
        this.context = sslContext;
    }

    public WebsocketJetConnection(final String url) {
        this.url = url;
        this.connected = false;
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
        this.connected = false;
        ws.disconnect(WebSocketCloseCode.AWAY, "Closing Java-Jet-Peer");
    }

    @Override
    public void sendMessage(String message) {
        ws.sendText(message);
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    void connectCompleted(boolean success) {
        this.completed.completed(success);
        if (success) {
            this.connected = true;
        }
    }

    void onTextMessage(String text) {
        setChanged();
        notifyObservers(text);
    }
}
