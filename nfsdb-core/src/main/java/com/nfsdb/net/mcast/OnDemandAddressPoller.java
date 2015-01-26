/*
 * Copyright (c) 2014-2015. Vlad Ilyushchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nfsdb.net.mcast;

import com.nfsdb.logging.Logger;
import com.nfsdb.net.config.ClientConfig;
import com.nfsdb.net.config.ServerNode;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class OnDemandAddressPoller extends AbstractOnDemandPoller<ServerNode> {

    private static final Logger LOGGER = Logger.getLogger(OnDemandAddressPoller.class);

    public OnDemandAddressPoller(ClientConfig clientConfig, int inMessageCode, int outMessageCode) {
        super(clientConfig, inMessageCode, outMessageCode);
    }

    @Override
    protected ServerNode transform(ByteBuffer buf, InetSocketAddress sa) {
        char[] chars = new char[buf.getChar()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = buf.getChar();
        }
        // skip SSL byte
        buf.get();
        int port = buf.getInt();
        String addr = new String(chars);
        try {
            if (InetAddress.getByName(addr).isAnyLocalAddress() && sa != null) {
                return new ServerNode(0, sa.getAddress().getHostAddress(), port);
            }
        } catch (UnknownHostException e) {
            LOGGER.error("Got bad address [%s] from %s", addr, sa);
        }
        return new ServerNode(0, addr, port);
    }
}
