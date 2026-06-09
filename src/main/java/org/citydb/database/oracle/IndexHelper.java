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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class IndexHelper extends org.citydb.database.util.IndexHelper {
    private final Logger logger = LoggerFactory.getLogger(IndexHelper.class);

    protected IndexHelper(DatabaseAdapter adapter) {
        super(adapter);
    }

    @Override
    protected void createIndex(Index index, boolean ignoreNulls, Connection connection) throws SQLException {
        // TODO semantic-review: ported from former SchemaAdapter.getCreateIndex(). Oracle has no
        // "create index if not exists"; the base IndexHelper already guards with exists() before
        // calling this, so the clause is harmless but should be revisited.
        // ignoreNulls is intentionally not applied: Oracle does not index null values by default.
        String sql = "create index if not exists " + SchemaAdapter.enquoteSqlName(index.getName()) +
                " on " + SchemaAdapter.enquoteSqlName(adapter.getConnectionDetails().getSchema()) + "." +
                SchemaAdapter.enquoteSqlName(index.getTable()) +
                "(" + String.join(", ", SchemaAdapter.enquoteSqlNames(index.getColumns())) + ")" +
                (index.getType() == Index.Type.SPATIAL ? " INDEXTYPE IS MDSYS.SPATIAL_INDEX_V2 " : " ");
        logger.debug("createIndex: " + sql);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    protected void dropIndex(Index index, Connection connection) throws SQLException {
        // TODO semantic-review: ported from former SchemaAdapter.getDropIndex(); "drop index if exists"
        // is not supported on older Oracle releases. The base IndexHelper guards with exists() first.
        String sql = "drop index if exists " +
                SchemaAdapter.enquoteSqlName(adapter.getConnectionDetails().getSchema()) + "." +
                SchemaAdapter.enquoteSqlName(index.getName());
        logger.debug("dropIndex: " + sql);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    protected boolean indexExists(Index index, Connection connection) throws SQLException {
        // TODO semantic-review: ported from former SchemaAdapter.getIndexExists(). The original wrapped
        // owner/index_name with enquoteSqlName() inside single-quoted literals, which yields double-quoted
        // values; this likely needs the plain (upper-cased) identifier instead.
        String sql = "select 1 " +
                " from all_indexes " +
                " where owner = '" + SchemaAdapter.enquoteSqlName(adapter.getConnectionDetails().getSchema()) + "' " +
                " and index_name = '" + SchemaAdapter.enquoteSqlName(index.getName()) +
                "' and rownum = 1";
        logger.debug("indexExists: " + sql);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }
}
