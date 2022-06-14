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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.sonarsource.sonarlint.core.issuetracking.server.LineLevelServerIssueMatchable;
import org.sonarsource.sonarlint.core.issuetracking.server.RangeLevelServerIssueMatchable;
import org.sonarsource.sonarlint.core.issuetracking.server.ServerIssueMatchable;
import org.sonarsource.sonarlint.core.issuetracking.tracked.RangeLevelTrackedIssueMatchable;
import org.sonarsource.sonarlint.core.issuetracking.tracked.TrackedIssueMatchable;

/**
 * Match tracked issues with server issues.
 */
public class ServerIssueMatcher {

  public IssueMatchingResult<TrackedIssueMatchable, ServerIssueMatchable> track(Collection<TrackedIssueMatchable> trackedIssues, Collection<ServerIssueMatchable> serverIssues) {
    var matchingResult = new IssueMatchingResult<>(trackedIssues, serverIssues);

    // 1. match issues with same server issue key
    match(matchingResult, ServerIssueSearchKey::fromTracked, ServerIssueSearchKey::fromServer);

    // 2. match issues with same rule, same line and same text range hash, but not necessarily with same message
    match(matchingResult, LineAndTextRangeHashKey::fromTracked, LineAndTextRangeHashKey::fromServer);

    // 3. match issues with same rule, same message and same text range hash
    match(matchingResult, TextRangeHashAndMessageKey::fromTracked, TextRangeHashAndMessageKey::fromServer);

    // 4. match issues with same rule, same line and same message
    match(matchingResult, LineAndMessageKey::fromTracked, LineAndMessageKey::fromServer);

    // 5. match issues with same rule and same text range hash but different line and different message.
    // See SONAR-2812
    match(matchingResult, TextRangeHashKey::fromTracked, TextRangeHashKey::fromServer);

    // 6. match issues with same rule, same line and same line hash
    match(matchingResult, LineAndLineHashKey::fromTracked, LineAndLineHashKey::fromServer);

    // 7. match issues with same rule and same same line hash
    match(matchingResult, LineHashKey::fromTracked, LineHashKey::fromServer);

    return matchingResult;
  }

  private static void match(IssueMatchingResult<TrackedIssueMatchable, ServerIssueMatchable> matchingResult, Function<TrackedIssueMatchable, SearchKey> leftKeyFactory,
    Function<ServerIssueMatchable, SearchKey> rightKeyFactory) {
    if (matchingResult.isComplete()) {
      return;
    }

    Map<SearchKey, List<ServerIssueMatchable>> baseSearch = new HashMap<>();
    for (ServerIssueMatchable right : matchingResult.getUnmatchedRights()) {
      var searchKey = rightKeyFactory.apply(right);
      if (searchKey != null) {
        baseSearch.computeIfAbsent(searchKey, __ -> new ArrayList<>()).add(right);
      }
    }

    for (TrackedIssueMatchable left : matchingResult.getUnmatchedLefts()) {
      var leftKey = leftKeyFactory.apply(left);
      if (leftKey != null) {
        Collection<ServerIssueMatchable> rightCandidates = baseSearch.get(leftKey);
        if (rightCandidates != null && !rightCandidates.isEmpty()) {
          // TODO taking the first one. Could be improved if there are more than 2 issues on the same line.
          // Message could be checked to take the best one.
          var match = rightCandidates.iterator().next();
          matchingResult.match(left, match);
          baseSearch.get(leftKey).remove(match);
        }
      }
    }
  }

  private interface SearchKey {
  }

  private static class LineAndTextRangeHashKey implements SearchKey {
    private final String ruleKey;
    private final int line;
    private final String rangeHash;

    LineAndTextRangeHashKey(String ruleKey, int line, String rangeHash) {
      this.ruleKey = ruleKey;
      this.line = line;
      this.rangeHash = rangeHash;
    }

    static LineAndTextRangeHashKey fromTracked(TrackedIssueMatchable tracked) {
      if (tracked instanceof RangeLevelTrackedIssueMatchable) {
        return new LineAndTextRangeHashKey(tracked.getRuleKey(), ((RangeLevelTrackedIssueMatchable) tracked).getStartLine(),
          ((RangeLevelTrackedIssueMatchable) tracked).getRangeHash());
      }
      return null;
    }

    static LineAndTextRangeHashKey fromServer(ServerIssueMatchable server) {
      if (server instanceof RangeLevelServerIssueMatchable) {
        return new LineAndTextRangeHashKey(server.getRuleKey(), ((RangeLevelServerIssueMatchable) server).getStartLine(), ((RangeLevelServerIssueMatchable) server).getRangeHash());
      }
      return null;
    }

    @Override
    public int hashCode() {
      return Objects.hash(line, rangeHash, ruleKey);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof LineAndTextRangeHashKey)) {
        return false;
      }
      LineAndTextRangeHashKey other = (LineAndTextRangeHashKey) obj;
      return line == other.line && Objects.equals(rangeHash, other.rangeHash) && Objects.equals(ruleKey, other.ruleKey);
    }

  }

  private static class LineAndLineHashKey implements SearchKey {
    private final String ruleKey;
    private final int line;
    private final String lineHash;

    LineAndLineHashKey(String ruleKey, int line, String lineHash) {
      this.ruleKey = ruleKey;
      this.line = line;
      this.lineHash = lineHash;
    }

    static LineAndLineHashKey fromTracked(TrackedIssueMatchable tracked) {
      if (tracked instanceof RangeLevelTrackedIssueMatchable) {
        return new LineAndLineHashKey(tracked.getRuleKey(), ((RangeLevelTrackedIssueMatchable) tracked).getStartLine(), ((RangeLevelTrackedIssueMatchable) tracked).getLineHash());
      }
      return null;
    }

    static LineAndLineHashKey fromServer(ServerIssueMatchable server) {
      if (server instanceof LineLevelServerIssueMatchable) {
        return new LineAndLineHashKey(server.getRuleKey(), ((LineLevelServerIssueMatchable) server).getLine(), ((LineLevelServerIssueMatchable) server).getLineHash());
      }
      return null;
    }

    @Override
    public int hashCode() {
      return Objects.hash(line, lineHash, ruleKey);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof LineAndLineHashKey)) {
        return false;
      }
      LineAndLineHashKey other = (LineAndLineHashKey) obj;
      return line == other.line && Objects.equals(lineHash, other.lineHash) && Objects.equals(ruleKey, other.ruleKey);
    }

  }

  private static class LineHashKey implements SearchKey {
    private final String ruleKey;
    private final String lineHash;

    LineHashKey(String ruleKey, String lineHash) {
      this.ruleKey = ruleKey;
      this.lineHash = lineHash;
    }

    static LineHashKey fromTracked(TrackedIssueMatchable tracked) {
      if (tracked instanceof RangeLevelTrackedIssueMatchable) {
        return new LineHashKey(tracked.getRuleKey(), ((RangeLevelTrackedIssueMatchable) tracked).getLineHash());
      }
      return null;
    }

    static LineHashKey fromServer(ServerIssueMatchable server) {
      if (server instanceof LineLevelServerIssueMatchable) {
        return new LineHashKey(server.getRuleKey(), ((LineLevelServerIssueMatchable) server).getLineHash());
      }
      return null;
    }

    @Override
    public int hashCode() {
      return Objects.hash(lineHash, ruleKey);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof LineHashKey)) {
        return false;
      }
      LineHashKey other = (LineHashKey) obj;
      return Objects.equals(lineHash, other.lineHash) && Objects.equals(ruleKey, other.ruleKey);
    }

  }

  private static class TextRangeHashAndMessageKey implements SearchKey {
    private final String ruleKey;
    private final String message;
    private final String rangeHash;

    TextRangeHashAndMessageKey(String ruleKey, String message, String rangeHash) {
      this.ruleKey = ruleKey;
      this.message = message;
      this.rangeHash = rangeHash;
    }

    static TextRangeHashAndMessageKey fromTracked(TrackedIssueMatchable tracked) {
      if (tracked instanceof RangeLevelTrackedIssueMatchable) {
        return new TextRangeHashAndMessageKey(tracked.getRuleKey(), tracked.getMessage(), ((RangeLevelTrackedIssueMatchable) tracked).getRangeHash());
      }
      return null;
    }

    static TextRangeHashAndMessageKey fromServer(ServerIssueMatchable server) {
      if (server instanceof RangeLevelServerIssueMatchable) {
        return new TextRangeHashAndMessageKey(server.getRuleKey(), server.getMessage(), ((RangeLevelServerIssueMatchable) server).getRangeHash());
      }
      return null;
    }

    @Override
    public int hashCode() {
      return Objects.hash(message, rangeHash, ruleKey);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof TextRangeHashAndMessageKey)) {
        return false;
      }
      TextRangeHashAndMessageKey other = (TextRangeHashAndMessageKey) obj;
      return Objects.equals(message, other.message) && Objects.equals(rangeHash, other.rangeHash) && Objects.equals(ruleKey, other.ruleKey);
    }

  }

  private static class LineAndMessageKey implements SearchKey {
    private final String ruleKey;
    private final String message;
    private final int line;

    LineAndMessageKey(String ruleKey, String message, int line) {
      this.ruleKey = ruleKey;
      this.message = message;
      this.line = line;
    }

    static LineAndMessageKey fromTracked(TrackedIssueMatchable tracked) {
      if (tracked instanceof RangeLevelTrackedIssueMatchable) {
        return new LineAndMessageKey(tracked.getRuleKey(), tracked.getMessage(), ((RangeLevelTrackedIssueMatchable) tracked).getStartLine());
      }
      return null;
    }

    static LineAndMessageKey fromServer(ServerIssueMatchable server) {
      if (server instanceof RangeLevelServerIssueMatchable) {
        return new LineAndMessageKey(server.getRuleKey(), server.getMessage(), ((RangeLevelServerIssueMatchable) server).getStartLine());
      }
      if (server instanceof LineLevelServerIssueMatchable) {
        return new LineAndMessageKey(server.getRuleKey(), server.getMessage(), ((LineLevelServerIssueMatchable) server).getLine());
      }
      return null;
    }

    @Override
    public int hashCode() {
      return Objects.hash(line, message, ruleKey);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof LineAndMessageKey)) {
        return false;
      }
      LineAndMessageKey other = (LineAndMessageKey) obj;
      return line == other.line && Objects.equals(message, other.message) && Objects.equals(ruleKey, other.ruleKey);
    }

  }

  private static class TextRangeHashKey implements SearchKey {
    private final String ruleKey;
    private final String rangeHash;

    TextRangeHashKey(String ruleKey, String rangeHash) {
      this.ruleKey = ruleKey;
      this.rangeHash = rangeHash;
    }

    static TextRangeHashKey fromTracked(TrackedIssueMatchable tracked) {
      if (tracked instanceof RangeLevelTrackedIssueMatchable) {
        return new TextRangeHashKey(tracked.getRuleKey(), ((RangeLevelTrackedIssueMatchable) tracked).getRangeHash());
      }
      return null;
    }

    static TextRangeHashKey fromServer(ServerIssueMatchable server) {
      if (server instanceof RangeLevelServerIssueMatchable) {
        return new TextRangeHashKey(server.getRuleKey(), ((RangeLevelServerIssueMatchable) server).getRangeHash());
      }
      return null;
    }

    @Override
    public int hashCode() {
      return Objects.hash(rangeHash, ruleKey);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof TextRangeHashKey)) {
        return false;
      }
      TextRangeHashKey other = (TextRangeHashKey) obj;
      return Objects.equals(rangeHash, other.rangeHash) && Objects.equals(ruleKey, other.ruleKey);
    }

  }

  private static class ServerIssueSearchKey implements SearchKey {
    private final String serverIssueKey;

    ServerIssueSearchKey(String serverIssueKey) {
      this.serverIssueKey = serverIssueKey;
    }

    static ServerIssueSearchKey fromTracked(TrackedIssueMatchable tracked) {
      var key = tracked.getServerIssueKey();
      if (key != null) {
        return new ServerIssueSearchKey(key);
      }
      return null;
    }

    static ServerIssueSearchKey fromServer(ServerIssueMatchable server) {
      return new ServerIssueSearchKey(server.getKey());
    }

    @Override
    public int hashCode() {
      return Objects.hash(serverIssueKey);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ServerIssueSearchKey)) {
        return false;
      }
      ServerIssueSearchKey other = (ServerIssueSearchKey) obj;
      return Objects.equals(serverIssueKey, other.serverIssueKey);
    }

  }

}
