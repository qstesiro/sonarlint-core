/*
 * SonarLint Issue Tracking
 * Copyright (C) 2016-2022 SonarSource SA
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
package org.sonarsource.sonarlint.core.issuetracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class CachingIssueTracker {

  private final IssueTrackerCache cache;

  public CachingIssueTracker(IssueTrackerCache cache) {
    this.cache = cache;
  }

  /**
   * Match a new set of trackables to current state.
   * If this is the first analysis, leave creation date as null.
   *
   * @param file the file analyzed
   * @param trackables the trackables in the file
   */
  public synchronized Collection<Trackable> matchAndTrackRaws(String file, Collection<Trackable> trackables) {
    Collection<Trackable> tracked;
    if (cache.isFirstAnalysis(file)) {
      tracked = trackables;
    } else {
      tracked = apply(cache.getCurrentTrackables(file), trackables, false);
    }
    cache.put(file, tracked);
    return tracked;
  }

  /**
   * "Rebase" current trackables against given trackables.
   *
   * @param file the file analyzed
   * @param trackables the trackables in the file
   */
  public synchronized Collection<Trackable> matchAndTrackAsBase(String file, Collection<Trackable> trackables) {
    // store issues (ProtobufIssueTrackable) are of no use since they can't be used in markers. There should have been
    // an analysis before that set the live issues for the file (even if it is empty)
    Collection<Trackable> current = cache.getLiveOrFail(file);
    if (current.isEmpty()) {
      // whatever is the base, if current is empty, then nothing to do
      return Collections.emptyList();
    }
    var tracked = apply(trackables, current, true);
    cache.put(file, tracked);
    return tracked;
  }

  /**
   * Local issue tracking: baseIssues are existing issue, nextIssues are raw issues coming from the analysis.
   * Server issue tracking: baseIssues are server issues, nextIssues are the existing issue, coming from local issue tracking.
   */
  private static Collection<Trackable> apply(Collection<Trackable> baseIssues, Collection<Trackable> nextIssues, boolean inheritSeverity) {
    Collection<Trackable> trackedIssues = new ArrayList<>();
    var tracking = new Tracker<>().track(() -> nextIssues, () -> baseIssues);

    tracking.getMatchedLefts().entrySet().stream()
      .map(e -> new CombinedTrackable(e.getValue(), e.getKey(), inheritSeverity))
      .forEach(trackedIssues::add);

    for (Trackable next : tracking.getUnmatchedLefts()) {
      if (next.getServerIssueKey() != null) {
        // not matched with server anymore
        next = new DisconnectedTrackable(next);
      } else if (next.getCreationDate() == null) {
        // first time we see this issue locally
        next = new LeakedTrackable(next);
      }
      trackedIssues.add(next);
    }

    return trackedIssues;
  }

  public void clear() {
    cache.clear();
  }

  public void shutdown() {
    cache.shutdown();
  }

}
