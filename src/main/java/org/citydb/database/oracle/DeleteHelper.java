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

import oracle.jdbc.OracleTypes;
import org.citydb.database.adapter.DatabaseAdapter;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DeleteHelper extends org.citydb.database.util.DeleteHelper {
    private final SqlHelper helper;
    private final String citydbDelete;
    private final String citydbTerminate;
    private CallableStatement deleteFeatureStmt;
    private CallableStatement deleteFeaturesStmt;
    private CallableStatement terminateFeatureStmt;
    private CallableStatement terminateFeaturesStmt;

    DeleteHelper(Connection connection, DatabaseAdapter adapter) {
        super(connection);
        helper = (SqlHelper) adapter.getSchemaAdapter().getSqlHelper();
        String schema = adapter.getConnectionDetails().getSchema();
        citydbDelete = SchemaAdapter.qualify(schema, "citydb_delete");
        citydbTerminate = SchemaAdapter.qualify(schema, "citydb_terminate");
    }

    @Override
    public boolean deleteFeature(long id) throws SQLException {
        if (deleteFeatureStmt == null) {
            deleteFeatureStmt = prepareDeleteStatement(Types.NUMERIC);
        }

        deleteFeatureStmt.setLong(2, id);
        return getId(deleteFeatureStmt) != null;
    }

    @Override
    public Set<Long> deleteFeatures(Collection<Long> ids) throws SQLException {
        if (deleteFeaturesStmt == null) {
            deleteFeaturesStmt = prepareDeleteStatement(OracleTypes.ARRAY);
        }

        helper.setLongArrayOrNull(deleteFeaturesStmt, 2, ids);
        return getIds(deleteFeaturesStmt);
    }

    @Override
    public boolean terminateFeature(long id, String metadata, boolean cascade) throws SQLException {
        if (terminateFeatureStmt == null) {
            terminateFeatureStmt = prepareTerminateStatement(Types.NUMERIC);
        }

        terminateFeatureStmt.setLong(2, id);
        setMetadata(terminateFeatureStmt, metadata);
        terminateFeatureStmt.setInt(4, cascade ? 1 : 0);
        return getId(terminateFeatureStmt) != null;
    }

    @Override
    public Set<Long> terminateFeatures(Collection<Long> ids, String metadata, boolean cascade) throws SQLException {
        if (terminateFeaturesStmt == null) {
            terminateFeaturesStmt = prepareTerminateStatement(OracleTypes.ARRAY);
        }

        helper.setLongArrayOrNull(terminateFeaturesStmt, 2, ids);
        setMetadata(terminateFeaturesStmt, metadata);
        terminateFeaturesStmt.setInt(4, cascade ? 1 : 0);
        return getIds(terminateFeaturesStmt);
    }

    // the functions run DML, so they cannot be used in a select (ORA-14551); read the result as OUT parameter
    private CallableStatement prepareDeleteStatement(int resultType) throws SQLException {
        CallableStatement stmt = connection.prepareCall("{? = call " + citydbDelete + ".delete_feature(?)}");
        registerResult(stmt, resultType);
        return stmt;
    }

    private CallableStatement prepareTerminateStatement(int resultType) throws SQLException {
        CallableStatement stmt = connection.prepareCall("{? = call " + citydbTerminate +
                ".terminate_feature(?, json(?), ?)}");
        registerResult(stmt, resultType);
        return stmt;
    }

    private void registerResult(CallableStatement stmt, int resultType) throws SQLException {
        if (resultType == OracleTypes.ARRAY) {
            stmt.registerOutParameter(1, OracleTypes.ARRAY, helper.getNumberTab());
        } else {
            stmt.registerOutParameter(1, resultType);
        }
    }

    private void setMetadata(PreparedStatement stmt, String metadata) throws SQLException {
        if (metadata != null) {
            stmt.setString(3, metadata);
        } else {
            stmt.setNull(3, Types.VARCHAR);
        }
    }

    private Long getId(CallableStatement stmt) throws SQLException {
        stmt.execute();
        long id = stmt.getLong(1);
        return !stmt.wasNull() ? id : null;
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
             CallableStatement deleteFeaturesStmt = this.deleteFeaturesStmt;
             CallableStatement terminateFeatureStmt = this.terminateFeatureStmt;
             CallableStatement terminateFeaturesStmt = this.terminateFeaturesStmt) {
        }
    }
}
