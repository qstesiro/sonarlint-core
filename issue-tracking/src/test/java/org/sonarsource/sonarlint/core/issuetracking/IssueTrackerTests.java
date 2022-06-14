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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

class IssueTrackerTests {

  private final IssueTrackerCache cache = new InMemoryIssueTrackerCache();

  private final CachingIssueTracker tracker = new CachingIssueTracker(cache);

  private final String file1 = "dummyFile1";

  // note: these mock trackables are used by many test cases,
  // with their line numbers to distinguish their identities.
  private final Trackable trackable1 = builder().line(1).textRangeHash(1).build();
  private final Trackable trackable2 = builder().line(2).textRangeHash(2).build();

  static class MockTrackable implements Trackable {
    String ruleKey = "";
    Integer line = null;
    String message = "";
    Integer textRangeHash = null;
    TextRange textRange = null;
    Integer lineHash = null;
    Long creationDate = null;
    String serverIssueKey = null;
    boolean resolved = false;
    String severity = "MAJOR";
    String type = "BUG";

    static int counter = Integer.MIN_VALUE;

    MockTrackable ruleKey(String ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    @Override
    public String getRuleKey() {
      return ruleKey;
    }

    MockTrackable line(@Nullable Integer line) {
      this.line = line;
      return this;
    }

    @Override
    public Integer getLine() {
      return line;
    }

    MockTrackable message(String message) {
      this.message = message;
      return this;
    }

    @Override
    public String getMessage() {
      return message;
    }

    MockTrackable textRangeHash(@Nullable Integer textRangeHash) {
      this.textRangeHash = textRangeHash;
      return this;
    }

    @Override
    public Integer getTextRangeHash() {
      return textRangeHash;
    }

    MockTrackable textRange(TextRange textRange) {
      this.textRange = textRange;
      return this;
    }

    @Override
    public TextRange getTextRange() {
      return textRange;
    }

    MockTrackable lineHash(Integer lineHash) {
      this.lineHash = lineHash;
      return this;
    }

    @Override
    public Integer getLineHash() {
      return lineHash;
    }

    MockTrackable creationDate(Long creationDate) {
      this.creationDate = creationDate;
      return this;
    }

    @Override
    public Long getCreationDate() {
      return creationDate;
    }

    MockTrackable serverIssueKey(String serverIssueKey) {
      this.serverIssueKey = serverIssueKey;
      return this;
    }

    @Override
    public String getServerIssueKey() {
      return serverIssueKey;
    }

    MockTrackable resolved(boolean resolved) {
      this.resolved = resolved;
      return this;
    }

    @Override
    public boolean isResolved() {
      return resolved;
    }

    MockTrackable severity(String severity) {
      this.severity = severity;
      return this;
    }

    @Override
    public String getSeverity() {
      return severity;
    }

    MockTrackable type(String type) {
      this.type = type;
      return this;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public Object getClientObject() {
      return null;
    }

    MockTrackable copy() {
      return builder()
        .line(line)
        .message(message)
        .textRangeHash(textRangeHash)
        .textRange(textRange)
        .lineHash(lineHash)
        .ruleKey(ruleKey)
        .creationDate(creationDate)
        .serverIssueKey(serverIssueKey)
        .resolved(resolved)
        .severity(severity)
        .type(type);
    }

    Trackable build() {

      if (message.isEmpty()) {
        this.message("m" + (counter++));
      }

      return this;
    }
  }

  private static MockTrackable builder() {
    return new MockTrackable();
  }

  @BeforeEach
  void setUp() {
    cache.clear();
  }

  @Test
  void should_track_first_trackables_exactly() {
    Collection<Trackable> trackables = Arrays.asList(mock(Trackable.class), mock(Trackable.class));
    tracker.matchAndTrackRaws(file1, trackables);
    assertThat(cache.getCurrentTrackables(file1)).isEqualTo(trackables);
  }

  @Test
  void should_preserve_known_standalone_trackables_with_null_date() {
    Collection<Trackable> trackables = Arrays.asList(trackable1, trackable2);
    tracker.matchAndTrackRaws(file1, trackables);
    tracker.matchAndTrackRaws(file1, trackables);

    Collection<Trackable> next = cache.getCurrentTrackables(file1);
    assertThat(next).extracting(Trackable::getLine).containsExactlyInAnyOrder(trackable1.getLine(), trackable2.getLine());
    assertThat(next).extracting(Trackable::getCreationDate).containsExactlyInAnyOrder(null, null);
  }

  @Test
  void should_add_creation_date_for_leaked_trackables() {
    var start = System.currentTimeMillis();

    tracker.matchAndTrackRaws(file1, Collections.singletonList(trackable1));
    tracker.matchAndTrackRaws(file1, Arrays.asList(trackable1, trackable2));

    Collection<Trackable> next = cache.getCurrentTrackables(file1);
    assertThat(next).extracting(Trackable::getLine).contains(trackable1.getLine(), trackable2.getLine());

    assertThat(next).extracting(Trackable::getCreationDate).containsOnlyOnce((Long) null);

    var leaked = next.stream().filter(t -> t.getCreationDate() != null).findFirst().get();
    assertThat(leaked.getCreationDate()).isGreaterThanOrEqualTo(start);
    assertThat(leaked.getLine()).isEqualTo(trackable2.getLine());
  }

  @Test
  void should_drop_disappeared_issues() {
    tracker.matchAndTrackRaws(file1, Arrays.asList(trackable1, trackable2));
    tracker.matchAndTrackRaws(file1, Collections.singletonList(trackable1));

    Collection<Trackable> next = cache.getCurrentTrackables(file1);
    assertThat(next).extracting(Trackable::getLine).containsExactly(trackable1.getLine());
  }

  @Test
  void should_not_match_trackables_with_different_rule_key() {
    var ruleKey = "dummy ruleKey";
    var base = builder()
      .line(7)
      .message("dummy message")
      .textRangeHash(11)
      .lineHash(13)
      .ruleKey(ruleKey)
      .serverIssueKey("dummy serverIssueKey")
      .creationDate(17L);

    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.build()));
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.ruleKey(ruleKey + "x").build()));
  }

  @Test
  void should_treat_new_issues_as_leak_when_old_issues_disappeared() {
    var start = System.currentTimeMillis();

    tracker.matchAndTrackRaws(file1, Collections.singletonList(trackable1));
    tracker.matchAndTrackRaws(file1, Collections.singletonList(trackable2));

    Collection<Trackable> next = cache.getCurrentTrackables(file1);
    assertThat(next).extracting(Trackable::getLine).containsExactly(trackable2.getLine());

    var leaked = next.stream().filter(t -> t.getCreationDate() != null).findFirst().get();
    assertThat(leaked.getCreationDate()).isGreaterThanOrEqualTo(start);
  }

  @Test
  void should_match_by_line_and_text_range_hash() {
    var base = builder().ruleKey("dummy ruleKey");
    var line = 7;
    var textRangeHash = 11;
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().line(line).textRangeHash(textRangeHash).build()));

    var differentLine = base.copy().line(line + 1).textRangeHash(textRangeHash).build();
    var differentTextRangeHash = base.copy().line(line).textRangeHash(textRangeHash + 1).build();
    var differentBoth = base.copy().line(line + 1).textRangeHash(textRangeHash + 1).build();
    var same = base.copy().line(line).textRangeHash(textRangeHash).build();
    tracker.matchAndTrackRaws(file1, Arrays.asList(differentLine, differentTextRangeHash, differentBoth, same));

    Collection<Trackable> current = cache.getCurrentTrackables(file1);
    assertThat(current).hasSize(4);
    assertThat(current)
      .extracting("line", "textRangeHash")
      .containsOnlyOnce(tuple(line, textRangeHash));
  }

  @Test
  void should_match_by_line_and_line_hash() {
    var base = builder().ruleKey("dummy ruleKey");
    var line = 7;
    var lineHash = 11;
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().line(line).lineHash(lineHash).build()));

    var differentLine = base.copy().line(line + 1).lineHash(lineHash).build();
    var differentLineHash = base.copy().line(line).lineHash(lineHash + 1).build();
    var differentBoth = base.copy().line(line + 1).lineHash(lineHash + 1).build();
    var same = base.copy().line(line).lineHash(lineHash).build();
    tracker.matchAndTrackRaws(file1, Arrays.asList(differentLine, differentLineHash, differentBoth, same));

    Collection<Trackable> current = cache.getCurrentTrackables(file1);
    assertThat(current).hasSize(4);
    assertThat(current)
      .extracting("line", "lineHash")
      .containsOnlyOnce(tuple(line, lineHash));
  }

  @Test
  void should_match_by_line_and_message() {
    var base = builder().ruleKey("dummy ruleKey");
    var line = 7;
    var message = "should make this condition not always false";
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().line(line).message(message).build()));

    var differentLine = base.copy().line(line + 1).message(message).build();
    var differentMessage = base.copy().line(line).message(message + "x").build();
    var differentBoth = base.copy().line(line + 1).message(message + "x").build();
    var same = base.copy().line(line).message(message).build();
    tracker.matchAndTrackRaws(file1, Arrays.asList(differentLine, differentMessage, differentBoth, same));

    Collection<Trackable> current = cache.getCurrentTrackables(file1);
    assertThat(current).hasSize(4);
    assertThat(current)
      .extracting("line", "message")
      .containsOnlyOnce(tuple(line, message));
  }

  @Test
  void should_match_by_text_range_hash() {
    var base = builder().ruleKey("dummy ruleKey").textRangeHash(11);
    var newLine = 7;

    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().line(newLine + 3).build()));
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.line(newLine).build()));

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("line")
      .containsExactly(newLine);
  }

  @Test
  void should_match_by_line_hash() {
    var base = builder().ruleKey("dummy ruleKey").lineHash(11);
    var newLine = 7;

    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().line(newLine + 3).build()));
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.line(newLine).build()));

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("line")
      .containsExactly(newLine);
  }

  @Test
  void should_match_local_issues_by_line_hash() {
    var lineContent = "dummy content";
    var newLine = 7;

    var trackable = builder().line(newLine + 3).lineHash(lineContent.hashCode()).build();
    var movedTrackable = builder().line(newLine).lineHash(lineContent.hashCode()).build();
    var nonMatchingTrackable = builder().lineHash((lineContent + "x").hashCode()).build();

    tracker.matchAndTrackRaws(file1, Collections.singletonList(trackable));
    tracker.matchAndTrackRaws(file1, Arrays.asList(movedTrackable, nonMatchingTrackable));

    assertThat(movedTrackable.getLineHash()).isEqualTo(trackable.getLineHash());
    assertThat(movedTrackable.getLineHash()).isNotEqualTo(nonMatchingTrackable.getLineHash());

    Collection<Trackable> next = cache.getCurrentTrackables(file1);

    // matched trackable has no date
    assertThat(next.stream().filter(t -> t.getCreationDate() == null))
      .extracting("line", "lineHash")
      .containsOnly(tuple(movedTrackable.getLine(), movedTrackable.getLineHash()));

    // unmatched trackable has a date -> it is a leak
    assertThat(next.stream().filter(t -> t.getCreationDate() != null))
      .extracting("line", "lineHash")
      .containsOnly(tuple(nonMatchingTrackable.getLine(), nonMatchingTrackable.getLineHash()));
  }

  @Test
  void should_match_server_issues_by_line_hash() {
    var ruleKey = "dummy ruleKey";
    var message = "dummy message";
    var lineContent = "dummy content";
    var newLine = 7;

    var trackable = builder().ruleKey(ruleKey).message(message).line(newLine).lineHash(lineContent.hashCode()).build();
    var movedTrackable = builder().line(newLine).lineHash(lineContent.hashCode()).build();
    var nonMatchingTrackable = builder().lineHash((lineContent + "x").hashCode()).build();

    tracker.matchAndTrackRaws(file1, Collections.singletonList(trackable));
    tracker.matchAndTrackAsBase(file1, Arrays.asList(movedTrackable, nonMatchingTrackable));

    assertThat(movedTrackable.getLineHash()).isEqualTo(trackable.getLineHash());
    assertThat(movedTrackable.getLineHash()).isNotEqualTo(nonMatchingTrackable.getLineHash());

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("line", "lineHash", "serverIssueKey", "resolved")
      .containsOnly(tuple(newLine, movedTrackable.getLineHash(), movedTrackable.getServerIssueKey(), movedTrackable.isResolved()));
  }

  @Test
  void should_match_server_issues_by_range_hash() {
    var ruleKey = "dummy ruleKey";
    var message = "dummy message";
    var rangeContent = "dummy content";
    var newLine = 7;

    var trackable = builder().ruleKey(ruleKey).message(message).line(newLine).textRangeHash(rangeContent.hashCode()).build();
    var movedTrackable = builder().line(newLine).textRangeHash(rangeContent.hashCode()).build();
    var nonMatchingTrackable = builder().textRangeHash((rangeContent + "x").hashCode()).build();

    tracker.matchAndTrackRaws(file1, Collections.singletonList(trackable));
    tracker.matchAndTrackAsBase(file1, Arrays.asList(movedTrackable, nonMatchingTrackable));

    assertThat(movedTrackable.getTextRangeHash()).isEqualTo(trackable.getTextRangeHash());
    assertThat(movedTrackable.getTextRangeHash()).isNotEqualTo(nonMatchingTrackable.getTextRangeHash());

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("line", "lineHash", "serverIssueKey", "resolved")
      .containsOnly(tuple(newLine, movedTrackable.getLineHash(), movedTrackable.getServerIssueKey(), movedTrackable.isResolved()));
  }

  @Test
  void should_match_by_server_issue_key() {
    var base = builder().ruleKey("dummy ruleKey").serverIssueKey("dummy server issue key");
    var newLine = 7;

    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().line(newLine + 3).build()));
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.line(newLine).build()));

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("line")
      .containsExactly(newLine);
  }

  @Test
  void should_preserve_creation_date() {
    var base = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11);
    var creationDate = 123L;

    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().creationDate(creationDate).build()));
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("creationDate")
      .containsExactly(creationDate);
  }

  @Test
  void should_preserve_creation_date_of_leaked_issues_in_connected_mode() {
    Long leakCreationDate = 1L;
    var leak = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11).creationDate(leakCreationDate).build();

    // fake first analysis, trackable has a date
    tracker.matchAndTrackRaws(file1, Collections.singletonList(leak));

    // fake server issue tracking
    tracker.matchAndTrackAsBase(file1, Collections.emptyList());

    assertThat(cache.getCurrentTrackables(file1)).extracting("creationDate").containsOnly(leakCreationDate);
  }

  @Test
  void should_preserve_server_issue_details() {
    var base = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11);
    var serverIssueKey = "dummy serverIssueKey";
    var resolved = true;

    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().serverIssueKey(serverIssueKey).resolved(resolved).build()));
    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("serverIssueKey", "resolved")
      .containsExactly(tuple(serverIssueKey, resolved));
  }

  @Test
  void should_drop_server_issue_reference_if_gone() {
    var base = builder().ruleKey("dummy ruleKey").line(7).textRangeHash(11);
    var serverIssueKey = "dummy serverIssueKey";
    var resolved = true;

    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().serverIssueKey(serverIssueKey).resolved(resolved).build()));
    tracker.matchAndTrackAsBase(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("serverIssueKey", "resolved")
      .containsExactly(tuple(null, false));
  }

  @Test
  void should_update_server_issue_details() {
    var serverIssueKey = "dummy serverIssueKey";
    var resolved = true;
    var base = builder().ruleKey("dummy ruleKey").serverIssueKey(serverIssueKey).resolved(resolved);

    tracker.matchAndTrackRaws(file1, Collections.singletonList(base.copy().resolved(!resolved).build()));
    tracker.matchAndTrackAsBase(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1))
      .extracting("serverIssueKey", "resolved")
      .containsExactly(tuple(serverIssueKey, resolved));
  }

  @Test
  void should_clear_server_issue_details_if_disappeared() {
    var resolved = true;
    var serverIssueTrackable = builder().ruleKey("dummy ruleKey")
      .serverIssueKey("dummy serverIssueKey").resolved(resolved).creationDate(1L).build();

    var start = System.currentTimeMillis();

    tracker.matchAndTrackRaws(file1, Collections.emptyList());
    tracker.matchAndTrackRaws(file1, Collections.singletonList(serverIssueTrackable));

    Collection<Trackable> trackables = cache.getCurrentTrackables(file1);
    assertThat(trackables)
      .extracting("serverIssueKey", "resolved")
      .containsExactly(tuple(null, !resolved));
    assertThat(trackables.iterator().next().getCreationDate()).isGreaterThanOrEqualTo(start);
  }

  @Test
  void should_ignore_server_issues_when_there_are_no_local() {
    var serverIssueKey = "dummy serverIssueKey";
    var resolved = true;
    var base = builder().ruleKey("dummy ruleKey").serverIssueKey(serverIssueKey).resolved(resolved);

    tracker.matchAndTrackRaws(file1, Collections.emptyList());
    tracker.matchAndTrackAsBase(file1, Collections.singletonList(base.build()));

    assertThat(cache.getCurrentTrackables(file1)).isEmpty();
  }
}
