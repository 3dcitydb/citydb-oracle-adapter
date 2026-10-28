/*
 * citydb-tool - Command-line tool for the 3D City Database
 * https://www.3dcitydb.org/
 *
 * Copyright © 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citydb.database.oracle;

import org.citydb.database.adapter.DatabaseAdapter;
import org.citydb.database.schema.Index;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public class IndexHelper extends org.citydb.database.util.IndexHelper {

    protected IndexHelper(DatabaseAdapter adapter) {
        super(adapter);
    }

    @Override
    protected void createIndex(Index index, boolean ignoreNulls, Connection connection) throws SQLException {
        String sql = "create index if not exists " + index.getName() +
                " on " + adapter.getConnectionDetails().getSchema() + "." + index.getTable() +
                "(" + String.join(", ", index.getColumns()) + ")" +
                (index.getType() == Index.Type.SPATIAL ? " indextype is mdsys.spatial_index_v2" : "");

        // ignoreNulls is intentionally not applied: Oracle does not index null values by default
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    protected void dropIndex(Index index, Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("drop index if exists " + adapter.getConnectionDetails().getSchema() + "." + index.getName());
        }
    }

    @Override
    protected boolean indexExists(Index index, Connection connection) throws SQLException {
        String sql = "select 1 from all_indexes " +
                "where owner = '" + adapter.getConnectionDetails().getSchema().toUpperCase(Locale.ROOT) + "' " +
                "and index_name = '" + index.getName().toUpperCase(Locale.ROOT) + "' and rownum = 1";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }
}
