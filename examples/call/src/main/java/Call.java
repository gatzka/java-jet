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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hbm.devices.jet.ConnectionCompleted;
import com.hbm.devices.jet.JetConnection;
import com.hbm.devices.jet.JetPeer;
import com.hbm.devices.jet.Peer;
import com.hbm.devices.jet.ResponseCallback;
import com.hbm.devices.jet.WebsocketJetConnection;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

public class Call {

    public static void main(String[] args) {
        JetConnection connection = new WebsocketJetConnection("ws://localhost:11123/api/jet/");
        Peer peer = new JetPeer(connection);
        JetHandler handler = new JetHandler(peer);
        peer.connect(handler, 5000);

        try {
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(Call.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            peer.close();
        } catch (IOException ex) {
            Logger.getLogger(Call.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class JetHandler implements ConnectionCompleted, ResponseCallback {

    private final Peer peer;

    JetHandler(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void completed(boolean success) {
        if (success) {
            Logger.getLogger(Call.class.getName()).log(Level.INFO, "Call Connection completed!");
            JsonPrimitive value = new JsonPrimitive(42);
            peer.call("theMethod", value, this, 5000);
        } else {
            Logger.getLogger(Call.class.getName()).log(Level.SEVERE, "Call Connection failed!");
        }
    }

    @Override
    public void onResponse(boolean completed, JsonObject response) {
        Logger.getLogger(Call.class.getName()).log(Level.INFO, "completed: {0} response: {1}", new Object[]{completed, response});
    }
}
