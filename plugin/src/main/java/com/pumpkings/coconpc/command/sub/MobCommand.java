package com.pumpkings.coconpc.command.sub;

import com.pumpkings.coconpc.CocoNPC;
import com.pumpkings.coconpc.command.SubCommand;
import com.pumpkings.coconpc.core.config.Message;
import com.pumpkings.coconpc.core.mob.CustomMob;
import com.pumpkings.coconpc.core.mob.MobDefinition;
import com.pumpkings.coconpc.core.mob.MobTextureService;
import com.pumpkings.coconpc.core.mob.model.BbModel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /coconpc mob ...} — manages real .bbmodel mobs loaded from
 * {@code plugins/CocoNPC/mobs}.
 */
public class MobCommand extends SubCommand {

    public MobCommand(CocoNPC plugin) {
        super(plugin);
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            Message.MOB_USAGE.send(plugin, player);
            return;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "list" -> list(player);
            case "spawn" -> spawn(player, args);
            case "remove", "delete" -> remove(player, args);
            case "reload" -> reload(player);
            case "info" -> info(player, args);
            default -> Message.MOB_USAGE.send(plugin, player);
        }
    }

    private void list(Player player) {
        List<MobDefinition> definitions = new ArrayList<>(plugin.getMobRegistry().getDefinitions());
        if (definitions.isEmpty()) {
            Message.MOB_LIST_EMPTY.send(plugin, player);
            return;
        }

        definitions.sort(Comparator.comparing(MobDefinition::getId));
        Message.MOB_LIST_HEADER.send(plugin, player);

        MobTextureService textures = plugin.getMobTextureService();
        for (MobDefinition definition : definitions) {
            String status = status(definition, textures);
            Message.MOB_LIST_ENTRY.send(plugin, player,
                    "{id}", definition.getId(),
                    "{driver}", definition.getDriverType().name(),
                    "{cubes}", String.valueOf(cubeCount(definition)),
                    "{alive}", String.valueOf(plugin.getMobRegistry()
                            .countMobs(definition.getId())),
                    "{status}", status);
        }
    }

    private void spawn(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_SPAWN_USAGE.send(plugin, player);
            return;
        }

        String id = args[2];
        MobDefinition definition = plugin.getMobRegistry().getDefinition(id);
        if (definition == null) {
            Message.MOB_NOT_FOUND.send(plugin, player, "{id}", id);
            return;
        }

        Location location = playerFacingLocation(player);
        Message.MOB_SPAWNING.send(plugin, player, "{id}", definition.getId());

        CompletableFuture<Void> ready = plugin.getMobTextureService().whenReady(definition.getId());
        ready.whenComplete((ignored, error) -> {
            if (error != null) {
                sendLater(player, Message.MOB_TEXTURE_FAILED,
                        "{id}", definition.getId(),
                        "{error}", rootMessage(error));
                return;
            }

            runAt(location, () -> {
                MobDefinition current = plugin.getMobRegistry().getDefinition(definition.getId());
                if (current == null) {
                    sendLater(player, Message.MOB_NOT_FOUND, "{id}", definition.getId());
                    return;
                }

                try {
                    CustomMob mob = plugin.getMobRegistry().spawn(current, location);
                    if (mob == null) {
                        sendLater(player, Message.MOB_SPAWN_FAILED,
                                "{id}", definition.getId(),
                                "{error}", "unknown error");
                    } else {
                        sendLater(player, Message.MOB_SPAWNED, "{id}", definition.getId());
                    }
                } catch (RuntimeException exception) {
                    plugin.getLogger().warning("[CocoNPC] Could not spawn mob '"
                            + definition.getId() + "': " + exception.getMessage());
                    sendLater(player, Message.MOB_SPAWN_FAILED,
                            "{id}", definition.getId(),
                            "{error}", rootMessage(exception));
                }
            });
        });
    }

    private void remove(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_REMOVE_USAGE.send(plugin, player);
            return;
        }

        String id = args[2];
        if (id.equalsIgnoreCase("all")) {
            int amount = plugin.getMobRegistry().removeMobs(null);
            Message.MOB_REMOVED_ALL.send(plugin, player, "{amount}", String.valueOf(amount));
            return;
        }

        MobDefinition definition = plugin.getMobRegistry().getDefinition(id);
        if (definition == null) {
            Message.MOB_NOT_FOUND.send(plugin, player, "{id}", id);
            return;
        }

        int amount = plugin.getMobRegistry().removeMobs(definition.getId());
        Message.MOB_REMOVED.send(plugin, player,
                "{amount}", String.valueOf(amount),
                "{id}", definition.getId());
    }

    private void reload(Player player) {
        plugin.reloadMobs();
        Message.MOB_RELOADED.send(plugin, player,
                "{amount}", String.valueOf(plugin.getMobRegistry().getDefinitionCount()));
    }

    private void info(Player player, String[] args) {
        if (args.length < 3) {
            Message.MOB_INFO_USAGE.send(plugin, player);
            return;
        }

        MobDefinition definition = plugin.getMobRegistry().getDefinition(args[2]);
        if (definition == null) {
            Message.MOB_NOT_FOUND.send(plugin, player, "{id}", args[2]);
            return;
        }

        BbModel model = definition.getModel();
        Message.MOB_INFO_HEADER.send(plugin, player, "{id}", definition.getId());
        line(player, "Driver", definition.getDriverType().name());
        line(player, "Model", model != null ? model.getName() : "unknown");
        line(player, "Cubes", String.valueOf(cubeCount(definition)));
        line(player, "Animations", String.valueOf(model != null
                ? model.getAnimations().size() : 0));
        line(player, "Texture mode", definition.getTextureMode().name());
        line(player, "AI", String.valueOf(definition.isAiEnabled()));
        line(player, "Gravity", String.valueOf(definition.isGravity()));
        line(player, "Collidable", String.valueOf(definition.isCollidable()));
        line(player, "Invulnerable", String.valueOf(definition.isInvulnerable()));
        line(player, "Marker", String.valueOf(definition.isMarker()));
        line(player, "Scale", String.valueOf(definition.getScale()));
        line(player, "Yaw offset", String.valueOf(definition.getYawOffset()));
        line(player, "Textures", status(definition, plugin.getMobTextureService()));
        line(player, "Spawned", String.valueOf(plugin.getMobRegistry()
                .countMobs(definition.getId())));
    }

    private void line(Player player, String key, String value) {
        Message.MOB_INFO_LINE.send(plugin, player, "{key}", key, "{value}", value);
    }

    private String status(MobDefinition definition, MobTextureService textures) {
        if (textures == null) return Message.MOB_TEXTURES_GENERATING.get(plugin);

        String error = textures.getError(definition.getId());
        if (error != null) {
            return Message.MOB_TEXTURES_FAILED.get(plugin);
        }
        if (textures.isReady(definition.getId())) {
            return Message.MOB_TEXTURES_READY.get(plugin);
        }
        return Message.MOB_TEXTURES_GENERATING.get(plugin);
    }

    private static int cubeCount(MobDefinition definition) {
        BbModel model = definition.getModel();
        if (model == null) return 0;

        int cubes = 0;
        for (BbModel.Bone bone : model.getBones()) {
            cubes += bone.getCubes().size();
        }
        return cubes;
    }

    private void runAt(Location location, Runnable action) {
        try {
            Bukkit.getRegionScheduler().run(plugin, location, task -> action.run());
        } catch (RuntimeException exception) {
            action.run();
        }
    }

    private void sendLater(Player player, Message message, String... replacements) {
        if (player == null) return;
        try {
            player.getScheduler().run(plugin, task -> message.send(plugin, player, replacements), null);
        } catch (RuntimeException exception) {
            message.send(plugin, player, replacements);
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}
