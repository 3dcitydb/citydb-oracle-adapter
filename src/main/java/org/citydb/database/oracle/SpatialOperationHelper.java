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

import org.citydb.sqlbuilder.function.Function;
import org.citydb.sqlbuilder.literal.DoubleLiteral;
import org.citydb.sqlbuilder.literal.IntegerLiteral;
import org.citydb.sqlbuilder.literal.ScalarExpression;
import org.citydb.sqlbuilder.literal.StringLiteral;
import org.citydb.sqlbuilder.operation.BooleanExpression;
import org.citydb.sqlbuilder.operation.Operators;
import org.citydb.sqlbuilder.util.PlainSql;

public class SpatialOperationHelper implements org.citydb.database.util.SpatialOperationHelper {
    private static final double TOLERANCE = 0.001;
    private final StringLiteral TRUE = StringLiteral.of("1");

    @Override
    public Function extent(ScalarExpression operand) {
        return Function.of("sdo_aggr_mbr", operand);
    }

    @Override
    public Function transform(ScalarExpression operand, int srid) {
        return Function.of("MDSYS.sdo_cs.transform", operand, IntegerLiteral.of(srid));
    }

    @Override
    public BooleanExpression bbox(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Function.of("MDSYS.sdo_filter", leftOperand, rightOperand, StringLiteral.of("querytype=WINDOW"));
    }

    @Override
    public BooleanExpression bbox3d(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Operators.and(bbox(leftOperand, rightOperand),
                Operators.le(minOrdinate(leftOperand, 3), maxOrdinate(rightOperand, 3)),
                Operators.ge(maxOrdinate(leftOperand, 3), minOrdinate(rightOperand, 3)));
    }

    @Override
    public BooleanExpression contains(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Function.of("MDSYS.sdo_relate", leftOperand, rightOperand, StringLiteral.of("mask=CONTAINS querytype=WINDOW"));
    }

    @Override
    public BooleanExpression crosses(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        // Oracle has no exact crosses: the operands must overlap and either differ in dimension
        // or both be lines that intersect in points only
        ScalarExpression leftDimension = dimension(leftOperand);
        ScalarExpression rightDimension = dimension(rightOperand);
        ScalarExpression intersection = Function.of("MDSYS.sdo_geom.sdo_intersection",
                leftOperand, rightOperand, DoubleLiteral.of(TOLERANCE));
        return Operators.and(overlaps(leftOperand, rightOperand),
                Operators.or(Operators.ne(leftDimension, rightDimension),
                        Operators.and(Operators.eq(leftDimension, IntegerLiteral.of(1)),
                                Operators.eq(Function.of("MDSYS.sdo_geom.sdo_length",
                                        intersection, DoubleLiteral.of(TOLERANCE)), IntegerLiteral.of(0)))));
    }

    @Override
    public BooleanExpression disjoint(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Function.of("MDSYS.sdo_relate", leftOperand, rightOperand, StringLiteral.of("mask=DISJOINT querytype=WINDOW"));
    }

    @Override
    public BooleanExpression equals(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Function.of("MDSYS.sdo_relate", leftOperand, rightOperand, StringLiteral.of("mask=EQUAL querytype=WINDOW"));
    }

    @Override
    public BooleanExpression intersects(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Function.of("MDSYS.sdo_relate", leftOperand, rightOperand, StringLiteral.of("mask=ANYINTERACT querytype=WINDOW"));
    }

    @Override
    public BooleanExpression overlaps(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Function.of("MDSYS.sdo_relate", leftOperand, rightOperand, StringLiteral.of("mask=OVERLAPBDYDISJOINT+OVERLAPBDYINTERSECT querytype=WINDOW"));
    }

    @Override
    public BooleanExpression touches(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Function.of("MDSYS.sdo_relate", leftOperand, rightOperand, StringLiteral.of("mask=TOUCH querytype=WINDOW"));
    }

    @Override
    public BooleanExpression within(ScalarExpression leftOperand, ScalarExpression rightOperand) {
        return Function.of("MDSYS.sdo_relate", leftOperand, rightOperand, StringLiteral.of("mask=INSIDE querytype=WINDOW"));
    }

    @Override
    public BooleanExpression dWithin(ScalarExpression leftOperand, ScalarExpression rightOperand, ScalarExpression distance) {
        return Function.of("MDSYS.sdo_within_distance", leftOperand, rightOperand, StringLiteral.of("distance=" + distance.getPlaceholders().get(0).getValue().get()));
    }

    @Override
    public BooleanExpression beyond(ScalarExpression leftOperand, ScalarExpression rightOperand, ScalarExpression distance) {
        return Operators.not(Function.of("MDSYS.sdo_within_distance", leftOperand, rightOperand, StringLiteral.of("distance=" + distance.getPlaceholders().get(0).getValue().get())));
    }

    private ScalarExpression minOrdinate(ScalarExpression operand, int dimension) {
        return Function.of("MDSYS.sdo_geom.sdo_min_mbr_ordinate", operand, IntegerLiteral.of(dimension));
    }

    private ScalarExpression maxOrdinate(ScalarExpression operand, int dimension) {
        return Function.of("MDSYS.sdo_geom.sdo_max_mbr_ordinate", operand, IntegerLiteral.of(dimension));
    }

    private ScalarExpression dimension(ScalarExpression operand) {
        // points are 0, lines 1 and everything else is treated as areal
        return PlainSql.of("decode(({}).get_gtype(), 1, 0, 5, 0, 2, 1, 6, 1, 2)", operand);
    }

    private ScalarExpression cast(ScalarExpression expression) {
        // Oracle Database does not require explicit casting to a geometry type.
        return expression;
    }
}