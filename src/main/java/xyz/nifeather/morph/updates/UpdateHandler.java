package xyz.nifeather.morph.updates;

import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.command.ServerCommandSender;
import org.jetbrains.annotations.Nullable;
import xiamomc.pluginbase.Annotations.Initializer;
import xiamomc.pluginbase.Annotations.Resolved;
import xiamomc.pluginbase.Bindables.Bindable;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xiamomc.pluginbase.Messages.FormattableMessage;
import xiamomc.pluginbase.ScheduleInfo;
import xyz.nifeather.morph.MorphPlugin;
import xyz.nifeather.morph.MorphPluginObject;
import xyz.nifeather.morph.config.ConfigOption;
import xyz.nifeather.morph.config.MorphConfigManager;
import xyz.nifeather.morph.messages.MessageUtils;
import xyz.nifeather.morph.messages.UpdateStrings;
import xyz.nifeather.morph.misc.permissions.CommonPermissions;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class UpdateHandler extends MorphPluginObject
{
    @Resolved(shouldSolveImmediately = true)
    private MorphPlugin plugin;

    private final AtomicInteger requestId = new AtomicInteger(0);

    private volatile ScheduleInfo sched;

    private final Bindable<Boolean> checkUpdate = new Bindable<>(true);

    @Initializer
    private void load(MorphConfigManager config)
    {
        config.bind(checkUpdate, ConfigOption.CHECK_UPDATE);

        this.update();
    }

    private void update()
    {
        // 每三小时检查一次更新
        this.addSchedule(this::update, 3 * 60 * 60 * 20);

        if (checkUpdate.get())
            this.checkUpdate(true, null);
    }

    public void checkUpdate(boolean sendMessages, @Nullable CommandSender forwardTarget)
    {
        this.checkUpdate(sendMessages, null, forwardTarget);
    }

    public void checkUpdate(boolean sendMessages, @Nullable Consumer<CheckResult> onFinish, @Nullable CommandSender forwardTarget)
    {
        if (this.sched != null)
        {
            sched.cancel();
            sched = null;
        }

        //Run async
        this.sched = this.addSchedule(() -> doCheckUpdate(sendMessages, onFinish, forwardTarget), 0, true);
    }

    private void doCheckUpdate(boolean sendMessages, @Nullable Consumer<CheckResult> onFinish, @Nullable CommandSender forwardTarget)
    {
        logger.info("Checking updates...");
        updateAvailable = false;

        var reqId = requestId.addAndGet(1);

        try
        {
            var urlString = "https://api.modrinth.com"
                    + "/v2/project/feathermorph/version"
                    + "?"
                    + "game_versions=[\"%s\"]";

            urlString = urlString.formatted(Bukkit.getMinecraftVersion())
                    .replace("[", "%5B") // Make URI happy
                    .replace("]", "%5D")
                    .replace("\"", "%22");

            var url = new URL(urlString).toURI();

            var request = HttpRequest.newBuilder()
                    .GET()
                    .uri(url)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "feathermorph")
                    .build();

            var client = HttpClient.newBuilder()
                            .followRedirects(HttpClient.Redirect.ALWAYS)
                            .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
            {
                logger.error("Failed to check update: Server returned HTTP code {}", response.statusCode());
                logger.error("Server response: {}", response.body());

                if (onFinish != null)
                    onFinish.accept(CheckResult.FAIL);

                return;
            }

            this.onUpdateReqFinish(response.body(), reqId, sendMessages, onFinish, forwardTarget);
        }
        catch (Throwable t)
        {
            this.onUpdateReqFail(t, reqId, onFinish);

            if (onFinish != null)
                onFinish.accept(CheckResult.FAIL);
        }
    }

    private void onUpdateReqFail(Throwable e, int reqId, @Nullable Consumer<CheckResult> onFinish)
    {
        if (this.requestId.get() != reqId)
            return;

        if (onFinish != null)
            onFinish.accept(CheckResult.FAIL);

        logger.error("Failed checking update: " + e.getMessage());
        e.printStackTrace();
    }

    private void onUpdateReqFinish(String responseStr, int reqId,
                                   boolean sendMessages, @Nullable Consumer<CheckResult> onFinish,
                                   @Nullable CommandSender forwardTarget)
    {
        if (this.requestId.get() != reqId)
            return;

        try
        {
            // 反序列化为Map
            // 之后看情况再考虑要不要反序列化成一个类
            var gson = new GsonBuilder().create();
            var versionList = gson.fromJson(responseStr, ArrayList.class);
            var metaList = new ObjectArrayList<SingleUpdateInfoMeta>();
            for (Object o : versionList)
            {
                if (o instanceof Map<?,?> map)
                    metaList.add(SingleUpdateInfoMeta.fromMap(map));
                else
                    logger.warn("Cant deserialize element to SingleUpdateInfoMeta: Not a map (" + o + ")");
            }

            var loader = Platforms.fromName(Bukkit.getName());
            var matchMeta = metaList.stream()
                    .filter(m ->
                    {
                        var supportedLoaders = m.supportedLoaders;
                        if (supportedLoaders == null) return false;

                        var isRelease = "Release".equalsIgnoreCase(m.versionType);
                        var loaderMatch = supportedLoaders.stream().anyMatch(s -> s.equalsIgnoreCase(loader.getImplName()));

                        return isRelease && loaderMatch;
                    }).findFirst().orElse(null);

            if (matchMeta == null)
            {
                logger.error("Unable to check update: This version of Minecraft is not listed yet, or your server '%s' is not supported"
                        .formatted(loader.getImplName()));

                if (onFinish != null)
                    onFinish.accept(CheckResult.FAIL);

                return;
            }

            var currentVersion = VersionHandling.toVersionInfo(plugin.getPluginMeta().getVersion());
            var latestVersion = VersionHandling.toVersionInfo(matchMeta.versionNumber);

            if (latestVersion.isInvalid())
            {
                if (onFinish != null)
                    onFinish.accept(CheckResult.FAIL);

                throw new NullDependencyException("Null version number from response: " + gson.toJson(matchMeta));
            }

            var compare = currentVersion.compare(latestVersion);

            if (compare == VersionHandling.CompareResult.EQUAL)
            {
                logger.info("Already on the latest version for " + Bukkit.getMinecraftVersion());

                if (onFinish != null)
                    onFinish.accept(CheckResult.ALREADY_LATEST);

                return;
            }

            if (compare == VersionHandling.CompareResult.NEWER)
            {
                logger.info("Your version is newer than released for %s!".formatted(Bukkit.getMinecraftVersion()));

                if (onFinish != null)
                    onFinish.accept(CheckResult.ALREADY_LATEST);

                return;
            }

            if (compare == VersionHandling.CompareResult.NOT_ON_SAME_CHANNEL)
                logger.info("We are not on the same channel with the latest release, assuming there is a new update!");

            // 提醒服务器关于更新的消息
            var serverOps = Bukkit.getOperators();
            var sendTargets = new ObjectArrayList<CommandSender>();

            if (forwardTarget == null)
            {
                serverOps.forEach(offlinePlayer ->
                {
                    var onlinePlayer = offlinePlayer.getPlayer();
                    if (onlinePlayer != null && onlinePlayer.hasPermission(CommonPermissions.CHECK_UPDATE))
                        sendTargets.add(onlinePlayer);
                });
            }
            else
            {
                if (!(forwardTarget instanceof ServerCommandSender || forwardTarget instanceof ConsoleCommandSender))
                    sendTargets.add(forwardTarget);
            }

            sendTargets.add(Bukkit.getConsoleSender());

            this.msgPrimary = UpdateStrings.newVersionAvailable()
                    .resolve("current", currentVersion.toString())
                    .resolve("origin", latestVersion.toString());

            this.msgSecondary = UpdateStrings.update_here()
                    .resolve("url", "https://modrinth.com/plugin/feathermorph");

            this.updateAvailable = true;

            if (sendMessages)
            {
                for (var sendTarget : sendTargets)
                    sendUpdateNotifyTo(sendTarget);
            }

            if (onFinish != null)
                onFinish.accept(CheckResult.HAS_UPDATE);
        }
        catch (Throwable t)
        {
            logger.error("Error occurred while processing response: %s".formatted(t.getMessage()));
            t.printStackTrace();

            if (onFinish != null)
                onFinish.accept(CheckResult.FAIL);
        }
    }

    private final FormattableMessage messageHeaderFooter = UpdateStrings.messageHeaderFooter();

    private final FormattableMessage noNewVersionAvailable = UpdateStrings.noNewVersionAvailable();

    @Nullable
    private FormattableMessage msgPrimary;

    @Nullable
    private FormattableMessage msgSecondary;

    private boolean updateAvailable = false;

    public boolean updateAvailable()
    {
        return updateAvailable;
    }

    public void sendUpdateNotifyTo(CommandSender sendTarget)
    {
        if (!updateAvailable)
        {
            sendTarget.sendMessage(MessageUtils.prefixes(sendTarget, noNewVersionAvailable));
            return;
        }

        sendTarget.sendMessage(MessageUtils.prefixes(sendTarget, messageHeaderFooter));
        sendTarget.sendMessage(MessageUtils.prefixes(sendTarget, msgPrimary));
        sendTarget.sendMessage(MessageUtils.prefixes(sendTarget, msgSecondary));
        sendTarget.sendMessage(MessageUtils.prefixes(sendTarget, messageHeaderFooter));
    }

    private static class InvalidOperationException extends RuntimeException
    {
        public InvalidOperationException() {
        }

        public InvalidOperationException(String message) {
            super(message);
        }

        public InvalidOperationException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidOperationException(Throwable cause) {
            super(cause);
        }
    }

    public enum CheckResult
    {
        HAS_UPDATE,
        ALREADY_LATEST,
        FAIL
    }
}
