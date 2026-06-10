# citydb-oracle-adapter

Oracle database adapter for the [3D City Database](https://www.3dcitydb.org/) command-line
tool ([citydb-tool](https://github.com/3dcitydb/citydb-tool)).

This is a standalone, pluggable database adapter. It is built against the **published**
`org.citydb:citydb-database` artifact and ships as a Java module that the citydb-tool CLI
discovers at runtime via the `java.util.ServiceLoader` service-binding mechanism.

## Compatibility

The adapter is compiled against a specific citydb-tool release. The target version is set in
[`build.gradle`](build.gradle):

```gradle
ext {
    citydbVersion = '1.3.1'
}
```

## Build

```bash
./gradlew build
```

To produce a ready-to-drop-in folder containing the adapter jar together with its
Oracle-specific dependencies:

```bash
./gradlew assembleAdapter
# -> build/adapter/
#      citydb-oracle-adapter-<version>.jar
#      ojdbc17-*.jar
#      sdoapi-*-module.jar
#      sdoutl-*-module.jar
```

## Install into citydb-tool

Copy the contents of `build/adapter/` into the citydb-tool distribution's module path,
i.e. its `lib/` directory:

```bash
cp build/adapter/* /path/to/citydb-tool/lib/
```

Because every jar in `lib/` is on the JVM module path at launch, the JVM performs service
binding: `org.citydb.database` declares `uses org.citydb.database.adapter.DatabaseAdapter`,
so the `OracleAdapter` provided by this module is discovered automatically.

## Select Oracle for import/export

The adapter type is chosen via the `databaseName` connection property. There is currently no
CLI flag for it, so set it in a config file passed with `--config-file`:

```json
{
  "databaseOptions": {
    "connections": {
      "oracle": {
        "databaseName": "Oracle",
        "host": "localhost",
        "port": 1521,
        "database": "FREEPDB1",
        "schema": "CITYDB",
        "user": "citydb",
        "password": "..."
      }
    }
  }
}
```

Then run import/export as usual:

`import` and `export` each require a format subcommand (`citygml` or `cityjson`):

```bash
citydb import citygml --config-file oracle.json data.gml
citydb export citygml --config-file oracle.json -o out.gml
```

## Debugging with a local citydb-tool (Gradle composite build)

Wire the adapter into a local checkout of citydb-tool (matching release branch, e.g.
`release-1.3`) so the CLI runs with the adapter source on the module path:

```gradle
// citydb-tool/settings.gradle
includeBuild '../citydb-oracle-adapter'
```
```gradle
// citydb-tool/citydb-cli/build.gradle  (runtimeOnly is enough; discovered via ServiceLoader)
runtimeOnly 'org.citydb:citydb-oracle-adapter:1.0.0-Snapshot'
```

Then run the CLI via the `:citydb-cli:run` task:

```bash
./gradlew :citydb-cli:run --args="import --config-file oracle.json data.gml"
```

> Revert these `settings.gradle` / `build.gradle` edits when done.

## Testing the CLI from this repo

For an in-process, IDE-debuggable test loop you can drive the citydb-tool CLI directly from
this project's test sources &mdash; no distribution to assemble, no composite build. The
CLI is pulled from Maven (`org.citydb:citydb-cli`, same version as `citydbVersion`), which
transitively provides picocli, log4j and the citydb runtime.

The entry point is [`OracleCliLauncher`](src/test/java/org/citydb/database/oracle/cli/OracleCliLauncher.java),
wrapped by the `runCli` Gradle task. Point [`src/test/resources/oracle.json`](src/test/resources/oracle.json)
at your Oracle instance (it sets `databaseName: "Oracle"` to select this adapter), then:

```bash
./gradlew runCli --args="--version"
./gradlew runCli --args="connect --config-file src/test/resources/oracle.json"
./gradlew runCli --args="import citygml --config-file src/test/resources/oracle.json data.gml"
./gradlew runCli --args="export citygml --config-file src/test/resources/oracle.json -o out.gml"
```

## Status

The adapter is a work in progress. Several methods were mechanically ported across a
citydb-database API refactor and are marked with `// TODO semantic-review` for verification of
the Oracle SQL semantics (schema existence, index DDL, changelog detection, SRS lookup).
