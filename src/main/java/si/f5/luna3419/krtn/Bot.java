package si.f5.luna3419.krtn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.DisconnectEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import si.f5.luna3419.krtn.config.GsonConfig;
import si.f5.luna3419.krtn.gson.GenericHashMapDeserializer;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Bot {
    @Getter private static Bot instance;

    private static volatile boolean isRunning = true;

    private final File directory;
    private final String token;

    private final Logger logger;

    @Getter private DiscordClient client;
    @Getter private GatewayDiscordClient gateway;
    @Getter private GsonConfig config;

    public static void main(String[] args) {
        new Thread(Bot::new).start();

        while (isRunning) {
            Thread.onSpinWait();
        }
    }

    public Bot() {
        instance = this;
        logger = LogManager.getLogger("Logger");

        directory = new File(System.getProperty("user.dir"));

        File cfg = new File(directory, "config.json");

        if (!cfg.exists()) {
            saveConfig();
            token = "";
            System.out.println("DiscordのTokenを入力して起動してください。\n自動終了します。");
        } else {
            try {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(new TypeToken<Map<String, Object>>() {}.getType(), new GenericHashMapDeserializer<>())
                        .serializeNulls()
                        .create();

                config = gson.fromJson(new InputStreamReader(new FileInputStream(cfg), StandardCharsets.UTF_8), GsonConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            token = config.getToken();

            launch();
        }
    }

    private void launch() {
        client = DiscordClient.create(token);
        gateway = client.login().block();

        assert gateway != null;

        gateway.on(MessageCreateEvent.class).subscribe(this::onMessage);
        gateway.on(DisconnectEvent.class).subscribe(this::onExit);

        logger.info("Command name: " + config.getName());
        logger.info("Command Description: " + config.getDescription());

        logger.info("Guild ID: " + config.getGuild());

        logger.info("Messages: ");
        config.getMessages().forEach((key, value) -> logger.info("  " + key + " -> " + value));
        logger.info("Commands: ");
        config.getCommands().forEach((key, value) -> logger.info("  " + key + " -> " + value));
    }

    public void onExit(DisconnectEvent e) {
        isRunning = false;
    }

    @SuppressWarnings("ConstantConditions")
    public void onMessage(MessageCreateEvent e) {
        User user;
        if (e.getMessage().getAuthor().isPresent()) {
            user = e.getMessage().getAuthor().get();
        } else {
            return;
        }

        logger.info(user.getUsername() + ":");
        logger.info(e.getMessage().getContent());

        if (user.isBot()) {
            return;
        }

        if (e.getMessage().getContent().contains(config.getName())) {
            String[] args = Utils.splitWithSpaces(e.getMessage().getContent().replace(config.getName() + " ", "").replace(config.getName() + "　", ""));

            onCommand(user, e.getMessage(), args);

            return;
        }

        String result = null;
        for (String str : config.getMessages().keySet()) {
            if (e.getMessage().getContent().contains(str)) {
                result = config.getMessages().get(str);
                break;
            }
        }

        if (result == null) {
            return;
        }

        result = Utils.s(user, result, null);

        logger.info(">> " + result);

        e.getMessage().getChannel().block().createMessage(MessageCreateSpec.builder().content(result).messageReference(e.getMessage().getId()).build()).block();
    }

    @SuppressWarnings("ConstantConditions")
    public void onCommand(User sender, Message message, String[] args) {
        List<String> list = Arrays.asList(args);

        String name = list.get(0);
        list.remove(0);

        args = list.toArray(new String[0]);

        if (config.getCommands().containsKey(name)) {
            String result = Utils.s(sender, config.getCommands().get(name), args);

            logger.info(">> " + result);

            message.getChannel().block().createMessage(MessageCreateSpec.builder().content(result).messageReference(message.getId()).build()).block();
        } else {
            message.getChannel().block().createMessage(MessageCreateSpec.builder().content(config.getMissing()).messageReference(message.getId()).build()).block();
        }
    }

    private void saveConfig() {
        String resourcePath = "config.json";

        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(directory, resourcePath);

        try {
            if (!outFile.exists()) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = getClass().getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
