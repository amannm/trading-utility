package systems.cauldron.utility.trading;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class ConfigurationFactory {

    private final static System.Logger LOG = System.getLogger(ConfigurationFactory.class.getName());

    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home")).resolve(".tradingutility");

    private static final JsonWriterFactory WRITER_FACTORY = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));

    public static void save(Configuration configuration) {
        try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            try (JsonWriter jsonWriter = WRITER_FACTORY.createWriter(writer)) {
                JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                        .add("clientId", configuration.getClientId())
                        .add("redirectUri", configuration.getRedirectUri())
                        .add("authorizationCodeContent", configuration.getRedirectUri() + "/?code=" + URLEncoder.encode(configuration.getAuthorizationCode(), StandardCharsets.UTF_8));
                Optional.ofNullable(configuration.getRefreshToken()).ifPresent(x -> jsonObjectBuilder.add("refreshToken", x));
                Optional.ofNullable(configuration.getRefreshTokenExpiry()).map(Instant::toEpochMilli).ifPresent(x -> jsonObjectBuilder.add("refreshTokenExpiry", x));
                jsonWriter.write(jsonObjectBuilder.build());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        LOG.log(System.Logger.Level.INFO, "configuration saved to disk");

    }

    public static Configuration load() {
        Configuration configuration = new Configuration();
        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            try (JsonReader jsonReader = Json.createReader(reader)) {
                JsonObject jsonObject = jsonReader.readObject();
                configuration.setClientId(jsonObject.getString("clientId"));
                configuration.setRedirectUri(jsonObject.getString("redirectUri"));
                configuration.setAuthorizationCode(URLDecoder.decode(jsonObject.getString("authorizationCodeContent").replace(jsonObject.getString("redirectUri") + "/?code=", ""), StandardCharsets.UTF_8));
                Optional.ofNullable(jsonObject.getJsonString("refreshToken")).map(JsonString::getString).ifPresent(configuration::setRefreshToken);
                Optional.ofNullable(jsonObject.getJsonNumber("refreshTokenExpiry")).map(JsonNumber::longValue).map(Instant::ofEpochMilli).ifPresent(configuration::setRefreshTokenExpiry);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        LOG.log(System.Logger.Level.INFO, "configuration loaded from disk");
        return configuration;
    }

}
