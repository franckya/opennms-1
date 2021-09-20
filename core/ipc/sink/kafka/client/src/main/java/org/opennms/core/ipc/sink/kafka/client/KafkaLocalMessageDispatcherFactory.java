/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.core.ipc.sink.kafka.client;

import static org.opennms.core.ipc.sink.api.Message.SINK_METRIC_PRODUCER_DOMAIN;

import org.opennms.core.ipc.sink.api.Message;
import org.opennms.core.ipc.sink.api.SinkModule;
import org.opennms.core.ipc.sink.common.AbstractMessageDispatcherFactory;
import org.opennms.core.ipc.sink.kafka.server.KafkaMessageConsumerManager;
import org.opennms.core.tracing.api.TracerRegistry;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.codahale.metrics.MetricRegistry;

import io.opentracing.Tracer;

/**
 * Dispatches the messages directly the consumers.
 *
 * @author ranger
 */
public class KafkaLocalMessageDispatcherFactory extends AbstractMessageDispatcherFactory<Void> implements InitializingBean, DisposableBean {

    @Autowired
    private KafkaMessageConsumerManager messageConsumerManager;

    @Autowired
    private TracerRegistry tracerRegistry;

    private MetricRegistry metrics;

    public <S extends Message, T extends Message> void dispatch(final SinkModule<S, T> module, final Void metadata, final T message) {
        messageConsumerManager.dispatch(module, message);
    }

    @Override
    public String getMetricDomain() {
        return SINK_METRIC_PRODUCER_DOMAIN;
    }

    @Override
    public BundleContext getBundleContext() {
        return null;
    }

    public TracerRegistry getTracerRegistry() {
        return tracerRegistry;
    }

    @Override
    public Tracer getTracer() {
        return getTracerRegistry().getTracer();
    }

    @Override
    public MetricRegistry getMetrics() {
        if(metrics == null) {
            metrics = new MetricRegistry();
        }
        return metrics;
    }

    public void setTracerRegistry(TracerRegistry tracerRegistry) {
        this.tracerRegistry = tracerRegistry;
    }

    @Override
    public void afterPropertiesSet() {
        onInit();
    }

    @Override
    public void destroy() {
        onDestroy();
    }

    public void setMetrics(MetricRegistry metrics) {
        this.metrics = metrics;
    }
}
