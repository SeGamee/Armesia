package fr.segame.armesia.commands;

import fr.segame.armesia.Main;
import fr.segame.armesia.listeners.InvSeeListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InvSeeCommand implements CommandExecutor {

    /**
     * Layout d'un inventaire 54 slots pour /invsee :
     * <pre>
     *  Slots 0-8   → rangée du bas   (hotbar cible  : inv slots 0-8)
     *  Slots 9-17  → rangée 2        (inv slots 9-17)
     *  Slots 18-26 → rangée 3        (inv slots 18-26)
     *  Slots 27-35 → rangée 4        (inv slots 27-35)
     *  Slots 36-39 → armure          (inv slots 36-39 : boots, leggings, chestplate, helmet)
     *  Slots 45    → offhand         (inv slot 40)
     *  Slots 40-44 / 46-53 → vides (séparateurs)
     * </pre>
     */

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        }

        if (!Main.hasGroupPermission(viewer, "armesia.invsee")) {
            viewer.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length != 1) {
            viewer.sendMessage("§cUsage : §f/invsee <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            viewer.sendMessage("§cCe joueur n'est pas connecté.");
            return true;
        }

        if (target.equals(viewer)) {
            viewer.sendMessage("§cVous ne pouvez pas voir votre propre inventaire avec cette commande.");
            return true;
        }

        boolean canTake = Main.hasGroupPermission(viewer, "armesia.invsee.take");

        // Création de l'inventaire GUI (54 slots)
        Inventory gui = Bukkit.createInventory(null, 54,
                "§8Inventaire de §f" + target.getName());

        // Remplissage : inventaire principal (slots 0-35)
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, target.getInventory().getItem(i));
        }
        // Armure (slots 36-39 de l'inventaire → slots 36-39 du GUI)
        ItemStack[] armor = target.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            gui.setItem(36 + i, armor[i]);
        }
        // Offhand (slot 40 inventaire → slot 44 GUI)
        gui.setItem(44, target.getInventory().getItemInOffHand());

        // Enregistrement dans le listener pour le suivi
        InvSeeListener.open(viewer, target, gui, canTake);

        viewer.openInventory(gui);

        if (canTake) {
            viewer.sendMessage("§aVous consultez l'inventaire de §f" + target.getName()
                    + " §a(vous pouvez prendre des objets).");
        } else {
            viewer.sendMessage("§aVous consultez l'inventaire de §f" + target.getName() + "§a.");
        }
        return true;
    }
}

