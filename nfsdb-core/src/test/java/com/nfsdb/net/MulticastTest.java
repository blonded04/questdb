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

package com.nfsdb.net;

import com.nfsdb.Journal;
import com.nfsdb.JournalWriter;
import com.nfsdb.exceptions.JournalNetworkException;
import com.nfsdb.model.Quote;
import com.nfsdb.net.config.ClientConfig;
import com.nfsdb.net.config.ServerConfig;
import com.nfsdb.net.config.ServerNode;
import com.nfsdb.net.mcast.AbstractOnDemandSender;
import com.nfsdb.net.mcast.OnDemandAddressPoller;
import com.nfsdb.net.mcast.OnDemandAddressSender;
import com.nfsdb.test.tools.AbstractTest;
import com.nfsdb.test.tools.TestUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.net.SocketException;
import java.util.concurrent.TimeUnit;

public class MulticastTest extends AbstractTest {

    @Test
    public void testAllNics() throws Exception {
        if (isMulticastDisabled()) {
            return;
        }
        assertMulticast();
    }

    @Test
    public void testDefaultNICBehaviour() throws Exception {
        if (isMulticastDisabled()) {
            return;
        }
        assertMulticast();
    }

    @Test
    public void testIPV4Forced() throws Exception {
        if (isMulticastDisabled()) {
            return;
        }
        System.setProperty("java.net.preferIPv4Stack", "true");
        assertMulticast();
    }

    @Test
    @Ignore
    public void testIPv6() throws Exception {
        if (isMulticastDisabled()) {
            return;
        }

        JournalServer server = new JournalServer(new ServerConfig() {{
            addNode(new ServerNode(0, "[0:0:0:0:0:0:0:0]"));
            setHeartbeatFrequency(100);
        }}, factory, null, 0);
        JournalClient client = new JournalClient(new ClientConfig() {{
        }}, factory);


        JournalWriter<Quote> remote = factory.writer(Quote.class, "remote");
        server.start();
        client.start();

        client.halt();
        server.halt();
        Journal<Quote> local = factory.reader(Quote.class, "local");
        TestUtils.assertDataEquals(remote, local);
    }

    @Test
    public void testLocalhostBehaviour() throws Exception {

        if (isMulticastDisabled()) {
            return;
        }

        assertMulticast();
    }

    private void assertMulticast() throws JournalNetworkException {
        AbstractOnDemandSender sender = new OnDemandAddressSender(new ServerConfig(), 120, 150, 0);
        sender.start();

        OnDemandAddressPoller poller = new OnDemandAddressPoller(new ClientConfig(), 150, 120);
        ServerNode address = poller.poll(2, 500, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(address);
        sender.halt();
    }

    private boolean isMulticastDisabled() throws JournalNetworkException, SocketException {
        return !new ServerConfig().getMultiCastInterface(0).supportsMulticast();
    }
}
