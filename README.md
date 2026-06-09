# citydb-oracle-adapter

Oracle database adapter for the [3D City Database](https://www.3dcitydb.org/) command-line
tool ([citydb-tool](https://github.com/3dcitydb/citydb-tool)).

This is a standalone, pluggable database adapter. It is built against the **published**
`org.citydb:citydb-database` artifact and ships as a Java module that the citydb-tool CLI
discovers at runtime via the `java.util.ServiceLoader` service-binding mechanism — no changes
to citydb-tool itself are required.

## Compatibility

The adapter is compiled against a specific citydb-tool release. The target version is set in
[`build.gradle`](build.gradle):

```gradle
ext {
    citydbVersion = '1.3.1'
}
```

The adapter **must** be run against a citydb-tool distribution of the same version.

## Build

```bash
./gradlew build
```

To produce a ready-to-drop-in folder containing the adapter jar together with its
Oracle-specific dependencies (Oracle JDBC + Spatial):

```bash
./gradlew assemblePlugin
# -> build/plugin/
#      citydb-oracle-adapter-<version>.jar
#      ojdbc17-*.jar
#      sdoapi-*-module.jar
#      sdoutl-*-module.jar
```

## Install into citydb-tool

Copy the contents of `build/plugin/` into the citydb-tool distribution's module path,
i.e. its `lib/` directory:

```bash
cp build/plugin/* /path/to/citydb-tool/lib/
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

```bash
citydb import --config-file oracle.json data.gml
citydb export --config-file oracle.json -o out.gml
```

## Debugging with a local citydb-tool (Gradle composite build)

For a fast edit-debug loop on the adapter, wire it into a **local checkout of citydb-tool**
(on the matching release branch, e.g. `release-1.3`) via a Gradle
[composite build](https://docs.gradle.org/current/userguide/composite_builds.html). This runs
the real CLI with the adapter source on the module path, so you can set breakpoints in both
the adapter and citydb-tool.

1. In the **citydb-tool** checkout, add the adapter as an included build and a runtime
   dependency of the CLI. `runtimeOnly` is enough — the adapter is discovered through
   `ServiceLoader` service binding, so no `requires` directive is needed in the CLI's
   `module-info.java`:

   ```gradle
   // citydb-tool/settings.gradle
   includeBuild '../citydb-oracle-adapter'
   ```

   ```gradle
   // citydb-tool/citydb-cli/build.gradle
   dependencies {
       runtimeOnly 'org.citydb:citydb-oracle-adapter:1.3.1'
   }
   ```

   Gradle substitutes the published coordinates with this project automatically (matched by
   `group:name`), so the version only has to exist, not match exactly.

2. Run the CLI with the JVM paused for a debugger. `--debug-jvm` makes Gradle listen on port
   5005 and suspend until a debugger attaches:

   ```bash
   ./gradlew :citydb-cli:run \
       --args="import --config-file /path/to/oracle.json /path/to/data.gml" \
       --debug-jvm
   ```

3. In IntelliJ, create a **Remote JVM Debug** run configuration on `localhost:5005` and attach.
   Open both this project and the citydb-tool checkout so the debugger can resolve sources on
   either side, then step through.

> This temporarily modifies the citydb-tool working tree. Revert the `settings.gradle` /
> `build.gradle` edits (e.g. `git checkout`) when you are done.

Selecting the Oracle adapter still requires a config file with `"databaseName": "Oracle"` (see
[Select Oracle for import/export](#select-oracle-for-importexport) above), since there is no
CLI flag for the adapter type.

## Status

The adapter is a work in progress. Several methods were mechanically ported across a
citydb-database API refactor and are marked with `// TODO semantic-review` for verification of
the Oracle SQL semantics (schema existence, index DDL, changelog detection, SRS lookup).
