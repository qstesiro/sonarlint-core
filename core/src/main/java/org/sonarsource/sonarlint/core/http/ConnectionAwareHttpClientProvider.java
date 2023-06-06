/*
 * SonarLint Core - Implementation
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
package org.sonarsource.sonarlint.core.http;

import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import javax.inject.Singleton;
import org.sonarsource.sonarlint.core.clientapi.SonarLintClient;
import org.sonarsource.sonarlint.core.clientapi.client.connection.GetCredentialsParams;
import org.sonarsource.sonarlint.core.clientapi.client.connection.TokenDto;
import org.sonarsource.sonarlint.core.clientapi.client.connection.UsernamePasswordDto;
import org.sonarsource.sonarlint.core.commons.log.SonarLintLogger;

@Named
@Singleton
public class ConnectionAwareHttpClientProvider {

  private final SonarLintLogger logger = SonarLintLogger.get();
  private final SonarLintClient client;
  private final HttpClientProvider httpClientProvider;

  public ConnectionAwareHttpClientProvider(SonarLintClient client, HttpClientProvider httpClientProvider) {
    this.client = client;
    this.httpClientProvider = httpClientProvider;
  }

  public HttpClient getHttpClient() {
    return httpClientProvider.getHttpClient();
  }

  public HttpClient getHttpClient(String connectionId) {
    try {
      var creds = client.getCredentials(new GetCredentialsParams(connectionId)).get(1, TimeUnit.MINUTES);
      return httpClientProvider.getHttpClientWithPreemptiveAuth(
        creds.getCredentials().map(TokenDto::getToken, UsernamePasswordDto::getUsername),
        creds.getCredentials().map(t -> null, UsernamePasswordDto::getPassword));
    } catch (Exception e) {
      logger.error("Unable to get credentials for connection {}", connectionId);
      return httpClientProvider.getHttpClient();
    }
  }

}