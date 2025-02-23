/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.jfr.api.consumer.log;

import java.time.Duration;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary Tests that log responds to log level changes after JVM has
 *          started
 * @requires vm.flagless
 * @requires vm.hasJFR
 * @library /test/lib
 * @build jdk.jfr.api.consumer.log.LogAnalyzer
 * @run main/othervm jdk.jfr.api.consumer.log.TestDynamicStart
 */
public class TestDynamicStart {
    private static final String FILE = "log.txt";

    static class UserEvent extends Event {
        String message;
    }

    public static void main(String... args) throws Exception {
        FlightRecorder.addPeriodicEvent(UserEvent.class, () -> {
            UserEvent event = new UserEvent();
            event.message = "Giraffe";
            event.commit();
        });
        LogAnalyzer la = new LogAnalyzer(FILE);
        try (Recording r = new Recording()) {
            r.enable(UserEvent.class).withPeriod(Duration.ofSeconds(1));
            r.start();
            executeJcmd("VM.log what=jfr+event=debug,jfr+system=debug output=" + FILE);
            la.await("Log stream started");
            la.await("Giraffe");
            executeJcmd("VM.log what=jfr+event=warning output=" + FILE);
            la.await("Log stream stopped");
        }
    }

    private static void executeJcmd(String cmd) {
        CommandExecutor executor = new PidJcmdExecutor();
        OutputAnalyzer oa = executor.execute(cmd);
        oa.shouldHaveExitValue(0);
    }
}
