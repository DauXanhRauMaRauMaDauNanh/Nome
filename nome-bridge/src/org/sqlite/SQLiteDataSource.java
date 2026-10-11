package org.sqlite;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class SQLiteDataSource implements DataSource {
    private String url = "jdbc:sqlite:";
    private SQLiteConfig config = new SQLiteConfig();

    public SQLiteDataSource() {}

    public SQLiteDataSource(SQLiteConfig config) {
        this.config = config;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setConfig(SQLiteConfig config) {
        this.config = config;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() { return null; }
    @Override
    public void setLogWriter(PrintWriter out) {}
    @Override
    public void setLoginTimeout(int seconds) {}
    @Override
    public int getLoginTimeout() { return 0; }
    @Override
    public Logger getParentLogger() { return Logger.getLogger("org.sqlite"); }
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not supported"); }
    @Override
    public boolean isWrapperFor(Class<?> iface) { return false; }
}
