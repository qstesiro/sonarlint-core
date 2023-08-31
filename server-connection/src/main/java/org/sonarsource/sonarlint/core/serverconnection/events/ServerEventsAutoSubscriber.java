/*
 * SonarLint Core - Server Connection
 * Copyright (C) 2016-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.sonarlint.core.serverconnection.events;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.commons.log.ClientLogOutput;
import org.sonarsource.sonarlint.core.serverapi.ServerApi;
import org.sonarsource.sonarlint.core.commons.push.ServerEvent;
import org.sonarsource.sonarlint.core.serverapi.push.IssueChangedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.RuleSetChangedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.SecurityHotspotChangedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.SecurityHotspotClosedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.SecurityHotspotRaisedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.TaintVulnerabilityClosedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.TaintVulnerabilityRaisedEvent;
import org.sonarsource.sonarlint.core.serverapi.stream.EventStream;
import org.sonarsource.sonarlint.core.serverconnection.ConnectionStorage;
import org.sonarsource.sonarlint.core.serverconnection.events.hotspot.UpdateStorageOnSecurityHotspotChanged;
import org.sonarsource.sonarlint.core.serverconnection.events.hotspot.UpdateStorageOnSecurityHotspotClosed;
import org.sonarsource.sonarlint.core.serverconnection.events.hotspot.UpdateStorageOnSecurityHotspotRaised;
import org.sonarsource.sonarlint.core.serverconnection.events.issue.UpdateStorageOnIssueChanged;
import org.sonarsource.sonarlint.core.serverconnection.events.ruleset.UpdateStorageOnRuleSetChanged;
import org.sonarsource.sonarlint.core.serverconnection.events.taint.UpdateStorageOnTaintVulnerabilityClosed;
import org.sonarsource.sonarlint.core.serverconnection.events.taint.UpdateStorageOnTaintVulnerabilityRaised;

public class ServerEventsAutoSubscriber {

  private final AtomicReference<EventStream> eventStream = new AtomicReference<>();
  private final Set<String> subscribedProjectKeys = new LinkedHashSet<>();
  private final EventDispatcher coreEventRouter;
  private final Set<Language> enabledLanguages;

  public ServerEventsAutoSubscriber(ConnectionStorage storage, Set<Language> enabledLanguages) {
    coreEventRouter = new EventDispatcher()
      .dispatch(RuleSetChangedEvent.class, new UpdateStorageOnRuleSetChanged(storage))
      .dispatch(IssueChangedEvent.class, new UpdateStorageOnIssueChanged(storage))
      .dispatch(TaintVulnerabilityRaisedEvent.class, new UpdateStorageOnTaintVulnerabilityRaised(storage))
      .dispatch(TaintVulnerabilityClosedEvent.class, new UpdateStorageOnTaintVulnerabilityClosed(storage))
      .dispatch(SecurityHotspotRaisedEvent.class, new UpdateStorageOnSecurityHotspotRaised(storage))
      .dispatch(SecurityHotspotChangedEvent.class, new UpdateStorageOnSecurityHotspotChanged(storage))
      .dispatch(SecurityHotspotClosedEvent.class, new UpdateStorageOnSecurityHotspotClosed(storage));
    this.enabledLanguages = enabledLanguages;
  }

  public void subscribePermanently(ServerApi serverApi, Set<String> projectKeys, Consumer<ServerEvent> eventConsumer,
    ClientLogOutput clientLogOutput) {
    cancelSubscription();
    if (!projectKeys.isEmpty() && !enabledLanguages.isEmpty()) {
      subscribedProjectKeys.addAll(projectKeys);
      attemptSubscription(serverApi, subscribedProjectKeys, enabledLanguages, e -> notifyHandlers(e, eventConsumer), clientLogOutput);
    }
  }

  public void subscribeNew(ServerApi serverApi, Set<String> possiblyNewProjectKeys, Consumer<ServerEvent> eventConsumer,
    ClientLogOutput clientLogOutput) {
    if (!possiblyNewProjectKeys.isEmpty() && !subscribedProjectKeys.containsAll(possiblyNewProjectKeys) && !enabledLanguages.isEmpty()) {
      cancelSubscription();
      subscribedProjectKeys.addAll(possiblyNewProjectKeys);
      attemptSubscription(serverApi, subscribedProjectKeys, enabledLanguages, e -> notifyHandlers(e, eventConsumer), clientLogOutput);
    }
  }

  private void notifyHandlers(ServerEvent serverEvent, Consumer<ServerEvent> clientEventConsumer) {
    coreEventRouter.handle(serverEvent);
    clientEventConsumer.accept(serverEvent);
  }

  private void attemptSubscription(ServerApi serverApi, Set<String> projectKeys, Set<Language> enabledLanguages, ServerEventHandler<ServerEvent> eventConsumer,
    ClientLogOutput clientLogOutput) {
    eventStream.set(serverApi.push().subscribe(projectKeys, enabledLanguages, eventConsumer::handle, clientLogOutput));
  }

  public void resubscribe(ServerApi serverApi, Consumer<ServerEvent> eventConsumer, ClientLogOutput clientLogOutput) {
    cancelSubscription();
    if (!subscribedProjectKeys.isEmpty() && !enabledLanguages.isEmpty()) {
      attemptSubscription(serverApi, subscribedProjectKeys, enabledLanguages, e -> notifyHandlers(e, eventConsumer), clientLogOutput);
    }
  }

  public void unsubscribe(ServerApi api, String projectKey, Consumer<ServerEvent> eventConsumer, ClientLogOutput clientLogOutput) {

  }

  private void cancelSubscription() {
    subscribedProjectKeys.clear();
    if (eventStream.get() != null) {
      eventStream.get().close();
    }
  }

  public void stop() {
    cancelSubscription();
  }
}
