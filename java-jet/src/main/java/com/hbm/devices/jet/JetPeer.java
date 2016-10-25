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

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JetPeer implements Peer {
	
	private final JetConnection connection;
    private final Map<String, FetchEventCallback> openFetches;
    private final Map<Integer, JetMethod> openRequests;
    private final Gson gson;
	
	public JetPeer(JetConnection connection) {
		this.connection = connection;
        this.openFetches = new HashMap<String, FetchEventCallback>();
        this.openRequests = new HashMap<Integer, JetMethod>();
        this.gson = new GsonBuilder().create();
	}

	@Override
	public void connect(ConnectionCompleted connectionCompleted, int timeoutMs) {
		this.connection.connect(connectionCompleted, timeoutMs);
	}

	@Override
    public void disconnect() {
        this.connection.disconnect();
    }

	@Override
    public FetchId fetch(Matcher matcher, FetchEventCallback callback, ResponseCallback responseCallback, int timeoutMs) {

        FetchId fetchId = new FetchId();

        JsonObject parameters = new JsonObject();
        JsonObject path = fillPath(matcher);
        if (path != null) {
            parameters.add("path", path);
        }
        parameters.addProperty("id", fetchId.toString());
        parameters.addProperty("caseInsensitive", matcher.caseInsensitive);

        JetMethod fetch = new JetMethod(JetMethod.FETCH, parameters, responseCallback);
        this.registerFetcher(fetchId.toString(), callback);
        this.executeMethod(fetch, timeoutMs);

        return fetchId;
    }

    private void registerFetcher(String fetchId, FetchEventCallback callback) {
        synchronized(openFetches) {
            openFetches.put(fetchId, callback);
        }
    }

    private void unregisterFetcher(String fetchId) {
        synchronized(openFetches) {
            openFetches.remove(fetchId);
        }
    }

    private void executeMethod(JetMethod method, int timeoutMs) {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("timeoutMs");
        }

        if (method.hasResponseCallback()) {
            synchronized(openRequests) {
                openRequests.put(method.getRequestId(), method);
            }
        }

        JsonObject json = method.getJson();
        connection.sendMessage(gson.toJson(json));
    }

    private JsonObject fillPath(Matcher matcher) {
        JsonObject path = new JsonObject();

        if ((matcher.contains != null && !matcher.contains.isEmpty())) {
            path.addProperty("contains", matcher.contains);

        }

        if ((matcher.startsWith != null && !matcher.startsWith.isEmpty())) {
            path.addProperty("startsWith", matcher.startsWith);
        }

        if ((matcher.endsWith != null && !matcher.endsWith.isEmpty())) {
            path.addProperty("endsWith", matcher.endsWith);
        }

        if ((matcher.equals != null && !matcher.equals.isEmpty())) {
            path.addProperty("equals", matcher.equals);
        }

        if ((matcher.equalsNot != null && !matcher.equalsNot.isEmpty())) {
            path.addProperty("equalsNot", matcher.equalsNot);
        }

        if ((matcher.containsAllOf != null) && (matcher.containsAllOf.length > 0)) {
            JsonArray containsArray = new JsonArray();
            for (String element: matcher.containsAllOf) {
                containsArray.add(new JsonPrimitive(element));
            }

            path.add("containsAllOf", containsArray);
        }
        
        if ((path.entrySet() != null) && (path.entrySet().size() > 0)) {
            return path;
        } else {
            return null;
        }
    }
}
