package com.Tot.craftedsurvivors;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

import static com.Tot.craftedsurvivors.PlayerEventsHandler.*;
import static net.minecraft.network.chat.Component.literal;



@Mod("craftedsurvivors")
public class CraftedSurvivorsMod {
    public CraftedSurvivorsMod() {

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
        MinecraftForge.EVENT_BUS.register(new ScoreboardHelper());
        MinecraftForge.EVENT_BUS.register(new PlayerEventsHandler());

    }




//    @SubscribeEvent
//    public void onServerStart(ServerAboutToStartEvent event) {
//        MinecraftServer server = event.getServer();
//        Scoreboard scoreboard = server.getScoreboard();
//        createTeamIfMissing(scoreboard, "feralteam", "Feral", ChatFormatting.RED);
//        createTeamIfMissing(scoreboard, "criminalteam", "CRIMINAL", ChatFormatting.DARK_PURPLE);
//        createTeamIfMissing(scoreboard, "healthyteam", "Healthy", ChatFormatting.GREEN);
//        createTeamIfMissing(scoreboard, "atriskteam", "At Risk", ChatFormatting.YELLOW);
//    }
//    private static void createTeamIfMissing(Scoreboard scoreboard, String teamName, String displayName, ChatFormatting color) {
//        if (scoreboard.getPlayerTeam(teamName) == null) {
//            scoreboard.addPlayerTeam(teamName);
//        }
//    }

    @SubscribeEvent
    public void onServerStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        createTeamIfMissing(scoreboard, "feralteam", literal("Feral"), ChatFormatting.RED);
        createTeamIfMissing(scoreboard, "criminalteam", literal("CRIMINAL"), ChatFormatting.DARK_PURPLE);
        createTeamIfMissing(scoreboard, "healthyteam", literal("Healthy"), ChatFormatting.GREEN);
        createTeamIfMissing(scoreboard, "atriskteam", literal("At Risk"), ChatFormatting.YELLOW);
    }
    public static PlayerTeam createTeamIfMissing(Scoreboard scoreboard,
                                                  String id,
                                                  Component displayName,
                                                  ChatFormatting color) {
        PlayerTeam team = scoreboard.getPlayerTeam(id);
        if (team == null) {
            team = scoreboard.addPlayerTeam(id);
        }
        team.setDisplayName(displayName);
        team.setColor(color);
        return team;
    }

    // on player login setup scoreboard or update its display
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();

        player.server.execute(() -> {
            Scoreboard scoreboard = player.server.getScoreboard();

            // Create or get lives tracking objective
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

            // Handle display objective
            Objective displayObj = scoreboard.getObjective("player_display");
            if (displayObj != null) {
                // Clear existing display first
                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
                scoreboard.removeObjective(displayObj);
            }

            // Create fresh display objective with hearts title even on login
            displayObj = scoreboard.addObjective(
                    "player_display",
                    ObjectiveCriteria.DUMMY,
                    literal("§a❤ Lives ❤"), //title
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
            );


            //calling getHeartColor var to be used here
            String heartColor = getHeartColor(player);

            // Convert lives to hearts on side display
            int lives = scoreboard.getOrCreatePlayerScore(player, livesObj).get();
            String hearts = heartColor + "❤".repeat(Math.max(0, lives));

            // Set the combined display
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(hearts), displayObj).set(lives);

            // Force display update
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, displayObj);
            player.connection.send(new ClientboundSetDisplayObjectivePacket(
                    DisplaySlot.SIDEBAR,
                    displayObj
            ));
        });
    }


    public static void updatePlayerScoreboard(ServerPlayer player) {
        player.server.execute(() -> {
            Scoreboard scoreboard = player.server.getScoreboard();
            String objectiveName = "player_display_" + player.getUUID();

            // Clear existing
            Objective existingObj = scoreboard.getObjective(objectiveName);
            if (existingObj != null) {
                player.connection.send(new ClientboundSetObjectivePacket(existingObj, 1));
                scoreboard.removeObjective(existingObj);
            }

            int lives = getPlayerLives(player);
            if (lives <= 0) return;

            // Create fresh objective
            Objective displayObj = scoreboard.addObjective(
                    objectiveName,
                    ObjectiveCriteria.DUMMY,
                    Component.literal("§a❤ Lives ❤"),
                    ObjectiveCriteria.RenderType.INTEGER,
                    false,
                    null
            );

            // Add hearts display (using correct set() method)
            String hearts = getHeartColor(player) + "❤".repeat(lives);
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(hearts), displayObj).set(lives);

            // Add lives count
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("Lives: " + lives), displayObj).set(lives);

            // Force update
            player.connection.send(new ClientboundSetObjectivePacket(displayObj, 0));
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, displayObj));
        });
    }

    //function which updates the display when called
//    public static void updatePlayerScoreboard(ServerPlayer player) {
//        player.server.execute(() -> {
//            Scoreboard scoreboard = player.server.getScoreboard();
//            Objective livesObj = scoreboard.getObjective(player.getScoreboardName() + "_lives");
//            if (livesObj == null) return;
//            int lives = scoreboard.getOrCreatePlayerScore(player, livesObj).get();
//            Objective displayObj = scoreboard.getObjective("player_display");
//            if (displayObj != null) {
//                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
//                scoreboard.removeObjective(displayObj);
//            }
//            displayObj = scoreboard.addObjective(
//                    "player_display",
//                    ObjectiveCriteria.DUMMY,
//                    literal("§a❤ Lives ❤"), //title
//                    ObjectiveCriteria.RenderType.INTEGER,
//                    false,
//                    null
//            );
//            String heartColor = getHeartColor(player);
//            displayObj = scoreboard.getObjective("player_display");
//            if (displayObj != null) {
//                String hearts = heartColor + "❤".repeat(Math.max(0, lives));
//                scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(hearts), displayObj).set(lives);
//
//                scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, displayObj);
//                player.connection.send(new ClientboundSetDisplayObjectivePacket(
//                        DisplaySlot.SIDEBAR,
//                        displayObj
//                ));
//                checkPlayerLives(player);
//                setTeam(player);
//            }
//        });
//    }


    // get life function useful for returning the life int
    public static int getPlayerLives(ServerPlayer player) {
        Scoreboard scoreboard = player.server.getScoreboard();
        Objective livesObj = scoreboard.getObjective(player.getScoreboardName() + "_lives");
        if (livesObj != null) {
            return scoreboard.getOrCreatePlayerScore(player, livesObj).get();
        }
        return 0; // Default if no objective found, if a player somwhow doesnt get scores set to them theyll just become spectator
    }
    // changes its own color var to be used in other functions based on player lives
    private static String getHeartColor(ServerPlayer player) {
        int lives = getPlayerLives(player);
        if (lives <= 1) {
            return "§c"; // Red if 1 or fewer lives
        } else if (lives <= 2) {
            return "§e"; // Yellow if 2 lives
        } else {
            return "§a"; // Green if 3+ lives
        }
    }

    // when player lives hits 0 player is made into spectator
    private static boolean isProcessing = false;  // Flag to prevent recursion below
    public static void checkPlayerLives(ServerPlayer player) {
        // Skip if already processing or in creative mode
        if (isProcessing || player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }
        try {
            isProcessing = true;
            int lives = getPlayerLives(player);
            GameType currentMode = player.gameMode.getGameModeForPlayer();
            // Check for exactly 0 lives (or less than 1 dunno and dont want to know what happens with negatives lol)
            if (lives < 1) {
                if (currentMode != GameType.SPECTATOR) {
                    player.setGameMode(GameType.SPECTATOR);
                    player.sendSystemMessage(literal("§cYou're out of lives!"));
                }
            }
            // Check if they have lives and are in spectator
            else if (currentMode == GameType.SPECTATOR) {
                player.setGameMode(GameType.SURVIVAL);
            }
        } finally {
            isProcessing = false;
        }
    }



//    @SubscribeEvent
//    public void onRegisterCommands(RegisterCommandsEvent event) {
//        ResetCommand.register(event.getDispatcher());

    //    @Mod.EventBusSubscriber
//    public static class CommandHandler {
//        @SubscribeEvent
//        public static void onCommandRegister(RegisterCommandsEvent event) {
//            event.getDispatcher().register(
//                    Commands.literal("teamtest")
//                            .then(Commands.literal("create")
//                                    .executes(ctx -> {
//                                        ctx.getSource().getScoreboard().addPlayerTeam("testteam");
//                                        return 1;
//                                    })
//                            )
//                            .then(Commands.literal("join")
//                                    .executes(ctx -> {
//                                        Scoreboard sb = ctx.getSource().getScoreboard();
//                                        PlayerTeam team = sb.getPlayerTeam("testteam");
//                                        if (team != null) {
//                                            sb.addPlayerToTeam(
//                                                    ctx.getSource().getPlayer().getScoreboardName(),
//                                                    team
//                                            );
//                                        }
//                                        return 1;
//                                    })
//                            )
//            );
//        }
//    }
}


