package com.aws.spring.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class CloudSqlConnectionPullFactory {
    public static final String INSTANCE_CONNECTION_NAME =
            "testlexchatbot:us-central1:product-app-database";
    public static final String DB_USER = "postgres";
    public static final String DB_PASS = "superpassword";
    public static final String DB_NAME = "products";

    public static DataSource createConnectionPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql:///%s", DB_NAME));
        config.setUsername(DB_USER);
        config.setPassword(DB_PASS);
        config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
        config.addDataSourceProperty("cloudSqlInstance", INSTANCE_CONNECTION_NAME);

        return new HikariDataSource(config);
    }
}
