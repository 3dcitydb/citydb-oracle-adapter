/*
 * citydb-oracle-adapter
 * https://www.3dcitydb.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.citydb.database.oracle.cli;

import org.citydb.cli.Launcher;

/**
 * Executable entry point that drives the citydb-tool CLI in-process with this project's
 * Oracle adapter on the classpath.
 *
 * <p>This is a developer test harness, not part of the published adapter. It exists so the
 * adapter can be exercised end-to-end (import/export/connect/...) straight from this repo
 * &mdash; with breakpoints in the adapter code &mdash; without first assembling the adapter
 * and dropping it into a citydb-tool distribution.
 *
 * <p>Why it works: {@code org.citydb.cli.Launcher#execute(String[])} returns the exit code
 * instead of calling {@code System.exit(int)} (only {@code Launcher#main} exits the JVM), and
 * {@code DatabaseAdapterManager} discovers adapters via
 * {@code ServiceLoader.load(DatabaseAdapter.class, contextClassLoader)}. Because the test
 * runtime classpath contains this project's {@code META-INF/services/
 * org.citydb.database.adapter.DatabaseAdapter} entry (pointing at {@code OracleAdapter}) plus
 * the Oracle JDBC/Spatial jars, the adapter is registered automatically &mdash; no module path
 * needed.
 *
 * <p>Run it via Gradle:
 * <pre>
 *   ./gradlew runCli --args="--version"
 *   ./gradlew runCli --args="connect --config-file src/test/resources/oracle.json"
 *   ./gradlew runCli --args="import --config-file src/test/resources/oracle.json data.gml"
 *   ./gradlew runCli --args="export --config-file src/test/resources/oracle.json -o out.gml"
 * </pre>
 * or run this {@code main} directly from the IDE (arguments default to {@code --version} so a
 * plain Run verifies the CLI starts and the Oracle adapter is on the classpath).
 *
 * <p>Select the Oracle adapter through the {@code databaseName: "Oracle"} connection property
 * in the config file (see {@code src/test/resources/oracle.json}); there is no CLI flag for it.
 */
public class OracleCliLauncher {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // Smoke test: prints version info and, on the way, forces the adapter classpath
            // to be wired. Replace with a real subcommand when running from the IDE.
            args = new String[]{"--version"};
        }

        int exitCode = new Launcher().execute(args);
        System.out.println("[OracleCliLauncher] citydb CLI exited with code " + exitCode);

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private OracleCliLauncher() {
    }
}
