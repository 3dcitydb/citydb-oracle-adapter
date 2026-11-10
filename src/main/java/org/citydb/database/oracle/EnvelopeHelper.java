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
import org.citydb.database.geometry.GeometryException;
import org.citydb.model.geometry.Envelope;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class EnvelopeHelper extends org.citydb.database.util.EnvelopeHelper {
    private final DatabaseAdapter adapter;
    private final String citydbEnvelope;
    private final String citydbEnvelopeUpdate;
    private CallableStatement getEnvelopeStmt;
    private CallableStatement updateEnvelopeStmt;

    EnvelopeHelper(Connection connection, DatabaseAdapter adapter) {
        super(connection);
        this.adapter = adapter;
        String schema = adapter.getConnectionDetails().getSchema();
        citydbEnvelope = SchemaAdapter.qualify(schema, "citydb_envelope");
        citydbEnvelopeUpdate = SchemaAdapter.qualify(schema, "citydb_envelope_update");
    }

    @Override
    public Envelope getFeatureEnvelope(long id, Mode mode) throws GeometryException, SQLException {
        // storing is a separate package so that read-only users can still compute envelopes
        boolean store = mode == Mode.STORE_IF_MISSING || mode == Mode.RECOMPUTE_AND_STORE;
        boolean compute = mode == Mode.RECOMPUTE || mode == Mode.RECOMPUTE_AND_STORE;

        CallableStatement stmt;
        if (store) {
            if (updateEnvelopeStmt == null) {
                updateEnvelopeStmt = prepareCall(citydbEnvelopeUpdate + ".update_feature_envelope(?, ?)");
            }
            stmt = updateEnvelopeStmt;
        } else {
            if (getEnvelopeStmt == null) {
                getEnvelopeStmt = prepareCall(citydbEnvelope + ".get_feature_envelope(?, ?)");
            }
            stmt = getEnvelopeStmt;
        }

        stmt.setLong(2, id);
        stmt.setInt(3, compute ? 1 : 0);
        stmt.execute();
        return adapter.getGeometryAdapter().getEnvelope(stmt.getObject(1));
    }

    // the functions may run DML, so they cannot be used in a select (ORA-14551); read the result as OUT parameter
    private CallableStatement prepareCall(String function) throws SQLException {
        CallableStatement stmt = connection.prepareCall("{? = call " + function + "}");
        stmt.registerOutParameter(1, OracleTypes.STRUCT, "MDSYS.SDO_GEOMETRY");
        return stmt;
    }

    @Override
    public void close() throws SQLException {
        try (CallableStatement getEnvelopeStmt = this.getEnvelopeStmt;
             CallableStatement updateEnvelopeStmt = this.updateEnvelopeStmt) {
        }
    }
}
