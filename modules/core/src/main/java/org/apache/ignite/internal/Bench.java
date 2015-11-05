/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.managers.communication.GridIoMessage;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.jsr166.LongAdder8;

/**
 *
 */
public class Bench {
    public static volatile boolean shit;

    public static void main(String[] args) throws InterruptedException {
        Ignition.start(config("1",
            false));
        Ignition.start(config("2",
            false));

        final boolean client = false;
        final boolean forceRnd = true;

        final Ignite ignite = Ignition.start(config("0",
            client));

        final IgniteCache<Object, Object> cache =
            ignite.getOrCreateCache(new CacheConfiguration<>()
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setBackups(1).setRebalanceMode(CacheRebalanceMode.SYNC)
                .setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC));

        Thread.sleep(2000);

        shit = true;

        final LongAdder8 cnt = new LongAdder8();

        final AtomicLong time = new AtomicLong(U.currentTimeMillis());

        for (int i = 0; i < 3; i++) {
            new Thread(
                new Runnable() {
                    @Override public void run() {
                        for (;;) {
                            int key;

                            if (client || forceRnd)
                                key = ThreadLocalRandom.current().nextInt(10000);

                            else
                                for (;;) {
                                    key = ThreadLocalRandom.current().nextInt(10000);

                                    if (ignite.affinity(null).isPrimary(ignite.cluster().localNode(), key))
                                        break;
                                }

                            cache.put(key, 0);

                            cnt.increment();

                            long l = time.get();
                            long now = U.currentTimeMillis();

                            if (now - l > 1000 && time.compareAndSet(l, now))
                                System.out.println("TPS [client=" + client + ", cnt=" + cnt.sumThenReset() + ']');
                        }
                    }
                }
            ).start();
        }
    }

    private static IgniteConfiguration config(
        String name,
        boolean client
    ) {
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();

        commSpi.setSharedMemoryPort(-1);

        return new IgniteConfiguration()
            .setGridName(name)
            .setLocalHost("127.0.0.1")
            .setClientMode(client)
            .setCommunicationSpi(new CommunicationSpi());
    }

    /**
     *
     */
    private static class CommunicationSpi extends TcpCommunicationSpi {
        /** */
        private final Map<Byte, AtomicInteger> msgMap = new ConcurrentHashMap<>();

        /** */
        @Override public void spiStart(final String gridName) throws IgniteSpiException {
            super.spiStart(gridName);

            Thread thread = new Thread(new Runnable() {
                @Override public void run() {
                    for (;;) {
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        synchronized (Bench.class) {
                            U.debug("\nGrid: " + gridName);

                            for (Map.Entry<Byte, AtomicInteger> e : msgMap.entrySet())
                                U.debug("\t" + e.getKey() + " : " + e.getValue().get());
                        }

                        msgMap.clear();
                    }
                }
            });

            thread.setDaemon(true);

            thread.start();
        }

        /** {@inheritDoc} */
        @Override public void sendMessage(
            ClusterNode node,
            Message msg
        ) throws IgniteSpiException {
            sendMessage(
                node,
                msg,
                null);
        }

        /** {@inheritDoc} */
        @Override public void sendMessage(
            ClusterNode node,
            Message msg,
            IgniteInClosure<IgniteException> ackClosure
        ) throws IgniteSpiException {
            GridIoMessage gridIoMsg = (GridIoMessage)msg;

            Message unwrappedMsg = gridIoMsg.message();

            AtomicInteger cnt = msgMap.get(unwrappedMsg.directType());

            if (cnt == null)
                msgMap.put(unwrappedMsg.directType(), cnt = new AtomicInteger());

            if (shit && unwrappedMsg.directType() == 56) {
                cnt.decrementAndGet();

                return;
            }

            cnt.incrementAndGet();

            super.sendMessage(
                node,
                msg,
                ackClosure);
        }
    }
}