/*
 * SonarLint Core - Plugin Commons
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
package org.sonar.api.utils.log;

import org.sonarsource.sonarlint.core.commons.log.SonarConsoleLogger;

/**
 * This class can't be moved to another package because {@link BaseLogger} is not public.
 * And we have to extend BaseLogger to please {@link DefaultProfiler}.
 */
public class ConsoleApiLogger extends BaseLogger {

    private final static SonarConsoleLogger logger = new SonarConsoleLogger();

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public void doTrace(String msg) {
    }

    @Override
    public void doTrace(String pattern, Object arg) {
    }

    @Override
    public void doTrace(String msg, Object arg1, Object arg2) {
    }

    @Override
    public void doTrace(String msg, Object... args) {
    }

    @Override
    public boolean isDebugEnabled() {
        // Always produce debug logs, the filtering will be handled on client side
        return true;
    }

    @Override
    public void doDebug(String msg) {
        if (getLevel().ordinal() <= LoggerLevel.DEBUG.ordinal()) {
            logger.debug(msg);
        }
    }

    @Override
    public void doDebug(String pattern, Object arg) {
        if (getLevel().ordinal() <= LoggerLevel.DEBUG.ordinal()) {
            logger.debug(pattern, arg);
        }
    }

    @Override
    public void doDebug(String msg, Object arg1, Object arg2) {
        if (getLevel().ordinal() <= LoggerLevel.DEBUG.ordinal()) {
            logger.debug(msg, arg1, arg2);
        }
    }

    @Override
    public void doDebug(String msg, Object... args) {
        if (getLevel().ordinal() <= LoggerLevel.DEBUG.ordinal()) {
            logger.debug(msg, args);
        }
    }

    @Override
    public void doInfo(String msg) {
        if (getLevel().ordinal() <= LoggerLevel.INFO.ordinal()) {
            logger.info(msg);
        }
    }

    @Override
    public void doInfo(String msg, Object arg) {
        if (getLevel().ordinal() <= LoggerLevel.INFO.ordinal()) {
            logger.info(msg, arg);
        }
    }

    @Override
    public void doInfo(String msg, Object arg1, Object arg2) {
        if (getLevel().ordinal() <= LoggerLevel.INFO.ordinal()) {
            logger.info(msg, arg1, arg2);
        }
    }

    @Override
    public void doInfo(String msg, Object... args) {
        if (getLevel().ordinal() <= LoggerLevel.INFO.ordinal()) {
            logger.info(msg, args);
        }
    }

    @Override
    public void doWarn(String msg) {
        if (getLevel().ordinal() <= LoggerLevel.INFO.ordinal()) {
            logger.warn(msg);
        }
    }

    @Override
    public void doWarn(String msg, Throwable throwable) { // 未使用throwable
        if (getLevel().ordinal() <= LoggerLevel.WARN.ordinal()) {
            logger.warn(msg);
        }
    }

    @Override
    public void doWarn(String msg, Object arg) {
        if (getLevel().ordinal() <= LoggerLevel.WARN.ordinal()) {
            logger.warn(msg, arg);
        }
    }

    @Override
    public void doWarn(String msg, Object arg1, Object arg2) {
        if (getLevel().ordinal() <= LoggerLevel.WARN.ordinal()) {
            logger.warn(msg, arg1, arg2);
        }
    }

    @Override
    public void doWarn(String msg, Object... args) {
        if (getLevel().ordinal() <= LoggerLevel.WARN.ordinal()) {
            logger.warn(msg, args);
        }
    }

    @Override
    public void doError(String msg) {
        if (getLevel().ordinal() <= LoggerLevel.ERROR.ordinal()) {
            logger.error(msg);
        }
    }

    @Override
    public void doError(String msg, Object arg) {
        if (getLevel().ordinal() <= LoggerLevel.ERROR.ordinal()) {
            logger.error(msg, arg);
        }
    }

    @Override
    public void doError(String msg, Object arg1, Object arg2) {
        if (getLevel().ordinal() <= LoggerLevel.ERROR.ordinal()) {
            logger.error(msg, arg1, arg2);
        }
    }

    @Override
    public void doError(String msg, Object... args) {
        if (getLevel().ordinal() <= LoggerLevel.ERROR.ordinal()) {
            logger.error(msg, args);
        }
    }

    @Override
    public void doError(String msg, Throwable thrown) { // 未使用throwable
        if (getLevel().ordinal() <= LoggerLevel.ERROR.ordinal()) {
            logger.error(msg);
        }
    }

    @Override
    public boolean setLevel(LoggerLevel level) {
        return false;
    }

    @Override
    public LoggerLevel getLevel() {
        return LoggerLevel.DEBUG;
        // return LoggerLevel.INFO;
    }

}
