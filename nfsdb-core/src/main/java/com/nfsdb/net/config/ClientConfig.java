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

package com.nfsdb.net.config;

import com.nfsdb.exceptions.JournalNetworkException;
import com.nfsdb.logging.Logger;
import com.nfsdb.net.mcast.OnDemandAddressPoller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeUnit;

public class ClientConfig extends NetworkConfig {

    public static final Logger LOGGER = Logger.getLogger(ClientConfig.class);
    private final ClientReconnectPolicy reconnectPolicy = new ClientReconnectPolicy();
    private int soSndBuf = 8192;
    private boolean keepAlive = true;
    private boolean tcpNoDelay = true;
    private int linger = 0;

    public ClientConfig() {
        this(null);
    }

    public ClientConfig(String hosts) {
        super();
        getSslConfig().setClient(true);
        if (hosts != null && hosts.length() > 0) {
            ServerNode.parse(hosts, nodes);
        }
    }

    public boolean getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getLinger() {
        return linger;
    }

    public void setLinger(int linger) {
        this.linger = linger;
    }

    public ClientReconnectPolicy getReconnectPolicy() {
        return reconnectPolicy;
    }

    public int getSoSndBuf() {
        return soSndBuf;
    }

    public void setSoSndBuf(int soSndBuf) {
        this.soSndBuf = soSndBuf;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public DatagramChannelWrapper openDatagramChannel() throws JournalNetworkException {
        return openDatagramChannel(getMultiCastInterface());
    }

    public SocketChannel openSocketChannel() throws JournalNetworkException {
        if (nodes.size() == 0) {
            if (isMultiCastEnabled()) {
                nodes.add(pollServerAddress());
            } else {
                throw new JournalNetworkException("No server nodes");
            }
        }

        for (int i = 0; i < nodes.size(); i++) {
            ServerNode node = nodes.get(i);
            try {
                return openSocketChannel0(node);
            } catch (UnresolvedAddressException | IOException e) {
                LOGGER.info("Node %s is unavailable [%s]", node, e.getMessage());
            }
        }

        throw new JournalNetworkException("Could not connect to any node");
    }

    public SocketChannel openSocketChannel(ServerNode node) throws JournalNetworkException {
        try {
            return openSocketChannel0(node);
        } catch (UnresolvedAddressException | IOException e) {
            throw new JournalNetworkException(e);
        }
    }

    public SocketChannel openSocketChannel0(ServerNode node) throws IOException {
        InetSocketAddress address = new InetSocketAddress(node.getHostname(), node.getPort());
        SocketChannel channel = SocketChannel.open(address)
                .setOption(StandardSocketOptions.TCP_NODELAY, isTcpNoDelay())
                .setOption(StandardSocketOptions.SO_KEEPALIVE, getKeepAlive())
                .setOption(StandardSocketOptions.SO_SNDBUF, getSoSndBuf())
                .setOption(StandardSocketOptions.SO_RCVBUF, getSoRcvBuf())
                .setOption(StandardSocketOptions.SO_LINGER, getLinger());

        LOGGER.info("Connected to %s", node);
        return channel;
    }

    private NetworkInterface getMultiCastInterface() throws JournalNetworkException {
        try {
            if (getIfName() == null) {
                return NetworkInterface.getByInetAddress(
                        InetAddress.getByName(InetAddress.getLocalHost().getHostName()
                        )
                );
            }

            return NetworkInterface.getByName(getIfName());
        } catch (IOException e) {
            throw new JournalNetworkException(e);
        }
    }

    private ServerNode pollServerAddress() throws JournalNetworkException {
        return new OnDemandAddressPoller(this, 235, 230).poll(3, 500, TimeUnit.MILLISECONDS);
    }
}
