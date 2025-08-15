//package com.Tot.craftedsurvivors;
//
//import com.mojang.brigadier.Command;
//import com.mojang.brigadier.CommandDispatcher;
//import com.mojang.brigadier.arguments.StringArgumentType;
//import net.minecraft.commands.CommandSourceStack;
//import net.minecraft.commands.Commands;
//import net.minecraft.network.chat.Component;
//import net.minecraft.server.MinecraftServer;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraftforge.server.ServerLifecycleHooks;
//
//import java.util.Collection;
//
//public class ResetCommand {
//
//    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
//        dispatcher.register(
//                Commands.literal("craftedsurvivors")
//                        .requires(source -> source.hasPermission(2)) // Only OPs
//                        .then(Commands.literal("reset")
//                                .executes(context -> {
//                                    resetAllPlayers(context.getSource());
//                                    return Command.SINGLE_SUCCESS;
//                                })
//                                .then(Commands.argument("player", StringArgumentType.word())
//                                        .executes(context -> {
//                                            String playerName = StringArgumentType.getString(context, "player");
//                                            resetSinglePlayer(context.getSource(), playerName);
//                                            return Command.SINGLE_SUCCESS;
//                                        })
//                                )
//                        )
//        );
//    }
//
//    private static void resetAllPlayers(CommandSourceStack source) {
//        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//        if (server == null) {
//            source.sendFailure(Component.literal("Server not found!"));
//            return;
//        }
//
//        Collection<ServerPlayer> players = server.getPlayerList().getPlayers();
//        for (ServerPlayer player : players) {
//            ScoreboardHelper.setPlayerLife(player, 1);
//            ScoreboardHelper.setPlayerCriminal(player, 0);
//            ScoreboardHelper.assignPlayerToTeam(player, 1, 0);
//            player.sendSystemMessage(Component.literal("§aYour Crafted Survivors stats have been reset."));
//        }
//        source.sendSuccess(() -> Component.literal("All player's Crafted Survivors stats have been reset."), true);
//    }
//
//    private static void resetSinglePlayer(CommandSourceStack source, String playerName) {
//        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//        if (server == null) {
//            source.sendFailure(Component.literal("Server not found!"));
//            return;
//        }
//
//        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
//        if (player == null) {
//            source.sendFailure(Component.literal("Player " + playerName + " not found!"));
//            return;
//        }
//
//        ScoreboardHelper.setPlayerLife(player, 1);
//        ScoreboardHelper.setPlayerCriminal(player, 0);
//        ScoreboardHelper.assignPlayerToTeam(player, 1, 0);
//        player.sendSystemMessage(Component.literal("§aYour Crafted Survivors stats have been reset."));
//        source.sendSuccess(() -> Component.literal("Player " + playerName + "'s Crafted Survivors stats have been reset."), true);
//    }
//}
