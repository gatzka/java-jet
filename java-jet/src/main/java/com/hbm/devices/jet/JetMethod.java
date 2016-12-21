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

import com.google.gson.JsonObject;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

class JetMethod {

    static final String AUTHENTICATE = "authenticate";
    static final String INFO = "info";
    static final String SET = "set";
    static final String FETCH = "fetch";
    static final String GET = "get";
    static final String UNFETCH = "unfetch";
    static final String CALL = "call";
    static final String ADD = "add";
    static final String REMOVE = "remove";
    static final String CHANGE = "change";
    static final String CONFIG = "config";

    private static final AtomicInteger REQUEST_ID_ROUNTER = new AtomicInteger();

    private final ResponseCallback responseCallback;
    private int requestId;
    private final JsonObject json;
    private ScheduledFuture<Void> future;

    JetMethod(final String method, JsonObject parameters, ResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
        this.json = new JsonObject();
        this.json.addProperty("jsonrpc", "2.0");
        this.json.addProperty("method", method);
        if (responseCallback != null) {
            this.requestId = REQUEST_ID_ROUNTER.incrementAndGet();
            this.json.addProperty("id", this.requestId);
        }

        if (parameters != null) {
            this.json.add("params", parameters);
        }
    }

    boolean hasResponseCallback() {
        return responseCallback != null;
    }

    int getRequestId() {
        return requestId;
    }

    JsonObject getJson() {
        return json;
    }

    void addFuture(ScheduledFuture<Void> future) {
        this.future = future;
    }
    
    ScheduledFuture<Void> getFuture() {
        return this.future;
    }
    
    void callResponseCallback(boolean completed, JsonObject response) {
        if (hasResponseCallback()) {
            responseCallback.onResponse(completed, response);
        }
    }
}
