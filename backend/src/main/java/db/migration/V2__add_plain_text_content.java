package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jsoup.Jsoup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Adds a plain-text copy of post and page content (HTML stripped) for search.
 * Written in Java rather than SQL so existing rows are converted with Jsoup,
 * exactly like the entities do on every save; a SQL regex cannot parse HTML reliably.
 */
public class V2__add_plain_text_content extends BaseJavaMigration {

    private static final String[] TABLES = { "posts", "pages" };

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        for (String table : TABLES) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ADD COLUMN content_text TEXT");
            }
            backfill(connection, table);
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ALTER COLUMN content_text SET NOT NULL");
            }
        }
    }

    private void backfill(Connection connection, String table) throws SQLException {
        try (Statement select = connection.createStatement();
                ResultSet rows = select.executeQuery("SELECT id, content FROM " + table);
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE " + table + " SET content_text = ? WHERE id = ?")) {
            while (rows.next()) {
                update.setString(1, Jsoup.parse(rows.getString("content")).text());
                update.setLong(2, rows.getLong("id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }
}
