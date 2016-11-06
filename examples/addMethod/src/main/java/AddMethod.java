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
import com.hbm.devices.jet.MethodCallback;
import com.hbm.devices.jet.NaiveSSLContext;
import com.hbm.devices.jet.Peer;
import com.hbm.devices.jet.ResponseCallback;
import com.hbm.devices.jet.WebsocketJetConnection;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

public class AddMethod {

    public static void main(String[] args) {
        try {
            SSLContext context = NaiveSSLContext.getInstance("TLS");
            JetConnection connection = new WebsocketJetConnection("ws://cjet-raspi", context);
            Peer peer = new JetPeer(connection);
            JetHandler handler = new JetHandler(peer);
            peer.connect(handler, 5000);

            try {
                System.in.read();
            } catch (IOException ex) {
                Logger.getLogger(AddMethod.class.getName()).log(Level.SEVERE, null, ex);
            }
            peer.disconnect();
            try {
                peer.close();
            } catch (IOException ex) {
                Logger.getLogger(AddMethod.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(AddMethod.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class JetHandler implements ConnectionCompleted, ResponseCallback, MethodCallback {

    private final Peer peer;

    JetHandler(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void completed(boolean success) {
        if (success) {
            Logger.getLogger(AddMethod.class.getName()).log(Level.INFO, "AddMethod Connection completed!");
            peer.addMethod("theMethod", this, 1000000, this, 500000);
        } else {
            Logger.getLogger(AddMethod.class.getName()).log(Level.SEVERE, "Connection failed!");
        }
    }

    @Override
    public void onResponse(boolean completed, JsonObject response) {
        Logger.getLogger(AddMethod.class.getName()).log(Level.INFO, "completed: {0} response: {1}", new Object[]{completed, response});
    }

    @Override
    public JsonElement onMethodCalled(String path, JsonElement value) throws JsonRpcException {
        try {
            int newValue = value.getAsInt();
            Logger.getLogger(AddMethod.class.getName()).log(Level.INFO, "Method called with: {0}", newValue);
            newValue++;
            return new JsonPrimitive(newValue);
        } catch (ClassCastException | IllegalStateException e) {
            throw new JsonRpcException(JsonRpcException.INVALID_PARAMS, e.getMessage());
        }
    }
}
