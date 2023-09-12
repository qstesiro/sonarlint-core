/*
 * SonarLint Core - Commons
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
package org.sonarsource.sonarlint.core.commons.log;

import java.io.PrintStream;
import javax.annotation.Nullable;

/**
 * Slow implementation based on {@link java.lang.System#out}. It is not production-ready and it must be used
 * only for the tests that do not have logback dependency.
 * <br>Implementation of message patterns is naive. It does not support escaped '{' and '}'
 * arguments.
 */
public class SonarConsoleLogger {

    private final PrintStream stream;

    public SonarConsoleLogger() {
        this.stream = System.out;
    }

    public SonarConsoleLogger(PrintStream stream) {
        this.stream = stream;
    }

    public void trace(String msg) {
        stream.println(msg);
    }

    public void trace(String msg, @Nullable Object arg) {
        trace(MessageFormatter.arrayFormat(msg, new Object[]{arg}).getMessage());
    }

    public void trace(String msg, @Nullable Object arg1, @Nullable Object arg2) {
        trace(MessageFormatter.arrayFormat(msg, new Object[]{arg1, arg2}).getMessage());
    }

    public void trace(String msg, Object... args) {
        trace(MessageFormatter.arrayFormat(msg, args).getMessage());
    }

    public void debug(String msg) {
        stream.println(msg);
    }

    public void debug(String msg, @Nullable Object arg) {
        debug(MessageFormatter.arrayFormat(msg, new Object[]{arg}).getMessage());
    }

    public void debug(String msg, @Nullable Object arg1, @Nullable Object arg2) {
        debug(MessageFormatter.arrayFormat(msg, new Object[]{arg1, arg2}).getMessage());
    }

    public void debug(String msg, Object... args) {
        debug(MessageFormatter.arrayFormat(msg, args).getMessage());
    }

    public void info(String msg) {
        stream.println(msg);
    }

    public void info(String msg, @Nullable Object arg) {
        info(MessageFormatter.arrayFormat(msg, new Object[]{arg}).getMessage());
    }

    public void info(String msg, @Nullable Object arg1, @Nullable Object arg2) {
        info(MessageFormatter.arrayFormat(msg, new Object[]{arg1, arg2}).getMessage());
    }

    public void info(String msg, Object... args) {
        info(MessageFormatter.arrayFormat(msg, args).getMessage());
    }

    public void warn(String msg) {
        stream.println(msg);
    }

    void warn(String msg, Throwable thrown) {
        warn(msg);
    }

    public void warn(String msg, @Nullable Object arg) {
        warn(MessageFormatter.arrayFormat(msg, new Object[]{arg}).getMessage());
    }

    public void warn(String msg, @Nullable Object arg1, @Nullable Object arg2) {
        warn(MessageFormatter.arrayFormat(msg, new Object[]{arg1, arg2}).getMessage());
    }

    public void warn(String msg, Object... args) {
        warn(MessageFormatter.arrayFormat(msg, args).getMessage());
    }

    public void error(String msg) {
        stream.println(msg);
    }

    public void error(String msg, @Nullable Object arg) {
        error(MessageFormatter.arrayFormat(msg, new Object[]{arg}).getMessage());
    }

    public void error(String msg, @Nullable Object arg1, @Nullable Object arg2) {
        error(MessageFormatter.arrayFormat(msg, new Object[]{arg1, arg2}).getMessage());
    }

    public void error(String msg, Object... args) {
        error(MessageFormatter.arrayFormat(msg, args).getMessage());
    }

    public void error(String msg, Throwable thrown) {
        error(msg);
    }
}
