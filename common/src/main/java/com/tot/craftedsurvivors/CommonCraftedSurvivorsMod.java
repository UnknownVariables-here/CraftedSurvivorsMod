package com.tot.craftedsurvivors;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
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

            // Force display update
            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, displayObj);
            player.connection.send(new ClientboundSetDisplayObjectivePacket(
                    DisplaySlot.SIDEBAR,
                    displayObj


            ));
        };
    }
}

