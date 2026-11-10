# citydb-oracle-adapter

Oracle database adapter for the [3D City Database](https://www.3dcitydb.org/) command-line
tool ([citydb-tool](https://github.com/3dcitydb/citydb-tool)).

## Compatibility

The adapter is compiled against a specific citydb-tool version, which is set in
[`build.gradle`](build.gradle):

```gradle
ext {
    citydbVersion = '1.5.0-SNAPSHOT'
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
# -> build/database-oracle-adapter/
#      citydb-oracle-adapter-<version>.jar
#      ojdbc*.jar
#      sdoapi*.jar
#      sdoutl*.jar
```

## Install into citydb-tool

Copy the folder `build/database-oracle-adapter` including all its contents into the `extensions/`
directory of the citydb-tool distribution:

```bash
cp -r build/database-oracle-adapter /path/to/citydb-tool/extensions/
```

Alternatively, keep the adapter folder anywhere and point the CLI at it with
`citydb --extensions <dir> ...`, where `<dir>` is the adapter folder or any directory above it.
Note that this option replaces the default `extensions/` directory rather than adding to it.

## Select the Oracle adapter

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

Then run the CLI commands such as `import` and `export` as usual:

```bash
citydb import citygml --config-file oracle.json data.gml
citydb export citygml --config-file oracle.json -o out.gml
```

## Debugging with a local citydb-tool (Gradle composite build)

Wire the adapter into a local checkout of citydb-tool so the CLI runs with the adapter source
on the module path:

```gradle
// citydb-tool/settings.gradle
includeBuild '../citydb-oracle-adapter'
```
```gradle
// citydb-tool/citydb-cli/build.gradle
runtimeOnly 'org.citydb:citydb-oracle-adapter:1.0.0-Snapshot'
```

Then run the CLI via the `:citydb-cli:run` task:

```bash
./gradlew :citydb-cli:run --args="import citygml --config-file oracle.json data.gml"
```

> Revert these `settings.gradle` / `build.gradle` edits when done.

## Testing the CLI from this repo

The citydb-tool CLI can also be run directly from this project's test sources, in-process and
debuggable from the IDE, without assembling a distribution or setting up a composite build.

The entry point is [`OracleCliLauncher`](src/test/java/org/citydb/database/oracle/cli/OracleCliLauncher.java),
wrapped by the `runCli` Gradle task. The config file
[`src/test/resources/oracle.json`](src/test/resources/oracle.json) is only an example: keep
`databaseName: "Oracle"`, which selects this adapter, and replace the connection parameters
with those of your local Oracle database. Then:

```bash
./gradlew runCli --args="--version"
./gradlew runCli --args="connect --config-file src/test/resources/oracle.json"
./gradlew runCli --args="import citygml --config-file src/test/resources/oracle.json data.gml"
./gradlew runCli --args="export citygml --config-file src/test/resources/oracle.json -o out.gml"
```
