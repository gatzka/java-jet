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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JetPeer implements Peer, Observer, Closeable {

    private final JetConnection connection;
    private final Map<Integer, FetchEventCallback> openFetches;
    private final Map<Integer, JetMethod> openRequests;
    private final Map<String, StateCallback> stateCallbacks;
    private final Map<String, MethodCallback> methodCallbacks;
    private final Gson gson;
    private final JsonParser parser;
    private final ScheduledThreadPoolExecutor executor;

    private static final Logger LOGGER = Logger.getLogger(JetConstants.LOGGER_NAME);

    public JetPeer(JetConnection connection) {
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.connection = connection;
        this.openFetches = new HashMap<>();
        this.openRequests = new HashMap<>();
        this.stateCallbacks = new HashMap<>();
        this.methodCallbacks = new HashMap<>();
        this.gson = new GsonBuilder().create();
        this.parser = new JsonParser();
    }

    @Override
    public void close() throws IOException {
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    LOGGER.log(Level.SEVERE, "Interrupted while waiting for termination of timer tasks!\n");
                }
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void connect(ConnectionCompleted connectionCompleted, int timeoutMs) {
        this.connection.addObserver(this);
        this.connection.connect(connectionCompleted, timeoutMs);
    }

    @Override
    public void disconnect() {
        this.connection.deleteObserver(this);
        this.connection.disconnect();
    }

    @Override
    public void set(String path, JsonElement value, ResponseCallback responseCallback, int timeoutMs) {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }

        synchronized (stateCallbacks) {
            if (stateCallbacks.containsKey(path)) {
                throw new IllegalArgumentException("Don't call set() on a state you own, use change() instead!");
            }
        }

        JsonObject parameters = new JsonObject();
        parameters.addProperty("path", path);
        parameters.add("value", value);
        parameters.addProperty("timeout", timeoutMs / 1000.0);
        JetMethod set = new JetMethod(JetMethod.SET, parameters, responseCallback);
        this.executeMethod(set, timeoutMs);
    }

    @Override
    public void addState(String path, JsonElement value, StateCallback stateCallback, int stateSetTimeoutMs, ResponseCallback responseCallback, int responseTimeoutMs) {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }

        JsonObject parameters = new JsonObject();
        parameters.addProperty("path", path);
        parameters.add("value", value);
        parameters.addProperty("timeout", stateSetTimeoutMs / 1000.0);
        if (stateCallback == null) {
            parameters.addProperty("fetchOnly", true);
        } else {
            registerStateCallback(path, stateCallback);
        }

        JetMethod add = new JetMethod(JetMethod.ADD, parameters, responseCallback);
        this.executeMethod(add, responseTimeoutMs);
    }

    @Override
    public void removeState(String path, ResponseCallback responseCallback, int responseTimeoutMs) {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }

        unregisterStateCallback(path);

        JsonObject parameters = new JsonObject();
        parameters.addProperty("path", path);
        JetMethod remove = new JetMethod(JetMethod.REMOVE, parameters, responseCallback);
        this.executeMethod(remove, responseTimeoutMs);
    }

    @Override
    public void change(String path, JsonElement value, ResponseCallback responseCallback, int responseTimeoutMs) {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }

        synchronized (stateCallbacks) {
            if (!stateCallbacks.containsKey(path)) {
                throw new IllegalArgumentException("don't call change() on a state you do not own");
            }
        }

        JsonObject parameters = new JsonObject();
        parameters.addProperty("path", path);
        parameters.add("value", value);
        JetMethod change = new JetMethod(JetMethod.CHANGE, parameters, responseCallback);
        this.executeMethod(change, responseTimeoutMs);
    }

    @Override
    public FetchId fetch(Matcher matcher, FetchEventCallback callback, ResponseCallback responseCallback, int timeoutMs) {
        FetchId fetchId = new FetchId();

        JsonObject parameters = new JsonObject();
        JsonObject path = fillPath(matcher);
        if (path != null) {
            parameters.add("path", path);
        }
        parameters.addProperty("id", fetchId.getId());
        parameters.addProperty("caseInsensitive", matcher.caseInsensitive);

        JetMethod fetch = new JetMethod(JetMethod.FETCH, parameters, responseCallback);
        this.registerFetcher(fetchId.getId(), callback);
        this.executeMethod(fetch, timeoutMs);

        return fetchId;
    }

    @Override
    public void unfetch(FetchId id, ResponseCallback responseCallback, int responseTimeoutMs) {
        this.unregisterFetcher(id.getId());

        JsonObject parameters = new JsonObject();
        parameters.addProperty("id", id.getId());
        JetMethod unfetch = new JetMethod(JetMethod.UNFETCH, parameters, responseCallback);
        this.executeMethod(unfetch, responseTimeoutMs);
    }

    @Override
    public void call(String path, JsonElement arguments, ResponseCallback responseCallback, int responseTimeoutMs) {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }

        synchronized (methodCallbacks) {
            if (methodCallbacks.containsKey(path)) {
                throw new IllegalArgumentException("Don't call call() on a method you own!");
            }
        }

        JsonObject parameters = new JsonObject();
        parameters.addProperty("path", path);
        if (!arguments.isJsonNull()) {
            parameters.add("args", arguments);
        }
        parameters.addProperty("timeout", responseTimeoutMs / 1000.0);
        JetMethod call = new JetMethod(JetMethod.CALL, parameters, responseCallback);
        this.executeMethod(call, responseTimeoutMs);
    }

    @Override
    public void addMethod(String path, MethodCallback methodCallback, int methodCallTimeoutMs, ResponseCallback responseCallback, int responseTimeoutMs) {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }

        if (methodCallback == null) {
            throw new NullPointerException("methodCallback");
        }
        JsonObject parameters = new JsonObject();
        parameters.addProperty("path", path);
        parameters.addProperty("timeout", methodCallTimeoutMs / 1000.0);
 
        registerMethodCallback(path, methodCallback);

        JetMethod add = new JetMethod(JetMethod.ADD, parameters, responseCallback);
        this.executeMethod(add, responseTimeoutMs);
    }
    
    @Override
    public void removeMethod(String path, ResponseCallback responseCallback, int responseTimeoutMs) {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }

        unregisterMethodCallback(path);

        JsonObject parameters = new JsonObject();
        parameters.addProperty("path", path);
        JetMethod remove = new JetMethod(JetMethod.REMOVE, parameters, responseCallback);
        this.executeMethod(remove, responseTimeoutMs);
    }

    private void registerFetcher(int fetchId, FetchEventCallback callback) {
        synchronized (openFetches) {
            openFetches.put(fetchId, callback);
        }
    }

    private void unregisterFetcher(int fetchId) {
        synchronized (openFetches) {
            openFetches.remove(fetchId);
        }
    }

    private void registerStateCallback(String path, StateCallback callback) {
        synchronized (stateCallbacks) {
            stateCallbacks.put(path, callback);
        }
    }

    private void unregisterStateCallback(String path) {
        synchronized (stateCallbacks) {
            stateCallbacks.remove(path);
        }
    }
    
    private void registerMethodCallback(String path, MethodCallback callback) {
        synchronized (methodCallbacks) {
            methodCallbacks.put(path, callback);
        }
    }

    private void unregisterMethodCallback(String path) {
        synchronized (methodCallbacks) {
            methodCallbacks.remove(path);
        }
    }

    private void executeMethod(JetMethod method, int timeoutMs) {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("timeoutMs");
        }

        if (method.hasResponseCallback()) {
            ResponseTimeoutTask task;
            task = new ResponseTimeoutTask(method);
            synchronized (openRequests) {
                openRequests.put(method.getRequestId(), method);
                ScheduledFuture<Void> future;
                future = executor.schedule(task, timeoutMs, TimeUnit.MILLISECONDS);
                method.addFuture(future);
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
            for (String element : matcher.containsAllOf) {
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

    @Override
    public void update(Observable observable, Object obj) {
        try {
            JsonElement element = parser.parse((String) obj);
            if (element == null) {
                return;
            }

            if (element.isJsonObject()) {
                handleSingleJsonMessage((JsonObject) element);
            } else if (element.isJsonArray()) {
                JsonArray array = (JsonArray) element;
                for (int i = 0; i < array.size(); i++) {
                    JsonElement e = array.get(i);
                    if (e.isJsonObject()) {
                        handleSingleJsonMessage((JsonObject) e);
                    }
                }
            }
        } catch (JsonSyntaxException e) {
            /*
             * There is no error handling necessary in this case. If somebody sends us invalid JSON,
             * we just ignore the packet and go ahead.
             */
            LOGGER.log(Level.SEVERE, "Can't parse JSON!", e);
        }
    }

    private void handleSingleJsonMessage(JsonObject object) {
        JsonPrimitive fetchId = getFetchId(object);
        if (fetchId != null) {
            handleFetch(fetchId.getAsInt(), object);
            return;
        }

        if (isResponse(object)) {
            handleResponse(object);
            return;
        }

        handleStateOrMethodCallbacks(object);
    }

    private void handleFetch(int fetchId, JsonObject object) {
        synchronized (openFetches) {
            FetchEventCallback callback = openFetches.get(fetchId);
            if (callback != null) {
                JsonObject params = object.getAsJsonObject("params");
                if (params != null) {
                    callback.onFetchEvent(params);
                }
            }
        }
    }

    private void handleResponse(JsonObject object) {
        JsonPrimitive token = object.getAsJsonPrimitive("id");
        int id = token.getAsInt();
        synchronized (openRequests) {
            JetMethod method = openRequests.get(id);
            if (method == null) {
                return;
            }
            openRequests.remove(id);
            method.getFuture().cancel(true);
            method.callResponseCallback(true, object);
        }
    }

    private JsonPrimitive getFetchId(JsonObject object) {
        JsonPrimitive method = object.getAsJsonPrimitive("method");
        if ((method != null) && (method.isNumber())) {
            return method;
        }

        return null;
    }

    private boolean isResponse(JsonObject object) {
        JsonPrimitive id = object.getAsJsonPrimitive("id");
        return (id != null) && (id.isNumber());
    }

    private void handleStateOrMethodCallbacks(JsonObject object) {
        try {
            JsonPrimitive method = object.getAsJsonPrimitive("method");
            if (method == null) {
                throw new JsonRpcException(JsonRpcException.METHOD_NOT_FOUND, "no method given");
            }

            String path = method.getAsString();
            if ((path == null) || (path.length() == 0)) {
                throw new JsonRpcException(JsonRpcException.METHOD_NOT_FOUND, "method is not a string or integer");
            }

            boolean stateHandled = handleStateCallback(object, path);
            if (!stateHandled) {
                handleMethod(object, path);
            }
        } catch (JsonRpcException e) {
            sendResponse(object, e.getJson());
        }
    }

    private void sendResponse(JsonObject request, JsonObject responseObject) {
        JsonPrimitive id = request.getAsJsonPrimitive("id");
        if ((id != null) && ((id.isString()) || (id.isNumber()))) {
            responseObject.add("id", id);
            this.connection.sendMessage(gson.toJson(responseObject));
        }
    }

    private boolean handleStateCallback(JsonObject object, String path) throws JsonRpcException {
        boolean stateFound;

        synchronized (stateCallbacks) {
            stateFound = stateCallbacks.containsKey(path);
        }

        if (stateFound) {
            StateCallback callback;
            synchronized (stateCallbacks) {
                callback = stateCallbacks.get(path);
            }
            if (callback == null) {
                throw new JsonRpcException(JsonRpcException.INVALID_REQUEST, "state is readonly");
            }

            JsonObject parameters = object.getAsJsonObject("params");
            if (parameters == null) {
                throw new JsonRpcException(JsonRpcException.INVALID_PARAMS, "no parameters in json");
            }

            JsonElement value = parameters.get("value");
            if (value == null) {
                throw new JsonRpcException(JsonRpcException.INVALID_PARAMS, "no value in parameter");
            }

            JsonElement newValue = callback.onStateSet(path, value);
            if (newValue != null) {
                value = newValue;
            }

            this.change(path, value, null, 0);
            JsonObject result = new JsonObject();
            result.addProperty("result", true);
            sendResponse(object, result);
            return true;

        } else {
            return false;
        }
    }

    private void handleMethod(JsonObject object, String path) {
    }

    private class ResponseTimeoutTask implements Callable<Void> {

        private final JetMethod method;

        private ResponseTimeoutTask(JetMethod method) {
            this.method = method;
        }

        @Override
        public Void call() throws Exception {
            synchronized (openRequests) {
                openRequests.remove(method.getRequestId());
            }

            JsonObject response = new JsonObject();
            response.addProperty("id", method.getRequestId());
            response.addProperty("jsonrpc", "2.0");

            JsonObject error = new JsonObject();
            error.addProperty("code", -32100);
            error.addProperty("message", "timeout while waiting for response");
            response.add("error", error);
            method.callResponseCallback(false, response);

            return null;
        }
    }
}
