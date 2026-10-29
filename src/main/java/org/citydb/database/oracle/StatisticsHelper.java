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
import org.citydb.database.metadata.DatabaseSize;
import org.citydb.sqlbuilder.function.Function;
import org.citydb.sqlbuilder.literal.StringLiteral;
import org.citydb.sqlbuilder.query.Selection;
import org.citydb.sqlbuilder.schema.Table;

import java.sql.Connection;
import java.util.Collections;

public class StatisticsHelper extends org.citydb.database.util.StatisticsHelper {

    StatisticsHelper(DatabaseAdapter adapter) {
        super(adapter);
    }

    @Override
    public DatabaseSize getDatabaseSize(Connection connection) {
        // Not supported yet: dba_segments needs DBA privileges and user_segments cannot cover a
        // schema other than the connected user's, so report all sizes as unknown instead of failing.
        return DatabaseSize.of(Collections.emptyMap());
    }

    @Override
    protected Selection<?> getGeometryType(Table geometryData) {
        return Function.of("json_value", geometryData.column("geometry_properties"), StringLiteral.of("$.type"));
    }
}