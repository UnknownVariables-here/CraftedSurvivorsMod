package com.tot.craftedsurvivors;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import static net.minecraft.network.chat.Component.literal;


public class CommonCraftedSurvivorsMod {
    public static void init(ServerPlayer player) {
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
            // Force display update NEEDS FIXING?
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, displayObj);
            player.connection.send(new ClientboundSetDisplayObjectivePacket(
                    DisplaySlot.SIDEBAR,
                    displayObj
            ));
        });
    }
    public static String getHeartColor(ServerPlayer player) {
        int lives = getPlayerLives(player);
        if (lives <= 1) {
            return "§c"; // Red if 1 or fewer lives
        } else if (lives <= 2) {
            return "§e"; // Yellow if 2 lives
        } else {
            return "§a"; // Green if 3+ lives
        }
    }
    public static int getPlayerLives(ServerPlayer player) {
        Scoreboard scoreboard = player.server.getScoreboard();
        Objective livesObj = scoreboard.getObjective(player.getScoreboardName() + "_lives");
        if (livesObj != null) {
            return scoreboard.getOrCreatePlayerScore(player, livesObj).get();
        }
        return 0; // Default if no objective found, if a player somwhow doesnt get scores set to them theyll just become spectator
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
    //function to set player to team feralteam
    public static void setFeralTeam(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        scoreboard.removePlayerFromTeam(player.getScoreboardName());
        PlayerTeam feralTeam = scoreboard.getPlayerTeam("feralteam");
        if (feralTeam != null) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), feralTeam);
        }
    }
    //function to set player to team atriskteam
    public static void setAtRiskTeam(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        scoreboard.removePlayerFromTeam(player.getScoreboardName());
        PlayerTeam atriskTeam = scoreboard.getPlayerTeam("atriskteam");
        if (atriskTeam != null) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), atriskTeam);
        }
    }
    //function to set player to team healthyteam
    public static void setHealthyTeam(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        scoreboard.removePlayerFromTeam(player.getScoreboardName());
        PlayerTeam healthyTeam = scoreboard.getPlayerTeam("healthyteam");
        if (healthyTeam != null) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), healthyTeam);
        }
    }
    //function to set player to team criminalteam
    public static void setCriminalTeam(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        scoreboard.removePlayerFromTeam(player.getScoreboardName());
        PlayerTeam criminalTeam = scoreboard.getPlayerTeam("criminalteam");
        if (criminalTeam != null) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), criminalTeam);
        }
    }

    public static void setTeam(ServerPlayer player) {
        if(player == null) return;

        int lives = getPlayerLives(player);
        if (lives <= 1) {
            setFeralTeam(player);
        } else if (lives == 2) {
            setAtRiskTeam(player);
        } else {
            setHealthyTeam(player);
        }
    }

}

