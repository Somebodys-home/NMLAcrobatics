package io.github.NoOne.nMLAcrobatics;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetYawAndPitchCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /setyawandpitch <yaw> <pitch>");
            return true;
        }

        try {
            float yaw = Float.parseFloat(args[0]);
            float pitch = Float.parseFloat(args[1]);

            Location loc = player.getLocation();
            loc.setYaw(yaw);
            loc.setPitch(pitch);

            player.teleport(loc); // Teleports in place, just changes rotation
            player.sendMessage("§aYour yaw and pitch have been updated!");
        } catch (NumberFormatException e) {
            player.sendMessage("§cYaw and pitch must be numbers.");
        }

        return true;
    }
}
