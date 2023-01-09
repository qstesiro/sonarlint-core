/*
 * SonarLint Core - ITs - Tests
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
package its;

import com.sonar.orchestrator.OrchestratorExtension;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import its.utils.OrchestratorUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.DoTransitionRequest;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.issues.SetSeverityRequest;
import org.sonarqube.ws.client.issues.SetTypeRequest;
import org.sonarqube.ws.client.users.CreateRequest;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;
import org.sonarsource.sonarlint.core.serverconnection.issues.ServerIssue;

import static its.utils.ItUtils.SONAR_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

class ConnectedCommunityIssueDownloadTests extends AbstractConnectedTests {
  private static final String PROJECT_KEY = "sample-xoo";

  @RegisterExtension
  static OrchestratorExtension ORCHESTRATOR = OrchestratorUtils.defaultEnvBuilder()
    .addPlugin(MavenLocation.of("org.sonarsource.sonarqube", "sonar-xoo-plugin", SONAR_VERSION))
    .restoreProfileAtStartup(FileLocation.ofClasspath("/xoo-sonarlint.xml"))
    .build();

  private ConnectedSonarLintEngine engine;
  private final List<String> logs = new ArrayList<>();

  private static Issue wfIssue;
  private static Issue fpIssue;
  private static Issue overridenSeverityIssue;
  private static Issue overridenTypeIssue;

  @BeforeAll
  static void prepare() {
    var adminWsClient = newAdminWsClient(ORCHESTRATOR);
    adminWsClient.users().create(new CreateRequest().setLogin(SONARLINT_USER).setPassword(SONARLINT_PWD).setName("SonarLint"));

    provisionProject(ORCHESTRATOR, PROJECT_KEY, "Sample Xoo");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "SonarLint IT Xoo");

    analyzeProject("sample-xoo-v1");
    // Second analysis with less issues to have closed issues
    analyzeProject("sample-xoo-v2");

    // Mark a few issues as closed WF and closed FP
    var issueSearchResponse = adminWsClient.issues()
      .search(new SearchRequest().setStatuses(List.of("OPEN")).setTypes(List.of("CODE_SMELL")).setComponentKeys(List.of(PROJECT_KEY)));
    wfIssue = issueSearchResponse.getIssues(0);
    fpIssue = issueSearchResponse.getIssues(1);
    // Change severity and type
    overridenSeverityIssue = issueSearchResponse.getIssues(2);
    overridenTypeIssue = issueSearchResponse.getIssues(3);

    adminWsClient.issues().doTransition(new DoTransitionRequest().setIssue(wfIssue.getKey()).setTransition("wontfix"));
    adminWsClient.issues().doTransition(new DoTransitionRequest().setIssue(fpIssue.getKey()).setTransition("falsepositive"));

    adminWsClient.issues().setSeverity(new SetSeverityRequest().setIssue(overridenSeverityIssue.getKey()).setSeverity("BLOCKER"));
    adminWsClient.issues().setType(new SetTypeRequest().setIssue(overridenTypeIssue.getKey()).setType("BUG"));

    // Ensure an hostpot has been reported on server side
    if (ORCHESTRATOR.getServer().version().isGreaterThanOrEquals(8, 2)) {
      assertThat(adminWsClient.hotspots().search(new org.sonarqube.ws.client.hotspots.SearchRequest().setProjectKey(PROJECT_KEY)).getHotspotsList()).isNotEmpty();
    } else {
      assertThat(
        adminWsClient.issues().search(new SearchRequest().setTypes(List.of("SECURITY_HOTSPOT")).setComponentKeys(List.of(PROJECT_KEY))).getIssuesList())
          .isNotEmpty();
    }
  }

  @BeforeEach
  public void start(@TempDir Path sonarUserHome) throws IOException {
    engine = new ConnectedSonarLintEngineImpl(ConnectedGlobalConfiguration.sonarQubeBuilder()
      .setConnectionId("orchestrator")
      .setSonarLintUserHome(sonarUserHome)
      .setLogOutput((msg, level) -> logs.add(msg))
      .setExtraProperties(new HashMap<>())
      .build());
  }

  @AfterEach
  public void stop() {
    engine.stop(true);
  }

  @Test
  void download_all_issues_not_limited_to_10k() {
    engine.updateProject(endpointParams(ORCHESTRATOR), sqHttpClient(), PROJECT_KEY, null);

    engine.downloadAllServerIssues(endpointParams(ORCHESTRATOR), sqHttpClient(), PROJECT_KEY, MAIN_BRANCH_NAME, null);

    var file1Issues = engine.getServerIssues(new ProjectBinding(PROJECT_KEY, "", ""), MAIN_BRANCH_NAME, "src/500lines.xoo");
    var file2Issues = engine.getServerIssues(new ProjectBinding(PROJECT_KEY, "", ""), MAIN_BRANCH_NAME, "src/10000lines.xoo");

    // Number of issues is not limited to 10k
    assertThat(file1Issues.size() + file2Issues.size()).isEqualTo(10_500);

    Map<String, ServerIssue> allIssues = new HashMap<>();
    engine.getServerIssues(new ProjectBinding(PROJECT_KEY, "", ""), MAIN_BRANCH_NAME, "src/500lines.xoo").forEach(i -> allIssues.put(i.getKey(), i));
    engine.getServerIssues(new ProjectBinding(PROJECT_KEY, "", ""), MAIN_BRANCH_NAME, "src/10000lines.xoo").forEach(i -> allIssues.put(i.getKey(), i));

    assertThat(allIssues).hasSize(10_500);
    assertThat(allIssues.get(wfIssue.getKey()).isResolved()).isTrue();
    assertThat(allIssues.get(fpIssue.getKey()).isResolved()).isTrue();
    assertThat(allIssues.get(overridenSeverityIssue.getKey()).getUserSeverity()).isEqualTo(IssueSeverity.BLOCKER);
    assertThat(allIssues.get(overridenTypeIssue.getKey()).getType()).isEqualTo(RuleType.BUG);

    // No hotspots
    assertThat(allIssues.values()).allSatisfy(i -> assertThat(i.getType()).isIn(RuleType.CODE_SMELL, RuleType.BUG, RuleType.VULNERABILITY));
  }

  private static void analyzeProject(String projectDirName) {
    var projectDir = Paths.get("projects/" + projectDirName).toAbsolutePath();
    ORCHESTRATOR.executeBuild(SonarScanner.create(projectDir.toFile())
      .setProjectKey(PROJECT_KEY)
      .setSourceDirs("src")
      .setProperty("sonar.login", com.sonar.orchestrator.container.Server.ADMIN_LOGIN)
      .setProperty("sonar.password", com.sonar.orchestrator.container.Server.ADMIN_PASSWORD));
  }
}