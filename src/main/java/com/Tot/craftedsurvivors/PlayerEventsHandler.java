package com.Tot.craftedsurvivors;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.Tot.craftedsurvivors.CraftedSurvivorsMod.getPlayerLives;
import static net.minecraft.network.chat.Component.literal;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import java.util.Objects;

public class PlayerEventsHandler {

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

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Scoreboard scoreboard = player.getScoreboard();
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (currentTeam != null && "criminalteam".equals(currentTeam.getName())) {
            setAtRiskTeam(player);
        }
    }



    //function to set player to team criminal

//    @SubscribeEvent
//    public void onPlayerDeath(LivingDeathEvent event) {
//        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) return;
//
//        int currentLife = ScoreboardHelper.getPlayerLife(deadPlayer);
//
//        // inv thing
//        if (currentLife == 1 || currentLife == 2) {
//            dropHalfInventory(deadPlayer);
//        } else if (currentLife == 3) {
//            dropFullInventory(deadPlayer);
//        }
//
//        // feedback last life
//        if (currentLife == 3) {
//            deadPlayer.sendSystemMessage(Component.literal("§cYou are in your final life! Eliminate or be eliminated!"));
//        }
//
//        // sussy criminal shit
//    }

//    private void dropHalfInventory(ServerPlayer player) {
//        var inventory = player.getInventory();
//        var random = player.level().random;
//        for (int i = 0; i < inventory.getContainerSize(); i++) {
//            ItemStack stack = inventory.getItem(i);
//            if (!stack.isEmpty() && random.nextBoolean()) {
//                player.drop(stack.copy(), true);
//                inventory.setItem(i, ItemStack.EMPTY);
//            }
//        }
//    }

//    private void dropFullInventory(ServerPlayer player) {
//        var inventory = player.getInventory();
//        for (int i = 0; i < inventory.getContainerSize(); i++) {
//            ItemStack stack = inventory.getItem(i);
//            if (!stack.isEmpty()) {
//                player.drop(stack.copy(), true);
//                inventory.setItem(i, ItemStack.EMPTY);
//            }
//        }
//    }
}
