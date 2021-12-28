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
package org.sonarsource.sonarlint.core.serverapi.qualityprofile;

import java.util.List;
import java.util.stream.Collectors;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;
import org.sonarsource.sonarlint.core.serverapi.ServerApiHelper;
import org.sonarsource.sonarlint.core.serverapi.UrlUtils;
import org.sonarsource.sonarlint.core.serverapi.exception.NotFoundException;
import org.sonarsource.sonarlint.core.serverapi.exception.ProjectNotFoundException;

public class QualityProfileApi {
  private static final SonarLintLogger LOG = SonarLintLogger.get();
  private static final String DEFAULT_QP_SEARCH_URL = "/api/qualityprofiles/search.protobuf";

  private final ServerApiHelper helper;

  public QualityProfileApi(ServerApiHelper helper) {
    this.helper = helper;
  }

  public List<QualityProfile> getQualityProfiles(String projectKey) {
    Qualityprofiles.SearchWsResponse qpResponse;
    var url = new StringBuilder();
    url.append(DEFAULT_QP_SEARCH_URL + "?project=");
    url.append(UrlUtils.urlEncode(projectKey));
    helper.getOrganizationKey()
      .ifPresent(org -> url.append("&organization=").append(UrlUtils.urlEncode(org)));
    try {
      qpResponse = ServerApiHelper.processTimed(
        () -> helper.get(url.toString()),
        response -> Qualityprofiles.SearchWsResponse.parseFrom(response.bodyAsStream()),
        duration -> LOG.debug("Downloaded project quality profiles in {}ms", duration));
      return qpResponse.getProfilesList().stream().map(QualityProfileApi::adapt).collect(Collectors.toList());
    } catch (NotFoundException e) {
      throw new ProjectNotFoundException(projectKey, helper.getOrganizationKey().orElse(null));
    }
  }

  private static QualityProfile adapt(Qualityprofiles.SearchWsResponse.QualityProfile wsQualityProfile) {
    return new QualityProfile(
      wsQualityProfile.getIsDefault(),
      wsQualityProfile.getKey(),
      wsQualityProfile.getName(),
      wsQualityProfile.getLanguage(),
      wsQualityProfile.getLanguageName(),
      wsQualityProfile.getActiveRuleCount(),
      wsQualityProfile.getRulesUpdatedAt(),
      wsQualityProfile.getUserUpdatedAt());
  }
}
