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
import org.citydb.database.util.SqlHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

public class DeleteHelper extends org.citydb.database.util.DeleteHelper {
    private final SqlHelper helper;
    private final String citydbDelete;
    private final String citydbTerminate;
    private final String numberTab;
    private PreparedStatement deleteFeatureStmt;
    private PreparedStatement deleteFeaturesStmt;
    private PreparedStatement terminateFeatureStmt;
    private PreparedStatement terminateFeaturesStmt;

    DeleteHelper(Connection connection, DatabaseAdapter adapter) {
        super(connection);
        helper = adapter.getSchemaAdapter().getSqlHelper();
        String schema = adapter.getConnectionDetails().getSchema();
        citydbDelete = SchemaAdapter.qualify(schema, "citydb_delete");
        citydbTerminate = SchemaAdapter.qualify(schema, "citydb_terminate");
        numberTab = SchemaAdapter.qualify(schema, "number_tab");
    }

    private PreparedStatement prepareDeleteStatement(String resultType) throws SQLException {
        return connection.prepareStatement("declare result " + resultType + "; " +
                "begin result := " + citydbDelete + ".delete_feature(?); end;");
    }

    private PreparedStatement prepareTerminateStatement(String resultType) throws SQLException {
        return connection.prepareStatement("declare result " + resultType + "; " +
                "begin result := " + citydbTerminate + ".terminate_feature(?, json(?), ?); end;");
    }

    @Override
    public void deleteFeature(long id) throws SQLException {
        if (deleteFeatureStmt == null) {
            deleteFeatureStmt = prepareDeleteStatement("number");
        }

        deleteFeatureStmt.setLong(1, id);
        deleteFeatureStmt.execute();
    }

    @Override
    public void deleteFeatures(Collection<Long> ids) throws SQLException {
        if (deleteFeaturesStmt == null) {
            deleteFeaturesStmt = prepareDeleteStatement(numberTab);
        }

        helper.setLongArrayOrNull(deleteFeaturesStmt, 1, ids);
        deleteFeaturesStmt.execute();
    }

    @Override
    public void terminateFeature(long id, String metadata, boolean cascade) throws SQLException {
        if (terminateFeatureStmt == null) {
            terminateFeatureStmt = prepareTerminateStatement("number");
        }

        terminateFeatureStmt.setLong(1, id);
        setMetadata(terminateFeatureStmt, metadata);
        terminateFeatureStmt.setInt(3, cascade ? 1 : 0);
        terminateFeatureStmt.execute();
    }

    @Override
    public void terminateFeatures(Collection<Long> ids, String metadata, boolean cascade) throws SQLException {
        if (terminateFeaturesStmt == null) {
            terminateFeaturesStmt = prepareTerminateStatement(numberTab);
        }

        helper.setLongArrayOrNull(terminateFeaturesStmt, 1, ids);
        setMetadata(terminateFeaturesStmt, metadata);
        terminateFeaturesStmt.setInt(3, cascade ? 1 : 0);
        terminateFeaturesStmt.execute();
    }

    private void setMetadata(PreparedStatement stmt, String metadata) throws SQLException {
        if (metadata != null) {
            stmt.setString(2, metadata);
        } else {
            stmt.setNull(2, Types.VARCHAR);
        }
    }

    @Override
    public void close() throws SQLException {
        SQLException exception = null;
        for (PreparedStatement stmt : new PreparedStatement[]{
                deleteFeatureStmt, deleteFeaturesStmt, terminateFeatureStmt, terminateFeaturesStmt}) {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    if (exception == null) {
                        exception = e;
                    } else {
                        exception.addSuppressed(e);
                    }
                }
            }
        }

        deleteFeatureStmt = null;
        deleteFeaturesStmt = null;
        terminateFeatureStmt = null;
        terminateFeaturesStmt = null;

        if (exception != null) {
            throw exception;
        }
    }
}
