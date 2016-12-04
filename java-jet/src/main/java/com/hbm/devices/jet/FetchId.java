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

public class FetchId {

    private static final AtomicInteger fetchIdCounter = new AtomicInteger();

    private final int fetchId;

    public FetchId() {
        this.fetchId = fetchIdCounter.incrementAndGet();
    }

    @Override
    public int hashCode() {
        return Integer.toString(fetchId).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FetchId)) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        FetchId rhs = (FetchId) obj;

        return rhs.fetchId == this.fetchId;
    }

    @Override
    public String toString() {
        return Integer.toString(fetchId);
    }
    
    int getId() {
        return fetchId;
    }
}
