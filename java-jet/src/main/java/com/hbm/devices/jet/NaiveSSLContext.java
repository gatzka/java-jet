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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NaiveSSLContext
{
    private NaiveSSLContext()
    {
    }


    /**
     * Get an SSLContext that implements the specified secure
     * socket protocol and naively accepts all certificates
     * without verification.
     */
    public static SSLContext getInstance(String protocol) throws NoSuchAlgorithmException
    {
        return init(SSLContext.getInstance(protocol));
    }


    /**
     * Get an SSLContext that implements the specified secure
     * socket protocol and naively accepts all certificates
     * without verification.
     */
    public static SSLContext getInstance(String protocol, Provider provider) throws NoSuchAlgorithmException
    {
        return init(SSLContext.getInstance(protocol, provider));
    }


    /**
     * Get an SSLContext that implements the specified secure
     * socket protocol and naively accepts all certificates
     * without verification.
     */
    public static SSLContext getInstance(String protocol, String provider) throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return init(SSLContext.getInstance(protocol, provider));
    }


    /**
     * Set NaiveTrustManager to the given context.
     */
    private static SSLContext init(SSLContext context)
    {
        try
        {
            // Set NaiveTrustManager.
            context.init(null, new TrustManager[] { new NaiveTrustManager() }, null);
        }
        catch (KeyManagementException e)
        {
            throw new RuntimeException("Failed to initialize an SSLContext.", e);
        }

        return context;
    }


    /**
     * A {@link TrustManager} which trusts all certificates naively.
     */
    private static class NaiveTrustManager implements X509TrustManager
    {
        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }


        public void checkClientTrusted(X509Certificate[] certs, String authType)
        {
        }


        public void checkServerTrusted(X509Certificate[] certs, String authType)
        {
        }
    }
}