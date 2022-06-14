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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Store the result of matching of issues.
 *
 * @param <L> type of the "left" issues
 * @param <R> type of the "right" issues
 */
public class IssueMatchingResult<L, R> {

  /**
   * Matched issues -> a left issue is associated to a right issue
   */
  private final IdentityHashMap<L, R> leftToRight = new IdentityHashMap<>();
  private final IdentityHashMap<R, L> rightToLeft = new IdentityHashMap<>();

  private final Collection<L> lefts;
  private final Collection<R> rights;

  public IssueMatchingResult(Collection<L> lefts, Collection<R> rights) {
    this.lefts = lefts;
    this.rights = rights;
  }

  /**
   * Returns an Iterable to be traversed when matching issues. That means
   * that the traversal does not fail if method {@link #match(Trackable, Trackable)}
   * is called.
   */
  public Iterable<L> getUnmatchedLefts() {
    List<L> result = new ArrayList<>();
    for (L r : lefts) {
      if (!leftToRight.containsKey(r)) {
        result.add(r);
      }
    }
    return result;
  }

  public Map<L, R> getMatchedLefts() {
    return leftToRight;
  }

  /**
   * The right issues that are not matched by a left issue.
   */
  public Iterable<R> getUnmatchedRights() {
    List<R> result = new ArrayList<>();
    for (R b : rights) {
      if (!rightToLeft.containsKey(b)) {
        result.add(b);
      }
    }
    return result;
  }

  void match(L left, R right) {
    leftToRight.put(left, right);
    rightToLeft.put(right, left);
  }

  boolean isComplete() {
    return leftToRight.size() == lefts.size();
  }

}
