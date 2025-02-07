package tauri.dev.jsg.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import tauri.dev.jsg.config.craftings.CraftingConfig;

import javax.annotation.Nonnull;

public class CommandReloadCraftingConfigs extends CommandBase {
    @Nonnull
    @Override
    public String getName() {
        return "jsgconfigsreload";
    }

    @Nonnull
    @Override
    public String getUsage(@Nonnull ICommandSender sender) {
        return "/jsgconfigsreload";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        CraftingConfig.reload();
        notifyCommandListener(sender, this, "Crafting configs reloaded!");
    }

}
