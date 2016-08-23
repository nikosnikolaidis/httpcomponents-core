/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.hc.core5.http.examples;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.config.ConnectionConfig;
import org.apache.hc.core5.http.impl.nio.BasicAsyncRequestProducer;
import org.apache.hc.core5.http.impl.nio.BasicAsyncResponseConsumer;
import org.apache.hc.core5.http.impl.nio.DefaultHttpClientIOEventHandlerFactory;
import org.apache.hc.core5.http.impl.nio.HttpAsyncRequestExecutor;
import org.apache.hc.core5.http.impl.nio.HttpAsyncRequester;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.pool.nio.BasicNIOConnPool;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.apache.hc.core5.http.protocol.HttpProcessor;
import org.apache.hc.core5.http.protocol.HttpProcessorBuilder;
import org.apache.hc.core5.http.protocol.RequestConnControl;
import org.apache.hc.core5.http.protocol.RequestContent;
import org.apache.hc.core5.http.protocol.RequestExpectContinue;
import org.apache.hc.core5.http.protocol.RequestTargetHost;
import org.apache.hc.core5.http.protocol.RequestUserAgent;
import org.apache.hc.core5.reactor.ConnectingIOReactor;
import org.apache.hc.core5.reactor.DefaultConnectingIOReactor;
import org.apache.hc.core5.reactor.IOEventHandlerFactory;
import org.apache.hc.core5.reactor.IOReactorConfig;

/**
 * Minimal pipelining HTTP/1.1 client.
 * <p>
 * Please note that this example represents a minimal HTTP client implementation.
 * It does not support HTTPS as is.
 * You either need to provide BasicNIOConnPool with a connection factory
 * that supports SSL or use a more complex HttpAsyncClient.
 *
 */
public class PipeliningHttpClient {

    public static void main(String[] args) throws Exception {
        // Create HTTP protocol processing chain
        HttpProcessor httpproc = HttpProcessorBuilder.create()
                // Use standard client-side protocol interceptors
                .add(new RequestContent())
                .add(new RequestTargetHost())
                .add(new RequestConnControl())
                .add(new RequestUserAgent("Test/1.1"))
                .add(new RequestExpectContinue()).build();
        // Create client-side HTTP protocol handler
        HttpAsyncRequestExecutor protocolHandler = new HttpAsyncRequestExecutor();
        // Create client-side I/O event handler factory
        IOEventHandlerFactory eventHandlerFactory = new DefaultHttpClientIOEventHandlerFactory(
                new HttpAsyncRequestExecutor(),
                ConnectionConfig.DEFAULT);
        // Create client-side I/O reactor
        final ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(
                eventHandlerFactory,
                IOReactorConfig.DEFAULT);
        // Create HTTP connection pool
        BasicNIOConnPool pool = new BasicNIOConnPool(ioReactor, 0);
        // Limit total number of connections to just two
        pool.setDefaultMaxPerRoute(2);
        pool.setMaxTotal(2);
        // Run the I/O reactor in a separate thread
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    // Ready to go!
                    ioReactor.execute();
                } catch (InterruptedIOException ex) {
                    System.err.println("Interrupted");
                } catch (IOException e) {
                    System.err.println("I/O error: " + e.getMessage());
                }
                System.out.println("Shutdown");
            }

        });
        // Start the client thread
        t.start();
        // Create HTTP requester
        HttpAsyncRequester requester = new HttpAsyncRequester(httpproc);

        final HttpHost target = new HttpHost("www.apache.org");
        List<BasicAsyncRequestProducer> requestProducers = Arrays.asList(
                new BasicAsyncRequestProducer(target, new BasicHttpRequest("GET", "/index.html")),
                new BasicAsyncRequestProducer(target, new BasicHttpRequest("GET", "/foundation/index.html")),
                new BasicAsyncRequestProducer(target, new BasicHttpRequest("GET", "/foundation/how-it-works.html"))
        );
        List<BasicAsyncResponseConsumer> responseConsumers = Arrays.asList(
                new BasicAsyncResponseConsumer(),
                new BasicAsyncResponseConsumer(),
                new BasicAsyncResponseConsumer()
        );

        final CountDownLatch latch = new CountDownLatch(1);

        HttpCoreContext context = HttpCoreContext.create();
        requester.executePipelined(
                target, requestProducers, responseConsumers, pool, context,
                new FutureCallback<List<HttpResponse>>() {

                    @Override
                    public void completed(final List<HttpResponse> result) {
                        latch.countDown();
                        for (HttpResponse response: result) {
                            System.out.println(target + "->" + response.getCode());
                        }
                    }

                    @Override
                    public void failed(final Exception ex) {
                        latch.countDown();
                        System.out.println(target + "->" + ex);
                    }

                    @Override
                    public void cancelled() {
                        latch.countDown();
                        System.out.println(target + " cancelled");
                    }

                });

        latch.await();
        System.out.println("Shutting down I/O reactor");
        ioReactor.shutdown();
        System.out.println("Done");
    }

}