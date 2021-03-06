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

public class Change {

    public static void main(String[] args) {
        JetConnection connection = new WebsocketJetConnection("ws://cjet-raspi");
        Peer peer = new JetPeer(connection);
        JetHandler handler = new JetHandler(peer);
        peer.connect(handler, 5000);

        try {
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(Change.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            peer.close();
        } catch (IOException ex) {
            Logger.getLogger(Change.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

class JetHandler implements ConnectionCompleted, ResponseCallback, StateCallback {

    private final Peer peer;
    private static final String state = "theState";

    JetHandler(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void completed(boolean success) {
        if (success) {
            Logger.getLogger(Change.class.getName()).log(Level.INFO, "Change Connection completed!");
            JsonPrimitive value = new JsonPrimitive(42);
            peer.addState(state, value, this, 1000, this, 5000);
        } else {
            Logger.getLogger(Change.class.getName()).log(Level.SEVERE, "Connection failed!");
        }
    }

    @Override
    public void onResponse(boolean completed, JsonObject response) {
        Logger.getLogger(Change.class.getName()).log(Level.INFO, "completed: {0} response: {1}", new Object[]{completed, response});
        int initialValue = 55;
        ChangeResponder changeResponder = new ChangeResponder(peer, state, initialValue);
        JsonPrimitive counter = new JsonPrimitive(initialValue);
        peer.change(state, counter, changeResponder, 5000);
    }

    @Override
    public JsonElement onStateSet(String path, JsonElement value) throws JsonRpcException {
        try {
            int newValue = value.getAsInt();
            if (newValue % 10 != 0) {
                newValue = ((newValue + 5) / 10) * 10;
                return new JsonPrimitive(newValue);
            } else {
                return null;
            }
        } catch (ClassCastException | IllegalStateException e) {
            throw new JsonRpcException(JsonRpcException.INVALID_PARAMS, e.getMessage());
        }
    }
}

class ChangeResponder implements ResponseCallback {

    private final Peer peer;
    private final String state;
    private int counter;
    private final int initialValue;

    ChangeResponder(Peer peer, String state, int initialValue) {
        this.peer = peer;
        this.state = state;
        this.initialValue = initialValue;
        this.counter = initialValue;
    }

    @Override
    public void onResponse(boolean completed, JsonObject response) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ChangeResponder.class.getName()).log(Level.SEVERE, null, ex);
        }

        counter++;
        if (counter < initialValue + 10) {
            JsonPrimitive jsonCounter = new JsonPrimitive(counter);
            peer.change(state, jsonCounter, this, 5000);
        } else {
            this.peer.removeState(state, null, 5000);
        }
    }
}
