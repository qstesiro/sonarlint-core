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
package org.sonarsource.sonarlint.core.issuetracking.tracked;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public abstract class TrackedIssueMatchable {
  private final String ruleKey;
  private final String message;
  private final String serverIssueKey;

  protected TrackedIssueMatchable(String ruleKey, String message, @Nullable String serverIssueKey) {
    this.ruleKey = ruleKey;
    this.message = message;
    this.serverIssueKey = serverIssueKey;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public String getMessage() {
    return message;
  }

  @CheckForNull
  public String getServerIssueKey() {
    return serverIssueKey;
  }

}
