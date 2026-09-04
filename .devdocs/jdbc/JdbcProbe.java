import java.sql.*;
import java.util.Properties;

/**
 * OpsMonitor 联调 JDBC 探测工具（只读）
 * 用法: java -cp .;mysql-connector-java-8.0.28.jar JdbcProbe host port user mode [dbname]
 *   mode=list  列出所有数据库（只读）
 *   mode=count 统计指定库的表数量（只读）
 * 密码从环境变量 DB_PWD 读取，不在命令行/进程列表暴露
 */
public class JdbcProbe {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("usage: java JdbcProbe <host> <port> <user> <mode> [dbname]");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String user = args[2];
        String mode = args[3];
        String dbname = args.length > 4 ? args[4] : null;
        String pwd = System.getenv("DB_PWD");
        if (pwd == null || pwd.isEmpty()) {
            System.out.println("DB_PWD env not set");
            return;
        }
        String url = "jdbc:mysql://" + host + ":" + port + "/" + (dbname == null ? "" : dbname)
                + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=8000&socketTimeout=8000";
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pwd);
        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("CONNECTED OK to " + host + ":" + port + " as " + user);
            if ("list".equals(mode)) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SHOW DATABASES")) {
                    System.out.println("--- databases ---");
                    while (rs.next()) {
                        System.out.println("  " + rs.getString(1));
                    }
                }
            } else if ("count".equals(mode)) {
                if (dbname == null) {
                    System.out.println("dbname required for count mode");
                    return;
                }
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='" + dbname + "'")) {
                    if (rs.next()) {
                        System.out.println("tables in " + dbname + ": " + rs.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("CONN FAILED: " + e.getMessage());
        }
    }
}
