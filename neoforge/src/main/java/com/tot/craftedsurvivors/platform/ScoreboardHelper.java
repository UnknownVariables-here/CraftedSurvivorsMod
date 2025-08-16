package com.tot.craftedsurvivors.platform;


import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.Objects;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.tot.craftedsurvivors.CommonCraftedSurvivorsMod.*;

@Mod("craftedsurvivors")
public class ScoreboardHelper {
    public ScoreboardHelper(IEventBus modEventBus) {

        NeoForge.EVENT_BUS.register(this);
    }

    //initialize display and lives
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        player.server.execute(() -> {
            ServerScoreboard scoreboard = player.server.getScoreboard();
            String livesKey = player.getScoreboardName() + "_lives";
            Objective livesObj = scoreboard.getObjective(livesKey);
            if (livesObj == null) {
                livesObj = scoreboard.addObjective(
                        livesKey,
                        ObjectiveCriteria.DUMMY,
                        Component.empty(),
                        ObjectiveCriteria.RenderType.INTEGER,
                        false,
                        null
                );
                scoreboard.getOrCreatePlayerScore(player, livesObj).set(3);
            }
            Objective displayObj = scoreboard.getObjective("player_display");
            if (displayObj != null) {
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
                scoreboard.removeObjective(displayObj);
            }
            displayObj = scoreboard.addObjective(
                    "player_display",
                    ObjectiveCriteria.DUMMY,
                    Component.literal("§a❤ Lives ❤"), //title
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
            );
            String heartColor = getHeartColor(player);
            int lives = scoreboard.getOrCreatePlayerScore(player, livesObj).get();
            String hearts = heartColor + "❤".repeat(Math.max(0, lives));
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(hearts), displayObj).set(lives);

            // Force display update
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, displayObj);
            player.connection.send(new ClientboundSetDisplayObjectivePacket(
                    DisplaySlot.SIDEBAR,
                    displayObj
            ));
        });
    }
    // setup teams if they don't exist
    @SubscribeEvent
    public void onServerStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        createTeamIfMissing(scoreboard, "feralteam", Component.literal("Feral"), ChatFormatting.RED);
        createTeamIfMissing(scoreboard, "criminalteam", Component.literal("CRIMINAL"), ChatFormatting.DARK_PURPLE);
        createTeamIfMissing(scoreboard, "healthyteam", Component.literal("Healthy"), ChatFormatting.GREEN);
        createTeamIfMissing(scoreboard, "atriskteam", Component.literal("At Risk"), ChatFormatting.YELLOW);
    }
    public PlayerTeam createTeamIfMissing(Scoreboard scoreboard,
                                          String id,
                                          Component displayName,
                                          ChatFormatting color) {
        PlayerTeam team = scoreboard.getPlayerTeam(id);
        if (team == null) {
            team = scoreboard.addPlayerTeam(id);
        }
        team.setDisplayName(displayName);
        team.setPlayerPrefix(Component.empty().withStyle(color));
        return team;
    }
    //anytime the score is changed the display is updated

    // this isnt working anymore and needs some king of rewrite
    @SubscribeEvent
    public void onScoreChanged(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String objectiveName = player.getScoreboardName() + "_lives";
            Objects.requireNonNull(player.getServer()).execute(() -> {
                Scoreboard scoreboard = player.getServer().getScoreboard();
                Objective livesObj = scoreboard.getObjective(objectiveName);

                if (livesObj != null) {
                    // Get current lives value
                    int lives = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(player.getScoreboardName()), livesObj).get();

                    // Update the display
                    updatePlayerScoreboard(player);
                }
            });
        }
    }
    // Whenever a player dies their score is deducted
    @SubscribeEvent
    public void onPlayerDeath(PlayerEvent.PlayerRespawnEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        player.server.execute(() -> {
            Scoreboard scoreboard = player.server.getScoreboard();
            Objective livesObj = scoreboard.getObjective(player.getScoreboardName() + "_lives");

            if (livesObj != null) {
                int current = scoreboard.getOrCreatePlayerScore(player, livesObj).get();
                if (current > 0) {
                    scoreboard.getOrCreatePlayerScore(player, livesObj).set(current - 1);
                }
            }
        });
    }
    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Scoreboard scoreboard = player.getScoreboard();
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (currentTeam != null && "criminalteam".equals(currentTeam.getName())) {
            setAtRiskTeam(player);
        }
    }
}