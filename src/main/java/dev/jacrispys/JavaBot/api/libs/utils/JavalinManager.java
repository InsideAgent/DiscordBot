package dev.jacrispys.JavaBot.api.libs.utils;

import dev.jacrispys.JavaBot.utils.SecretData;
import dev.jacrispys.JavaBot.utils.mysql.MySQLConnection;
import io.javalin.Javalin;
import net.dv8tion.jda.api.utils.data.DataObject;
import okhttp3.*;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

public class JavalinManager {

    private Javalin app;
    private final long CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String API_ENDPOINT = "https://discord.com/api/oauth2/token";
    private final String REDIRECT_URI = "https://bot.insideagent.pro";

    private final OkHttpClient client = new OkHttpClient();

    public JavalinManager(int port) {
        initJavalin(port);
        this.CLIENT_ID = SecretData.getDiscordId(true);
        this.CLIENT_SECRET = SecretData.getDiscordSecret(true);
    }


    protected void initJavalin(int port) {
        app = Javalin.create().start(port);
        app.get("/", ctx -> {
            String query = ctx.queryParam("code");
            if(query != null) {
                ctx.result("You May now close the tab.");
                if(!exchangeCode(query)) {
                    ctx.html("<body style=color:red;background-color:#121212;> ERROR: Invalid auth code! Please use a valid discord oauth method! If you think this is an error please contact an administrator. </body>");
                }
                return;
            }
            ctx.result("Error, invalid query!");
        });
    }

    protected boolean exchangeCode(String code) {
        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", String.valueOf(CLIENT_ID))
                .add("client_secret", CLIENT_SECRET)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .build();
        Request request = new Request.Builder()
                .url(API_ENDPOINT)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(requestBody)
                .build();
        try (Response response = client.newCall(request).execute()) {

            DataObject jsonResponse =  DataObject.fromJson(response.body().byteStream());
            if (jsonResponse.hasKey("error")) {
                return false;
            } else {
                String token = jsonResponse.get("token_type") + " " + jsonResponse.getString("access_token");
                Request getData = new Request.Builder()
                        .header("Authorization", token)
                        .url("https://discord.com/api/users/@me")
                        .build();
                Response data = client.newCall(getData).execute();
                DataObject userData = DataObject.fromJson(data.body().byteStream());
                authorizeUser(userData);
                return true;
            }

        } catch (IOException e) {
            return false;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return true;
        }
    }


    protected void authorizeUser(DataObject userData) throws SQLException {
        try {
            String email = userData.getString("email");
            String avatarUrl = "https://cdn.discordapp.com/avatars/" + userData.getString("id") + "/" + userData.getString("avatar");
            String user_tag = userData.getString("username") + "#" + userData.getString("discriminator");
            String token = tokenGenerator();
            long id = userData.getLong("id");

            MySQLConnection connection = MySQLConnection.getInstance();
            Connection sql = connection.getConnection("inside_agent_bot");
            Statement stmt = sql.createStatement();
            stmt.execute("REPLACE INTO api_auth (user_id, email, avatar_url, user_tag, token) VALUES ('" + id + "', '" + email + "', '" + avatarUrl + "', '" + user_tag + "', '" + token + "');");
            stmt.close();
    } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    protected String tokenGenerator() {
        SecureRandom secureRandom = new SecureRandom();
        Base64.Encoder encoder = Base64.getUrlEncoder();
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return encoder.encodeToString(randomBytes);
    }
}
