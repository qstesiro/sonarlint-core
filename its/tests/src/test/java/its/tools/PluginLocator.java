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
package its.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PluginLocator {
  private static final String DBD_VERSION = "1.7-SNAPSHOT";
  public static final String SONAR_DBD_ENGINE_PLUGIN_JAR = "sonar-dbd-plugin-" + DBD_VERSION + ".jar";
  public static final String SONAR_DBD_JAVA_FRONTEND_PLUGIN_JAR = "sonar-dbd-java-frontend-plugin" + DBD_VERSION + ".jar";
  public static final String SONAR_DBD_PYTHON_FRONTEND_PLUGIN_JAR = "sonar-dbd-python-frontend-plugin-" + DBD_VERSION + ".jar";
  public static final String SONAR_PYTHON_PLUGIN_JAR = "sonar-python-plugin-3.17.0.10029.jar";

  public static Path getCppPluginPath() {
    return getPluginPath("sonar-cfamily-plugin-6.32.0.44918.jar");
  }

  public static Path getDbdEnginePluginPath() {
    return getPluginPath(SONAR_DBD_ENGINE_PLUGIN_JAR);
  }

  public static Path getDbdJavaFrontendEnginePluginPath() {
    return getPluginPath(SONAR_DBD_JAVA_FRONTEND_PLUGIN_JAR);
  }

  public static Path getDbdPythonFrontendPluginPath() {
    return getPluginPath(SONAR_DBD_PYTHON_FRONTEND_PLUGIN_JAR);
  }

  public static Path getPythonPluginPath() {
    return getPluginPath(SONAR_PYTHON_PLUGIN_JAR);
  }

  private static Path getPluginPath(String file) {
    var path = Paths.get("target/plugins/").resolve(file);
    if (!Files.isRegularFile(path)) {
      throw new IllegalStateException("Unable to find file " + path);
    }
    return path;
  }

}
