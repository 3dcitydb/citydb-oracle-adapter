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

import com.alibaba.fastjson2.JSONObject;
import oracle.jdbc.OracleTypes;
import org.citydb.database.adapter.DatabaseAdapter;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeleteHelper extends org.citydb.database.util.DeleteHelper {
    private final SqlHelper helper;
    private final String citydbDelete;
    private final String citydbTerminate;
    private CallableStatement deleteFeatureStmt;
    private CallableStatement terminateFeatureStmt;

    DeleteHelper(Connection connection, DatabaseAdapter adapter) {
        super(connection);
        helper = (SqlHelper) adapter.getSchemaAdapter().getSqlHelper();
        String schema = adapter.getConnectionDetails().getSchema();
        citydbDelete = SchemaAdapter.qualify(schema, "citydb_delete");
        citydbTerminate = SchemaAdapter.qualify(schema, "citydb_terminate");
    }

    @Override
    public boolean deleteFeature(long id) throws SQLException {
        return !deleteFeatures(List.of(id)).isEmpty();
    }

    @Override
    public Set<Long> deleteFeatures(Collection<Long> ids) throws SQLException {
        if (deleteFeatureStmt == null) {
            deleteFeatureStmt = prepareCall(citydbDelete + ".delete_feature(?)");
        }

        helper.setLongArrayOrNull(deleteFeatureStmt, 2, ids);
        return getIds(deleteFeatureStmt);
    }

    @Override
    public boolean terminateFeature(long id, JSONObject metadata) throws SQLException {
        return !terminateFeatures(List.of(id), metadata).isEmpty();
    }

    @Override
    public Set<Long> terminateFeatures(Collection<Long> ids, JSONObject metadata) throws SQLException {
        if (terminateFeatureStmt == null) {
            terminateFeatureStmt = prepareCall(citydbTerminate + ".terminate_feature(?, json(?))");
        }

        helper.setLongArrayOrNull(terminateFeatureStmt, 2, ids);
        if (metadata != null) {
            terminateFeatureStmt.setString(3, metadata.toString());
        } else {
            terminateFeatureStmt.setNull(3, Types.VARCHAR);
        }

        return getIds(terminateFeatureStmt);
    }

    // the functions run DML, so they cannot be used in a select (ORA-14551); read the result as OUT parameter
    private CallableStatement prepareCall(String function) throws SQLException {
        CallableStatement stmt = connection.prepareCall("{? = call " + function + "}");
        stmt.registerOutParameter(1, OracleTypes.ARRAY, helper.getNumberTab());
        return stmt;
    }

    private Set<Long> getIds(CallableStatement stmt) throws SQLException {
        stmt.execute();
        Set<Long> ids = new HashSet<>();
        Array array = stmt.getArray(1);
        if (array != null) {
            try {
                for (Object value : (Object[]) array.getArray()) {
                    if (value != null) {
                        ids.add(((Number) value).longValue());
                    }
                }
            } finally {
                array.free();
            }
        }

        return ids;
    }

    @Override
    public void close() throws SQLException {
        try (CallableStatement deleteFeatureStmt = this.deleteFeatureStmt;
             CallableStatement terminateFeatureStmt = this.terminateFeatureStmt) {
        }
    }
}
