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

import com.nfsdb.collections.Lists;
import com.nfsdb.exceptions.JournalNetworkException;

import java.io.IOException;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

public class NetworkConfig {
    public static final int DEFAULT_DATA_PORT = 7075;
    protected static final String DEFAULT_MULTICAST_ADDRESS_IPV4 = "230.100.12.4";
    protected static final String DEFAULT_MULTICAST_ADDRESS_IPV6_1 = "FF02:231::4500";

    private static final int DEFAULT_MULTICAST_PORT = 4446;
    private static final int DEFAULT_SO_RCVBUF = 1024 * 1024;
    protected final List<ServerNode> nodes = new ArrayList<>();
    private final SslConfig sslConfig = new SslConfig();
    protected InetAddress multiCastAddress;
    private int multiCastPort = DEFAULT_MULTICAST_PORT;
    private int soRcvBuf = DEFAULT_SO_RCVBUF;
    private boolean enableMultiCast = true;
    private String ifName = null;

    public static boolean isInet6(InetAddress address) {
        return address instanceof Inet6Address;
    }

    public void addNode(ServerNode node) {
        nodes.add(node);
    }

    public String getIfName() {
        return ifName;
    }

    public void setIfName(String ifName) {
        this.ifName = ifName;
    }

    public InetAddress getMultiCastAddress() {
        return multiCastAddress;
    }

    public void setMultiCastAddress(InetAddress multiCastAddress) {
        this.multiCastAddress = multiCastAddress;
    }

    public int getMultiCastPort() {
        return multiCastPort;
    }

    public void setMultiCastPort(int multiCastPort) {
        this.multiCastPort = multiCastPort;
    }

    // todo: this is naive impl, optimise (hash map?)
    public ServerNode getNode(int instance) {
        for (int i = 0; i < nodes.size(); i++) {
            ServerNode node = nodes.get(i);
            if (node.getId() == instance) {
                return node;
            }
        }
        return null;
    }

    public int getSoRcvBuf() {
        return soRcvBuf;
    }

    public void setSoRcvBuf(int soRcvBuf) {
        this.soRcvBuf = soRcvBuf;
    }

    public SslConfig getSslConfig() {
        return sslConfig;
    }

    public boolean isMultiCastEnabled() {
        return enableMultiCast;
    }

    public Lists.ImmutableIteratorWrapper<ServerNode> nodes() {
        return Lists.immutableIterator(nodes).reset();
    }

    public void setEnableMultiCast(boolean enableMultiCast) {
        this.enableMultiCast = enableMultiCast;
    }

    protected InetAddress getDefaultMultiCastAddress(NetworkInterface ifn) throws JournalNetworkException {
        try {
            if (!ifn.supportsMulticast()) {
                throw new JournalNetworkException("Multicast is not supported on " + ifn.getName());
            }

            if (ifn.getInterfaceAddresses().size() == 0) {
                throw new JournalNetworkException("No IP addresses assigned to " + ifn.getName());
            }

            if (isInet6(ifn.getInterfaceAddresses().get(0).getAddress())) {
                return InetAddress.getByName(DEFAULT_MULTICAST_ADDRESS_IPV6_1);
            } else {
                return InetAddress.getByName(DEFAULT_MULTICAST_ADDRESS_IPV4);
            }
        } catch (Exception e) {
            throw new JournalNetworkException(e);
        }
    }

    protected DatagramChannelWrapper openDatagramChannel(NetworkInterface ifn) throws JournalNetworkException {
        InetAddress address = getMultiCastAddress();
        if (address == null) {
            address = getDefaultMultiCastAddress(ifn);
        }
        ProtocolFamily family = NetworkConfig.isInet6(address) ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;

        try {
            DatagramChannel dc = DatagramChannel.open(family)
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                    .setOption(StandardSocketOptions.IP_MULTICAST_IF, ifn)
                    .bind(new InetSocketAddress(getMultiCastPort()));

            dc.join(address, ifn);
            return new DatagramChannelWrapper(dc, new InetSocketAddress(address, getMultiCastPort()));
        } catch (IOException e) {
            throw new JournalNetworkException(e);
        }
    }
}
