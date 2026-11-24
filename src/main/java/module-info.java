module org.citydb.database.oracle {
    requires org.citydb.database;
    requires com.oracle.database.jdbc;
    requires com.oracle.spatial.geometry;
    requires com.oracle.spatial.util;

    exports org.citydb.database.oracle;

    provides org.citydb.database.adapter.DatabaseAdapter with org.citydb.database.oracle.OracleAdapter;
}
