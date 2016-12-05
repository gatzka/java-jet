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

import com.google.gson.JsonElement;
import java.io.Closeable;
import java.io.IOException;

public interface Peer extends Closeable {

    @Override
    public void close() throws IOException;

    public void connect(ConnectionCompleted connectionCompleted, int responseTimeoutMs);

    public boolean isConnected();

    public void config(final String peerName, ResponseCallback responseCallback, int timeoutMs);

    public void authenticate(final String user, final String password, ResponseCallback responseCallback, int timeoutMs);
    
    public FetchId fetch(Matcher matcher, FetchEventCallback callback, ResponseCallback responseCallback, int responseTimeoutMs);

    public void unfetch(FetchId id, ResponseCallback responseCallback, int responseTimeoutMs);

    public void set(String path, JsonElement value, ResponseCallback responseCallback, int responseTimeoutMs);

    public void addState(String path, JsonElement value, StateCallback stateCallback, int stateSetTimeoutMs, ResponseCallback responseCallback, int responseTimeoutMs);

    public void addState(String path, JsonElement value, String[] setGroups, String[] fetchGroups, StateCallback stateCallback, int stateSetTimeoutMs, ResponseCallback responseCallback, int responseTimeoutMs);

    public void removeState(String path, ResponseCallback responseCallback, int responseTimeoutMs);
    
    public void change(String path, JsonElement value, ResponseCallback responseCallback, int responseTimeoutMs);

    public void addMethod(String path, MethodCallback methodCallback, int methodCallTimeoutMs, ResponseCallback responseCallback, int responseTimeoutMs);
    
    public void addMethod(String path, String[] callGroups, String[] fetchGroups, MethodCallback methodCallback, int methodCallTimeoutMs, ResponseCallback responseCallback, int responseTimeoutMs);
    
    public void removeMethod(String path, ResponseCallback responseCallback, int responseTimeoutMs);
    
    public void call(String path, JsonElement arguments, ResponseCallback responseCallback, int responseTimeoutMs);
}
