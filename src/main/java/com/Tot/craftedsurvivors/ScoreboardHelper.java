package com.Tot.craftedsurvivors;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import static com.Tot.craftedsurvivors.CraftedSurvivorsMod.checkPlayerLives;
import static com.Tot.craftedsurvivors.CraftedSurvivorsMod.updatePlayerScoreboard;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.server.ServerStartingEvent;
import java.util.Objects;

import static net.minecraft.network.chat.Component.literal;

public class ScoreboardHelper {

// we need to setup teams somewhere somehow doing this in main
// we want that as a player looses life their team changes
// we want that as a player looses life their chat text changes based on teams
    // we want to setup nametag color
// Add criminal system
//
 // You have 3 lives, not meant to kill player
    // if kill on phase 1 player = criminal
    // when criminal is killed or dies lost role and becomes AtRisk
    // keep in mind this is only for the killer team set
    // on player kill, (return) if killed by player (return), if player that kills is Healthy (else ), and target is healthy
    // become criminal
    // criminal makes you glow, loose role criminal on death criminal is purple
    // if player is criminal and kills healthy again stay criminal

    // make code to spawn warden on everyone every 5 minutes obfuscate it and make read only

    // if

    // intialize teams
    // make functions to set player to each team that can be called on
    // make healthy / criminal vs healthy logic
    // make logic where if criminal dies they loose role and set to yellow

    // think about half inventory logic








    //anytime the score is changed the display is updated
    @SubscribeEvent
    public void onScoreUpdated(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String objectiveName = player.getScoreboardName() + "_lives";
            player.server.execute(() -> {
                Scoreboard scoreboard = player.server.getScoreboard();
                Objective livesObj = scoreboard.getObjective(objectiveName);

                if (livesObj != null) {
                    // Get current lives value
                    int lives = scoreboard.getOrCreatePlayerScore(player, livesObj).get();

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





// the fuck is the actual point of this section??
//    public static Objective getObjective(MinecraftServer server, String name) {
//        if (server == null || name == null) {
//            throw new IllegalArgumentException("Server and name cannot be null");
//        }
//        // Validate objective name length (max 16 characters in Minecraft)
//        if (name.length() > 16) {
//            throw new IllegalArgumentException("Objective name cannot be longer than 16 characters");
//        }
//        Scoreboard scoreboard = server.getScoreboard();
//        if (scoreboard == null) {
//            throw new IllegalStateException("Could not get scoreboard from server");
//        }
//        //this is always false so its pointless..
//        Objective obj = scoreboard.getObjective(name);
//        if (obj == null) {
//            obj = scoreboard.addObjective(
//                    name,
//                    ObjectiveCriteria.DUMMY,
//                    Component.literal(name),
//                    ObjectiveCriteria.RenderType.INTEGER,
//                    false,
//                    null
//            );
//        }
//        return obj; //for what purpose ??
//    }

// Get criminal score
//    public static int getPlayerCriminal(ServerPlayer player) {
//        Objective obj = getObjective(player.getServer(), "criminal");
//        Score score = (Score) (Score) player.getServer().getScoreboard()
//                .getOrCreatePlayerScore(ScoreHolder.forNameOnly(player.getScoreboardName()), obj);
//        return score.value();
//    }

// Set criminal score
//    public static void setPlayerCriminal(ServerPlayer player, int value) {
//        Objective obj = getObjective(player.getServer(), "criminal");
//        Score score = (Score) player.getServer().getScoreboard()
//                .getOrCreatePlayerScore(ScoreHolder.forNameOnly(player.getScoreboardName()), obj);
//        score.value();
//    }

// Assign player to a colored team based on phase
// Phase ??
//    public static void assignPlayerToTeam(ServerPlayer player, int lifePhase, int criminalStatus) {
//        Scoreboard scoreboard = player.getServer().getScoreboard();
//        //possible null pointer exception.
//        // Remove from current team if any
//        PlayerTeam currentTeam = scoreboard.getPlayersTeam(player.getScoreboardName());
//        if (currentTeam != null) {
//            scoreboard.removePlayerFromTeam(player.getScoreboardName(), currentTeam);
//        }
//        String teamName = getTeamName(lifePhase, criminalStatus);
//        // Get or create team by name
//        PlayerTeam team = scoreboard.getPlayersTeam(teamName);
//        if (team == null) {
//            team = scoreboard.addPlayerTeam(teamName);
//        }
//        // Set team color
//        team.setColor(resolveColor(teamName));
//        // Add player to team
//        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
//    }

// Determine color based on team
// what?
//    private static ChatFormatting resolveColor(String teamName) {
//        return switch (teamName) {
//            case "green" -> ChatFormatting.GREEN;
//            case "purple" -> ChatFormatting.DARK_PURPLE;
//            case "yellow" -> ChatFormatting.YELLOW;
//            case "red" -> ChatFormatting.RED;
//            default -> ChatFormatting.WHITE;
//        };
//    }

}
