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
import org.citydb.sqlbuilder.function.Function;
import org.citydb.sqlbuilder.literal.DoubleLiteral;
import org.citydb.sqlbuilder.literal.IntegerLiteral;
import org.citydb.sqlbuilder.query.CommonTableExpression;
import org.citydb.sqlbuilder.query.Select;
import org.citydb.sqlbuilder.query.Selection;
import org.citydb.sqlbuilder.schema.Table;
import org.citydb.sqlbuilder.util.PlainSql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class ChangelogHelper implements org.citydb.database.util.ChangelogHelper {
    private static final double TOLERANCE = 0.001;
    private final OracleAdapter adapter;

    ChangelogHelper(DatabaseAdapter adapter) {
        this.adapter = (OracleAdapter) adapter;
    }

    @Override
    public boolean isChangelogEnabled(String schemaName, Connection connection) throws SQLException {
        String sql = "select 1 from all_triggers " +
                "where owner = ? " +
                "and table_name = ? " +
                "and trigger_name = 'FEATURE_CHANGELOG_INSERT_UPDATE_TRIGGER' " +
                "and rownum = 1";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName.toUpperCase(Locale.ROOT));
            stmt.setString(2, org.citydb.database.schema.Table.FEATURE.getName().toUpperCase(Locale.ROOT));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public Select getChangeRegions(Select baseQuery, Selection<?> envelope) {
        return getAndTransformChangeRegions(baseQuery, envelope,
                adapter.getDatabaseMetadata().getSpatialReference().getSRID());
    }

    @Override
    public Select getAndTransformChangeRegions(Select baseQuery, Selection<?> envelope, int srid) {
        String alias = "extent";
        envelope.as(alias);
        Table base = Table.of(baseQuery);

        CommonTableExpression extents = CommonTableExpression.of("extents", Select.newInstance()
                .select(Function.of("MDSYS.sdo_cs.make_2d", base.column(alias)).as(alias))
                .from(base)
                .where(base.column(alias).isNotNull()));

        Selection<?> union = Function.of("MDSYS.sdo_aggr_set_union",
                PlainSql.of("cast(collect({}) as MDSYS.sdo_geometry_array)", extents.asTable().column(alias)),
                DoubleLiteral.of(TOLERANCE));
        if (adapter.getDatabaseMetadata().getSpatialReference().getSRID() != srid) {
            union = Function.of("MDSYS.sdo_cs.transform", union, IntegerLiteral.of(srid));
        }

        CommonTableExpression regions = CommonTableExpression.of("regions", Select.newInstance()
                .select(union.as("region"))
                .from(extents.asTable()));

        Table region = regions.asTable();
        Table elements = Table.of(Function.of("table", PlainSql.of(
                "cast(multiset(select level from dual connect by level <= MDSYS.sdo_util.getnumelem({})) " +
                        "as sys.odcinumberlist)", region.column("region"))));

        return Select.newInstance()
                .with(extents, regions)
                .select(Function.of("MDSYS.sdo_util.extract", region.column("region"), elements.column("column_value"))
                        .as("region"))
                .from(region, elements);
    }
}
