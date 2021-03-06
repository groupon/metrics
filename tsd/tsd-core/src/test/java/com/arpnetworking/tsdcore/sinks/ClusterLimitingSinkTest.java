/**
 * Copyright 2015 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arpnetworking.tsdcore.sinks;

import com.arpnetworking.metrics.MetricsFactory;
import com.arpnetworking.test.TestBeanFactory;
import com.arpnetworking.tsdcore.model.AggregatedData;
import com.arpnetworking.tsdcore.model.Condition;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Tests for the <code>ClusterLimitingSink</code> class.
 *
 * @author Ville Koskela (vkoskela at groupon dot com)
 */
public class ClusterLimitingSinkTest {

    @Test
    public void testClusterLimitingSink() {
        final Sink targetSink = Mockito.mock(Sink.class, "TargetSink");
        final MockingSinkFactory limitingSinkFactory = new MockingSinkFactory(targetSink);

        final Sink sink = new ClusterLimitingSink.Builder()
                    .setSinkFactory(limitingSinkFactory)
                    .setName("testClusterLimitingSink")
                    .setMaxAggregations(1L)
                    .setMetricsFactory(Mockito.mock(MetricsFactory.class))
                    .setSink(targetSink)
                    .setStateFileDirectory(new File("./"))
                    .build();

        final AggregatedData clusterAData1 = TestBeanFactory.createAggregatedDataBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setCluster("ClusterA")
                        .build())
                .build();
        final AggregatedData clusterBData1 = TestBeanFactory.createAggregatedDataBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setCluster("ClusterB")
                        .build())
                .build();
        final AggregatedData clusterBData2 = TestBeanFactory.createAggregatedDataBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setCluster("ClusterB")
                        .build())
                .build();

        final Condition clusterACondition1 = TestBeanFactory.createConditionBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setCluster("ClusterA")
                        .build())
                .build();
        final Condition clusterBCondition1 = TestBeanFactory.createConditionBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setCluster("ClusterB")
                        .build())
                .build();
        final Condition clusterBCondition2 = TestBeanFactory.createConditionBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setCluster("ClusterB")
                        .build())
                .build();

        sink.recordAggregateData(
                Lists.newArrayList(clusterAData1, clusterBData1, clusterBData2),
                Lists.newArrayList(clusterACondition1, clusterBCondition1, clusterBCondition2));

        final Map<String, Sink> mockLimitingSinks = limitingSinkFactory.getSinks();

        Mockito.verify(mockLimitingSinks.get("./testClusterLimitingSink_ClusterA.state")).recordAggregateData(
                Lists.newArrayList(clusterAData1),
                Lists.newArrayList(clusterACondition1));

        Mockito.verify(mockLimitingSinks.get("./testClusterLimitingSink_ClusterB.state")).recordAggregateData(
                Lists.newArrayList(clusterBData1, clusterBData2),
                Lists.newArrayList(clusterBCondition1, clusterBCondition2));

        final AggregatedData clusterAData2 = TestBeanFactory.createAggregatedDataBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setCluster("ClusterA")
                        .build())
                .build();

        final Condition clusterACondition2 = TestBeanFactory.createConditionBuilder()
                .setFQDSN(TestBeanFactory.createFQDSNBuilder()
                        .setCluster("ClusterA")
                        .build())
                .build();

        sink.recordAggregateData(
                Lists.newArrayList(clusterAData2),
                Lists.newArrayList(clusterACondition2));

        Mockito.verify(mockLimitingSinks.get("./testClusterLimitingSink_ClusterA.state")).recordAggregateData(
                Lists.newArrayList(clusterAData2),
                Lists.newArrayList(clusterACondition2));
    }

    private static class MockingSinkFactory implements ClusterLimitingSink.LimitingSinkFactory {

        public MockingSinkFactory(final Sink targetSink) {
            _targetSink = targetSink;
            _sinks = Maps.newHashMap();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Sink create(final String cluster, final File stateFile) {
            final Sink mockSink = Mockito.mock(Sink.class, stateFile.toString());
            Mockito.doAnswer(new Answer<Void>() {
                @Override
                // CHECKSTYLE.OFF: IllegalThrows - Defined by interface
                public Void answer(final InvocationOnMock invocation) throws Throwable {
                    // CHECKSTYLE.ON: IllegalThrows
                    _targetSink.recordAggregateData(
                            (Collection<AggregatedData>) invocation.getArguments()[0],
                            (Collection<Condition>) invocation.getArguments()[1]);
                    return null;
                }
            }).when(mockSink).recordAggregateData(Mockito.<AggregatedData>anyCollection(), Mockito.<Condition>anyCollection());
            _sinks.put(stateFile.toString(), mockSink);
            return mockSink;
        }

        public Map<String, Sink> getSinks() {
            return Collections.unmodifiableMap(_sinks);
        }

        private final Sink _targetSink;
        private final Map<String, Sink> _sinks;
    }
}
