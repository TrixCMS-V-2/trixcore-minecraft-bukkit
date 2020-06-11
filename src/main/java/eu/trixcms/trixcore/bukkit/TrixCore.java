package eu.trixcms.trixcore.bukkit;

import eu.trixcms.trixcore.api.command.ICommandExecutor;
import eu.trixcms.trixcore.api.config.IConfig;
import eu.trixcms.trixcore.api.config.exception.InvalidConfigException;
import eu.trixcms.trixcore.api.container.CommandContainer;
import eu.trixcms.trixcore.api.i18n.Lang;
import eu.trixcms.trixcore.api.method.exception.DuplicateMethodNameException;
import eu.trixcms.trixcore.api.method.exception.InvalidMethodDefinitionException;
import eu.trixcms.trixcore.api.server.exception.InvalidPortException;
import eu.trixcms.trixcore.api.util.ServerTypeEnum;
import eu.trixcms.trixcore.bukkit.method.*;
import eu.trixcms.trixcore.common.CommandManager;
import eu.trixcms.trixcore.common.SchedulerManager;
import eu.trixcms.trixcore.common.TrixServer;
import eu.trixcms.trixcore.common.i18n.JsonMessageSource;
import eu.trixcms.trixcore.common.i18n.Translator;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class TrixCore extends JavaPlugin implements Listener, IConfig, ICommandExecutor<CommandContainer> {

    private static final Logger logger = LoggerFactory.getLogger(TrixCore.class);
    private static final int TRIXCORE_PLUGIN_ID = 6622;

    @Getter
    private static TrixCore instance;

    @Getter private TrixServer trixServer;
    @Getter private Translator translator;
    @Getter private SchedulerManager schedulerManager;
    @Getter private CommandManager commandManager;

    private FileConfiguration config;
    private File configFile = new File(getDataFolder(), "config.yml");

    @Override
    public void onLoad() {
        super.onLoad();

        config = getConfig();

        if (!configFile.exists()) {
            config.addDefault("port", 0);
            config.addDefault("secret_key", "");
            config.addDefault("custom_motd", null);
            config.options().copyDefaults(true);
            saveDefaultConfig();
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        translator = new Translator(JsonMessageSource.class, Lang.values());

        if (Bukkit.getPluginManager().isPluginEnabled("MinewebBridge")) {
            throw new RuntimeException(translator.of("PLUGIN_MINEWEB"));
        }

        if (Bukkit.getPluginManager().isPluginEnabled("AzLink")) {
            throw new RuntimeException(translator.of("PLUGIN_AZLINK"));
        }

        schedulerManager = new SchedulerManager(translator);
        commandManager = new CommandManager(this,
                translator,
                schedulerManager,
                new File(getDataFolder(), "commands.json"));
        trixServer = new TrixServer();

        try {
            trixServer
                    .translator(translator)
                    .scheduler(schedulerManager)
                    .commandManager(commandManager)
                    .serverType(ServerTypeEnum.BUKKIT)
                    .registerMethods(
                            new GetBannedPlayersMethod(),
                            new GetPlayerListMethod(),
                            new GetServerInfoMethod(),
                            new GetWhiteListMethod(),
                            new IsBannedMethod(),
                            new IsConnectedMethod(),
                            new RunCommandMethod(),
                            new RunScheduledCommandMethod(),
                            new SetMOTDMethod(),
                            new RemoveScheduledCommandsMethod()
                    );
        } catch (DuplicateMethodNameException | InvalidMethodDefinitionException e) {
            logger.error(translator.of("ERROR"), e);
        }

        try {
            trixServer.config(this);

            logger.info(translator.of("STARTING_SERVER"));
            trixServer.start();
        } catch (InvalidPortException e) {
            logger.error(translator.of("PORT_HELP"));
            logger.error(translator.of("ERROR"), e);
        } catch (IOException e) {
            logger.error(translator.of("ERROR"), e);
        } catch (InvalidConfigException e) {
            logger.error(translator.of("UNKNOWN_SAVER"), e);
        }

        new Metrics(this, TRIXCORE_PLUGIN_ID);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        logger.info(TrixCore.getInstance().getTranslator().of("STOPPING_SERVER"));
        trixServer.stop();

        try {
            this.config.save(this.configFile);
        } catch (IOException ignored) {
        }
    }

    @Override
    public String getSecretKey() {
        if (this.config != null && this.config.getString("secret_key") != null && !this.config.getString("secret_key").isEmpty())
            return config.getString("secret_key");

        return "";
    }

    @Override
    public Integer getServerPort() {
        if (this.config != null)
            return this.config.getInt("port");

        return 0;
    }

    @Override
    public void saveSecretKey(String key) throws IOException {
        logger.info(translator.of("SAVER_SAVING_SECRET_KEY"));
        this.config.set("secret_key", key);
        this.config.save(this.configFile);
    }

    @Override
    public void saveServerPort(Integer port) throws IOException {
        logger.info(translator.of("SAVER_SAVING_SERVER_PORT"));
        this.config.set("port", port);
        this.config.save(this.configFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("trixcore")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
                if (this.getServerPort() == 0) {
                    logger.info(translator.of("CMD_PORT_ALREADY_RESET"));
                    return true;
                }

                trixServer.stop();

                try {
                    this.trixServer.setSecretKey("");
                    this.trixServer.setPort(-1);
                } catch (IOException e) {
                    logger.error(translator.of("ERROR"), e);
                } catch (InvalidPortException ignored) {
                }

                sender.sendMessage(translator.of("CMD_PORT_SUCCESSFULLY_RESET"));
                logger.info(translator.of("CMD_PORT_SUCCESSFULLY_RESET"));
                return true;
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
                if (this.getServerPort() != 0) {
                    logger.error(translator.of("CMD_NEED_RESET_BEFORE"));
                    return true;
                }

                trixServer.stop();

                try {
                    this.trixServer.setPort(Integer.parseInt(args[1]));
                } catch (InvalidPortException e) {
                    logger.error(translator.of("PORT_HELP"));
                    logger.error(translator.of("ERROR"), e);
                    sender.sendMessage(translator.of("PORT_HELP"));
                    sender.sendMessage(translator.of("ERROR") + e.getMessage());
                } catch (IOException e) {
                    logger.error(translator.of("ERROR"), e);
                }

                sender.sendMessage(translator.of("CMD_PORT_SUCCESSFULLY_SETUP", this.config.getInt("port") + ""));

                logger.info(translator.of("CMD_PORT_SUCCESSFULLY_SETUP", this.config.getInt("port") + ""));

                try {
                    trixServer.config(this).start();
                } catch (InvalidPortException e) {
                    logger.error(translator.of("PORT_HELP"));
                    logger.error(translator.of("ERROR"), e);
                    sender.sendMessage(translator.of("PORT_HELP"));
                    sender.sendMessage(translator.of("ERROR") + e.getMessage());
                } catch (InvalidConfigException e) {
                    sender.sendMessage(translator.of("UNKNOWN_SAVER") + e.getMessage());
                    logger.error(translator.of("UNKNOWN_SAVER"), e);
                } catch (IOException e) {
                    logger.error(translator.of("ERROR"), e);
                }

                return true;
            }
        }
        return false;
    }

    @Override
    public boolean executeCommand(CommandContainer commandContainer) {
        logger.debug(translator.of("HTTP_RUNNING_COMMAND", commandContainer.getCmd()));
        try {
            return Bukkit.getScheduler().callSyncMethod(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandContainer.getCmd())).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error(translator.of("ERROR"), e);
            return false;
        }
    }

    public void saveMotd(String motd) throws IOException {
        this.config.set("custom_motd", motd);
        this.config.save(this.configFile);
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (config.isString("custom_motd")) {
            event.setMotd(Objects.requireNonNull(config.getString("custom_motd")));
        }
    }
}
