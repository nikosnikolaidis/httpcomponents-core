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
package org.apache.hc.core5.http.impl.bootstrap;

import org.apache.hc.core5.annotation.Internal;
import org.apache.hc.core5.function.Decorator;
import org.apache.hc.core5.http.nio.command.ShutdownCommand;
import org.apache.hc.core5.reactor.IOEventHandlerFactory;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.IOSessionListener;
import org.apache.hc.core5.reactor.ProtocolIOSession;

/**
 * HTTP/1.1 server side message exchange handler.
 *
 * @since 5.0
 */
public class HttpAsyncServer extends AsyncServer {

    /**
     * Use {@link AsyncServerBootstrap} to create instances of this class.
     */
    @Internal
    public HttpAsyncServer(
            final IOEventHandlerFactory eventHandlerFactory,
            final IOReactorConfig ioReactorConfig,
            final Decorator<ProtocolIOSession> ioSessionDecorator,
            final IOSessionListener sessionListener) {
        super(eventHandlerFactory, ioReactorConfig, ioSessionDecorator, sessionListener,
                        ShutdownCommand.GRACEFUL_NORMAL_CALLBACK);
    }

}
