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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hbm.devices.jet.ConnectionCompleted;
import com.hbm.devices.jet.JetConnection;
import com.hbm.devices.jet.JetPeer;
import com.hbm.devices.jet.JsonRpcException;
import com.hbm.devices.jet.Peer;
import com.hbm.devices.jet.ResponseCallback;
import com.hbm.devices.jet.StateCallback;
import com.hbm.devices.jet.WebsocketJetConnection;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

public class Passwd {

    public static void main(String[] args) {
        JetConnection connection = new WebsocketJetConnection("ws://localhost:11123/api/jet/");
        Peer peer = new JetPeer(connection);
        ConnectionHandler handler = new ConnectionHandler(peer);
        peer.connect(handler, 5000);

        try {
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(Passwd.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            peer.close();
        } catch (IOException ex) {
            Logger.getLogger(Passwd.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class PasswdHandler implements ResponseCallback {

    @Override
    public void onResponse(boolean completed, JsonObject response) {
        if (completed) {
            Logger.getLogger(Passwd.class.getName()).log(Level.INFO, "completed: {0} response: {1}", new Object[]{completed, response});            
        } else {
            Logger.getLogger(Passwd.class.getName()).log(Level.SEVERE, "change password failed!");
        }
    }
}

class AuthHandler implements ResponseCallback {
    private final Peer peer;
    
    AuthHandler(final Peer peer) {
        this.peer = peer;
    }

    @Override
    public void onResponse(boolean completed, JsonObject response) {
        if (completed) {
            Logger.getLogger(Passwd.class.getName()).log(Level.INFO, "Authentication completed!");
            PasswdHandler stateHandler = new PasswdHandler();
            peer.passwd("bob", "bobby", stateHandler, 5000);
        } else {
            Logger.getLogger(Passwd.class.getName()).log(Level.SEVERE, "Authentication failed!");
        }
    }
}

class ConfigHandler implements ResponseCallback {
    private final Peer peer;
    
    ConfigHandler(final Peer peer) {
        this.peer = peer;
    }

    @Override
    public void onResponse(boolean completed, JsonObject response) {
        if (completed) {
            Logger.getLogger(Passwd.class.getName()).log(Level.INFO, "Passwd Config completed!");
            AuthHandler authHandler = new AuthHandler(peer);
            peer.authenticate("john-admin", "doe", authHandler, 5000);
        } else {
            Logger.getLogger(Passwd.class.getName()).log(Level.SEVERE, "Passwd Config failed!");
        }
    }
}

class ConnectionHandler implements ConnectionCompleted {
    private final Peer peer;

    ConnectionHandler(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void completed(boolean success) {
        if (success) {
            Logger.getLogger(Passwd.class.getName()).log(Level.INFO, "Passwd Connection completed!");
            ConfigHandler configHandler = new ConfigHandler(peer);
            peer.config(Passwd.class.getName(), configHandler, 5000);
        } else {
            Logger.getLogger(Passwd.class.getName()).log(Level.SEVERE, "Connection failed!");
        }
    }
}
