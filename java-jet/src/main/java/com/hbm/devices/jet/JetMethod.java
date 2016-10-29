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

import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonObject;

class JetMethod {
    static final String INFO = "info";
    static final String SET = "set";
    static final String FETCH = "fetch";
    static final String UNFETCH = "unfetch";
    static final String CALL = "call";
    static final String ADD = "add";
    static final String REMOVE = "remove";
    static final String CHANGE = "change";

    private static AtomicInteger requestIdCounter = new AtomicInteger();;

    private final ResponseCallback responseCallback;
    private int requestId;
    private final JsonObject json;

    JetMethod(final String method, JsonObject parameters, ResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
        JsonObject json = new JsonObject();
        json.addProperty("jsonrpc", "2.0");
        json.addProperty("method", method);
        if (responseCallback != null) {
            this.requestId = requestIdCounter.incrementAndGet();;
            json.addProperty("id", this.requestId);
        }

        if (parameters != null) {
            json.add("params", parameters);
        }

        this.json = json;
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

    void callResponseCallback(boolean completed, JsonObject response) {
        if (hasResponseCallback()) {
            responseCallback.onResponse(completed, response);
        }
    }
}
