package dev.jacrispys.JavaBot.api.analytics.objects;

import dev.jacrispys.JavaBot.utils.mysql.MySQLConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AudioActivity {


    private final GuildUser user;

    private final Connection connection;
    private final long guildId;
    private static AudioActivity instance = null;

    public static AudioActivity getAudioActivity(AudioUser user) throws SQLException {
        return instance != null ? instance : new AudioActivity(user);
    }

    private AudioActivity(AudioUser user) throws SQLException {
        this.user = user;
        instance = this;
        this.guildId = user.getUserGuild().getIdLong();
        this.connection = MySQLConnection.getInstance().getConnection("inside_agent_bot");
    }

    protected Object getGuildStat(long guildId, AudioActivities activity) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT " + activity.name().toLowerCase() + " FROM audio_activity WHERE guild_ID=" + guildId + " AND user_ID=" + user.getUser().getIdLong());
        rs.beforeFirst();
        rs.next();
        return rs.getObject(activity.name().toLowerCase());
    }

    public void incrementStat(long guildId, AudioActivities activity) {
        try {
            Statement statement = connection.createStatement();
            long statValue = statement.executeQuery("SELECT " + activity.name().toLowerCase() + " FROM guild_general_stats WHERE ID=" + guildId).getLong(activity.name().toLowerCase());
            statement.executeUpdate("UPDATE guild_general_stats SET " + activity.name().toLowerCase() + "=" + statValue + 1 + " WHERE ID=" + guildId);
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void incrementStat(long guildId, int increment, AudioActivities activity) {
        try {
            Statement statement = connection.createStatement();
            long statValue = statement.executeQuery("SELECT " + activity.name().toLowerCase() + " FROM guild_general_stats WHERE ID=" + guildId).getLong(activity.name().toLowerCase());
            statement.executeUpdate("UPDATE guild_general_stats SET " + activity.name().toLowerCase() + "=" + statValue + increment + " WHERE ID=" + guildId);
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long getSongQueues() throws SQLException {
        return (long) getGuildStat(guildId, AudioActivities.SONG_QUEUES);
    }

    public long getPlayListQueues() throws SQLException {
        return (long) getGuildStat(guildId, AudioActivities.PLAYLIST_QUEUES);
    }

    public long getListenTimeMillis() throws SQLException {
        return (long) getGuildStat(guildId, AudioActivities.LISTEN_TIME);
    }

    public long getSkipOthers() throws SQLException {
        return (long) getGuildStat(guildId, AudioActivities.SKIP_OTHERS);
    }


}

enum AudioActivities {

    SONG_QUEUES,
    PLAYLIST_QUEUES,
    LISTEN_TIME,
    SKIP_OTHERS

}
