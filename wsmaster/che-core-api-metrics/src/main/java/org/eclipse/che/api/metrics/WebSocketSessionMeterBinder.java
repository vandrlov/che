/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.metrics;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.binder.MeterBinder;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.CloseReason;
import org.eclipse.che.api.core.websocket.impl.MessagesReSender;
import org.eclipse.che.api.core.websocket.impl.WebSocketEndpointStatistic;
import org.eclipse.che.api.core.websocket.impl.WebSocketSessionRegistry;

@Singleton
public class WebSocketSessionMeterBinder implements MeterBinder {

  private final WebSocketSessionRegistry webSocketSessionRegistry;
  private final MessagesReSender messagesReSender;
  private final WebSocketEndpointStatistic endpointStatistic;

  @Inject
  public WebSocketSessionMeterBinder(
      WebSocketSessionRegistry webSocketSessionRegistry,
      MessagesReSender messagesReSender,
      WebSocketEndpointStatistic endpointStatistic) {
    this.webSocketSessionRegistry = webSocketSessionRegistry;
    this.messagesReSender = messagesReSender;
    this.endpointStatistic = endpointStatistic;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder(
            "che.websocket.registry.total",
            webSocketSessionRegistry,
            WebSocketSessionRegistry::getTotalSessions)
        .tags(Tags.empty())
        .baseUnit("sessions")
        .register(registry);

    Gauge.builder(
            "che.websocket.registry.active",
            webSocketSessionRegistry,
            WebSocketSessionRegistry::getActiveSessions)
        .tags(Tags.empty())
        .baseUnit("sessions")
        .register(registry);

    Gauge.builder(
            "che.websocket.resender.delayed",
            messagesReSender,
            MessagesReSender::getDelayedReceiversNumber)
        .tags(Tags.empty())
        .baseUnit("receivers")
        .register(registry);

    Gauge.builder(
            "che.websocket.resender.delayed.queue.size.max",
            messagesReSender,
            MessagesReSender::getLongestQueueSize)
        .tags(Tags.empty())
        .register(registry);
    FunctionCounter.builder(
            "che.websocket.endpoint.opened",
            endpointStatistic,
            WebSocketEndpointStatistic::getOpenedSession)
        .tags(Tags.empty())
        .baseUnit("sessions")
        .register(registry);
    FunctionCounter.builder(
            "che.websocket.endpoint.received",
            endpointStatistic,
            value -> value.getMessagesReceived(true))
        .tags(Tags.of("knownsession", "true"))
        .baseUnit("messages")
        .register(registry);
    FunctionCounter.builder(
            "che.websocket.endpoint.received",
            endpointStatistic,
            value -> value.getMessagesReceived(false))
        .tags(Tags.of("knownsession", "false"))
        .baseUnit("messages")
        .register(registry);

    FunctionCounter.builder(
            "che.websocket.endpoint.received.bytes",
            endpointStatistic,
            value -> value.getBytesReceived(false))
        .tags(Tags.of("knownsession", "false"))
        .register(registry);

    FunctionCounter.builder(
            "che.websocket.endpoint.received.bytes",
            endpointStatistic,
            value -> value.getBytesReceived(true))
        .tags(Tags.of("knownsession", "true"))
        .register(registry);

    FunctionCounter.builder(
            "che.websocket.endpoint.sent",
            endpointStatistic,
            WebSocketEndpointStatistic::getMessagesSent)
        .tags(Tags.empty())
        .baseUnit("messages")
        .register(registry);
    FunctionCounter.builder(
            "che.websocket.endpoint.sent",
            endpointStatistic,
            WebSocketEndpointStatistic::getBytesSentSuccessfully)
        .tags(Tags.of("result", "successful"))
        .baseUnit(BaseUnits.BYTES)
        .register(registry);
    FunctionCounter.builder(
            "che.websocket.endpoint.sent",
            endpointStatistic,
            WebSocketEndpointStatistic::getBytesSentUnSuccessfully)
        .tags(Tags.of("result", "fail"))
        .baseUnit(BaseUnits.BYTES)
        .register(registry);
    FunctionCounter.builder(
            "che.websocket.endpoint.error",
            endpointStatistic,
            value -> value.getErrorsReceived(true))
        .tags(Tags.of("knownsession", "true"))
        .baseUnit("messages")
        .register(registry);
    FunctionCounter.builder(
            "che.websocket.endpoint.error",
            endpointStatistic,
            value -> value.getErrorsReceived(false))
        .tags(Tags.of("knownsession", "false"))
        .baseUnit("messages")
        .register(registry);

    for (CloseReason.CloseCodes closeCode : CloseReason.CloseCodes.class.getEnumConstants()) {
      FunctionCounter.builder(
              "che.websocket.endpoint.close",
              endpointStatistic,
              value -> value.getClosedSession(new CloseReason(closeCode, ""), false))
          .tags(Tags.of("reason", closeCode.toString(), "knownsession", "false"))
          .baseUnit("messages")
          .register(registry);
      FunctionCounter.builder(
              "che.websocket.endpoint.close",
              endpointStatistic,
              value -> value.getClosedSession(new CloseReason(closeCode, ""), true))
          .tags(Tags.of("reason", closeCode.toString(), "knownsession", "true"))
          .baseUnit("messages")
          .register(registry);
    }
  }
}
