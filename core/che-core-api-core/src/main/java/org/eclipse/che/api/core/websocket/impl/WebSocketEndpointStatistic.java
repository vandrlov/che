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
package org.eclipse.che.api.core.websocket.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import javax.inject.Singleton;
import javax.websocket.CloseReason;

@Singleton
public class WebSocketEndpointStatistic {

  private final AtomicLong opened = new AtomicLong(0);
  private final AtomicLong messageReceived4KnownSession = new AtomicLong(0);
  private final AtomicLong messageReceived4UnKnownSession = new AtomicLong(0);
  private final AtomicLong bytes4KnownSession = new AtomicLong(0);
  private final AtomicLong bytes4UnKnownSession = new AtomicLong(0);
  private final AtomicLong messagesSent = new AtomicLong(0);
  private final AtomicLong bytesSentSuccessfully = new AtomicLong(0);
  private final AtomicLong bytesSentUnSuccessfully = new AtomicLong(0);
  private final AtomicLong error4KnownSession = new AtomicLong(0);
  private final AtomicLong error4UnKnownSession = new AtomicLong(0);
  private final AtomicLongArray closedReasons4KnownSessions = new AtomicLongArray(16);
  private final AtomicLongArray closedReasons4UnKnownSessions = new AtomicLongArray(16);

  public void sessionOpened() {
    opened.incrementAndGet();
  }

  public void sessionClosed(CloseReason closeReason, boolean isKnownSession) {
    int closedCode = closeReason.getCloseCode().getCode() - 1000;
    if (closedCode >= 0 && closedCode < 16) {
      if (isKnownSession) {
        closedReasons4KnownSessions.incrementAndGet(closedCode);
      } else {
        closedReasons4UnKnownSessions.incrementAndGet(closedCode);
      }
    }
  }

  public void messageReceived(long bytes, boolean isKnownSession) {

    if (isKnownSession) {
      messageReceived4KnownSession.incrementAndGet();
      bytes4KnownSession.addAndGet(bytes);
    } else {
      messageReceived4UnKnownSession.incrementAndGet();
      bytes4UnKnownSession.addAndGet(bytes);
    }
  }

  public void messageSent(long bytes, boolean successfully) {
    messagesSent.incrementAndGet();
    if (successfully) {
      bytesSentSuccessfully.addAndGet(bytes);
    } else {
      bytesSentUnSuccessfully.addAndGet(bytes);
    }
  }

  public void errorReceived(boolean isKnownSession) {
    if (isKnownSession) {
      error4KnownSession.incrementAndGet();
    } else {
      error4UnKnownSession.incrementAndGet();
    }
  }

  public long getMessagesReceived(boolean isKnownSession) {
    if (isKnownSession) {
      return messageReceived4KnownSession.get();
    } else {
      return messageReceived4UnKnownSession.get();
    }
  }

  public long getMessagesSent() {
    return messagesSent.get();
  }

  public long getBytesSentUnSuccessfully() {
    return bytesSentUnSuccessfully.get();
  }

  public long getBytesSentSuccessfully() {
    return bytesSentSuccessfully.get();
  }

  public long getBytesReceived(boolean isKnownSession) {
    if (isKnownSession) {
      return bytes4KnownSession.get();
    } else {
      return bytes4UnKnownSession.get();
    }
  }

  public long getOpenedSession() {
    return opened.get();
  }

  public long getClosedSession(CloseReason closeReason, boolean isKnownSession) {
    int closedCode = closeReason.getCloseCode().getCode() - 1000;
    if (closedCode >= 0 && closedCode < 16) {
      if (isKnownSession) {
        return closedReasons4KnownSessions.get(closedCode);
      } else {
        return closedReasons4UnKnownSessions.get(closedCode);
      }
    }
    return 0;
  }

  public long getErrorsReceived(boolean isKnownSession) {
    if (isKnownSession) {
      return bytes4KnownSession.get();
    } else {
      return error4UnKnownSession.get();
    }
  }
}
