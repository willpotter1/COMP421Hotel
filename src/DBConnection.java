import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String DRIVER = "com.ibm.db2.jcc.DB2Driver";
    private static final String HOST = "winter2026-comp421.cs.mcgill.ca";
    private static final int PORT = 50000;
    private static final String DATABASE = "COMP421";
    private static final String URL =
            "jdbc:db2://" + HOST + ":" + PORT + "/" + DATABASE;

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String user = System.getenv("SOCSUSER");
        String password = System.getenv("SOCSPASSWD");

        if (user == null || user.trim().isEmpty()) {
            throw new IllegalStateException("Environment variable SOCSUSER is not set");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException("Environment variable SOCSPASSWD is not set");
        }

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "DB2 JDBC driver not found. Add db2jcc.jar to the classpath.",
                    e
            );
        }

        return DriverManager.getConnection(URL, user, password);
    }
}
