/*
 * SonarLint Server API
 * Copyright (C) 2016-2021 SonarSource SA
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
package org.sonarsource.sonarlint.core.serverapi.branches;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;
import org.sonarsource.sonarlint.core.serverapi.ServerApiHelper;

public class ProjectBranchesApi {
  private static final SonarLintLogger LOG = SonarLintLogger.get();

  private static final String LIST_ALL_PROJECT_BRANCHES_URL = "/api/project_branches/list";
  private final ServerApiHelper helper;

  public ProjectBranchesApi(ServerApiHelper helper) {
    this.helper = helper;
  }

  public Collection<String> getAllBranchNames(String projectKey) {
    var response = helper.get(LIST_ALL_PROJECT_BRANCHES_URL + "?project=" + projectKey);
    var bodyAsString = response.bodyAsString();
    return getBranchNamesFromResponse(bodyAsString);
  }

  private static Collection<String> getBranchNamesFromResponse(String bodyAsString) {
    Collection<String> parsedBranchNames = new ArrayList<>();
    try {
      var root = JsonParser.parseString(bodyAsString).getAsJsonObject();
      var branches = root.get("branches").getAsJsonArray();

      for (JsonElement el : branches) {
        var branch = el.getAsJsonObject();
        var element = branch.get("name");
        if (element == null) {
          throw new IllegalStateException("Failed to parse response. Missing field 'name'.");
        }
        var name = element.getAsString();
        parsedBranchNames.add(name);
      }

    } catch (Exception e) {
      LOG.error("Failed to parse SonarQube branches list response", e);
      return Collections.emptyList();
    }
    return parsedBranchNames;
  }

}