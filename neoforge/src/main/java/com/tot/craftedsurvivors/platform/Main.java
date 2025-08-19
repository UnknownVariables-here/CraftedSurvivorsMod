package com.tot.craftedsurvivors.platform;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.world.level.GameType.SPECTATOR;
import static net.minecraft.world.level.GameType.SURVIVAL;

@Mod("craftedsurvivors")
public class Main {
    private static final int DEFAULT_LIVES = 3;
    public static final Map<UUID, PlayerLifeData> playerData = new ConcurrentHashMap<>();
    public Main(IEventBus modEventBus) {
        modEventBus.addListener(this::onModSetup);
        NeoForge.EVENT_BUS.register(this);
    }
    private void onModSetup(FMLCommonSetupEvent event) {
        Path dataDir = FMLPaths.CONFIGDIR.get().resolve("craftedsurvivors");
        PlayerDataManager.init(dataDir);
        Path configPath = dataDir.resolve("Config.json");
        Config.init(configPath);
    }
    // Player Life Data
    public static class PlayerLifeData {
        private int lives;
        private final String name;
        private boolean criminal;
        public PlayerLifeData(int lives, String name) {this.lives = Math.max(0, lives);this.name = name;}
        public int getLives() {return lives;}
        public void setLives(int lives) {this.lives = Math.max(0, lives);}
        public String getName() {return name;}
        public boolean isCriminal() {return criminal;}
        public void setCriminal(boolean criminal) {this.criminal = criminal;}
    }
    // Events
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        PlayerDataManager.saveData();
    }
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            PlayerDataManager.saveData();
        }
    }
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID id = player.getUUID();
            playerData.putIfAbsent(id, new PlayerLifeData(DEFAULT_LIVES, player.getScoreboardName()));
            PlayerLifeData data = playerData.get(id);
            handleLifeChange(player, data);
            applyPlayerData(player);
        }
    }
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("csm")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("resetall")
                                .executes(ctx -> {
                                    resetAllPlayers(ctx.getSource());
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("setlives")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    setLives(target.getUUID(), amount);
                                                    ctx.getSource().sendSuccess(
                                                            () -> Component.literal("Set " + target.getName().getString() +
                                                                    "'s lives to " + amount),
                                                            true);
                                                    return Command.SINGLE_SUCCESS;
                                                }))))
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal("Players with lives:"), false);
                                    playerData.forEach((uuid, data) ->
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal(" - " + data.getName() + ": " + data.getLives()), false));
                                    return Command.SINGLE_SUCCESS;
                                }))
        );
    }
    @SubscribeEvent
    public void onPlayerDeath(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerLifeData data = playerData.get(player.getUUID());
            if (data != null) {
                boolean wasCriminal = data.isCriminal();
                data.setCriminal(false);
                player.setGlowingTag(false);
                data.setLives(data.getLives() - 1);
                handleLifeChange(player, data);
                applyPlayerData(player);
                checkLastLife(player, data);
                Component hearts = getHearts(data.getLives(), false);
                if (wasCriminal) {
                    player.sendSystemMessage(
                            Component.literal("You've been rehabilitated! Lives: ")
                                    .withStyle(ChatFormatting.DARK_PURPLE)
                                    .append(hearts),
                            false
                    );
                    MinecraftServer server = player.getServer();
                    if (server != null) {
                        server.getPlayerList().broadcastSystemMessage(
                                Component.literal("Criminal " + player.getScoreboardName() + " has been reformed!")
                                        .withStyle(ChatFormatting.DARK_PURPLE),
                                false
                        );
                    }
                } else {
                    player.sendSystemMessage(
                            Component.literal("Live(s) Remaining: ").withStyle(ChatFormatting.RED)
                                    .append(hearts),
                            false
                    );
                }
            }
        }
    }
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            DamageSource source = event.getSource();
            if (source.getEntity() instanceof ServerPlayer killer && killer != victim) {
                handlePlayerKill(killer, victim);
            }
        }
    }
    @SubscribeEvent
    public void onPlayerUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!Config.instance.enableRevival) return;
        ItemStack stack = event.getItemStack();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || !itemId.toString().equals(Config.instance.item)) return;
        ServerLevel level = (ServerLevel) player.getCommandSenderWorld();
        ServerPlayer nearestSpectator = level.getPlayers(p -> p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR)
                .stream()
                .filter(p -> p.distanceToSqr(player) <= 30 * 30)
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(player)))
                .orElse(null);
        if (nearestSpectator == null) {
            player.sendSystemMessage(
                    Component.literal("No souls nearby to revive...")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return;
        }
        if (stack.getCount() < Config.instance.amount) {
            player.sendSystemMessage(
                    Component.literal("You need at least " + Config.instance.amount + " " + stack.getHoverName().getString() + " to revive a soul")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return;
        }
        stack.shrink(Config.instance.amount);
        Main.PlayerLifeData data = Main.playerData.get(nearestSpectator.getUUID());
        if (data != null) {
            data.setLives(data.getLives() + 1);
            applyPlayerData(nearestSpectator);
            handleLifeChange(nearestSpectator, data);
            checkLastLife(nearestSpectator, data);
            nearestSpectator.sendSystemMessage(
                    Component.literal("You were reborn from " + player.getScoreboardName() + "'s sacrifice")
                            .withStyle(ChatFormatting.GREEN),
                    false
            );
        }
    }
    // Logic
    private void resetAllPlayers(CommandSourceStack source) {
        int resetCount = 0;
        MinecraftServer server = source.getServer();
        for (Map.Entry<UUID, PlayerLifeData> entry : playerData.entrySet()) {
            PlayerLifeData data = entry.getValue();
            data.setLives(DEFAULT_LIVES);
            data.setCriminal(false);
            resetCount++;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                applyPlayerData(player);
                handleLifeChange(player, data);
            }
        }
        PlayerDataManager.saveData();
        final int count = resetCount;
        source.sendSuccess(() -> Component.literal(
                "Reset " + count + " players to default lives and cleared criminal status"
        ), true);

    }
    public void setLives(UUID playerId, int amount) {
        PlayerLifeData data = playerData.get(playerId);
        if (data != null) {
            data.setLives(amount);
            ServerPlayer player = ServerLifecycleHooks.getCurrentServer()
                    .getPlayerList().getPlayer(playerId);
            if (player != null) {
                handleLifeChange(player, data);
                checkLastLife(player, data);
                applyPlayerData(player);
            }
        }
    }
    private void handleLifeChange(ServerPlayer player, PlayerLifeData data) {
        if (data.getLives() < 1 && player.gameMode.getGameModeForPlayer() == SURVIVAL) {
            player.setGameMode(SPECTATOR);
        }
        if (data.getLives() >= 1 && player.gameMode.getGameModeForPlayer() == SPECTATOR) {
            player.setGameMode(SURVIVAL);
        }
    }
    private void handlePlayerKill(ServerPlayer killer, ServerPlayer victim) {
        PlayerLifeData killerData = playerData.get(killer.getUUID());
        PlayerLifeData victimData = playerData.get(victim.getUUID());
        if (killerData == null || victimData == null) return;
        if (killerData.getLives() >= 3 && victimData.getLives() >= 3) {
            killerData.setCriminal(true);
            applyPlayerData(killer);
            applyPlayerData(victim);
            killer.sendSystemMessage(Component.literal("You're a criminal!")
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
            victim.sendSystemMessage(Component.literal("It seems you've been betrayed!")
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        if (Config.instance.lastLifeKillGainEnabled && killerData.getLives() <= 1) {
            killerData.setLives(killerData.getLives() + 1);
            killer.sendSystemMessage(
                    Component.literal("You gained a life by taking one of " + victim.getScoreboardName() + "'s lives.. But at what cost?")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            applyPlayerData(killer);
            handleLifeChange(killer, killerData);
        }
    }
    private static void checkLastLife(ServerPlayer player, PlayerLifeData data) {
        if (data.getLives() == 1) {
            player.sendSystemMessage(
                    Component.literal("You are on your last life! Take out other players to survive!").withStyle(ChatFormatting.RED),
                    false
            );
        }
    }
    // Tablist + Glow + Hearts
    private static void applyPlayerData(ServerPlayer player) {
        PlayerLifeData data = playerData.get(player.getUUID());
        if (data == null) return;
        player.setGlowingTag(data.isCriminal());
        Scoreboard scoreboard = player.getScoreboard();
        String teamName = "life_" + player.getUUID().toString().replace("-", "").substring(0, 16);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        if (!team.getPlayers().contains(player.getScoreboardName())) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
        if (data.isCriminal()) {
            team.setColor(ChatFormatting.DARK_PURPLE);
            team.setPlayerPrefix(Component.literal("").withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            team.setColor(ChatFormatting.WHITE);
            team.setPlayerPrefix(Component.empty());
        }
        Component hearts = getHearts(data.getLives(), data.isCriminal());
        team.setPlayerSuffix(Component.literal(" ").append(hearts));
        scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        player.refreshTabListName();
    }
    private static Component getHearts(int lives, boolean criminal) {
        ChatFormatting color = criminal ? ChatFormatting.DARK_PURPLE : switch (lives) {
            case 0 -> ChatFormatting.DARK_GRAY;
            case 1 -> ChatFormatting.RED;
            case 2 -> ChatFormatting.YELLOW;
            default -> ChatFormatting.GREEN;
        };
        return Component.literal("[" + "❤".repeat(Math.max(0, lives)) + "]").withStyle(color);
    }
}