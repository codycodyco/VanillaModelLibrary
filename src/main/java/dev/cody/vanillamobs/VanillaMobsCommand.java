package dev.cody.vanillamobs;

import dev.cody.vanillamobs.api.ModelRecord;
import dev.cody.vanillamobs.assets.AssetBundle;
import dev.cody.vanillamobs.assets.AssetException;
import dev.cody.vanillamobs.assets.ValidatedAssets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;

public final class VanillaMobsCommand implements CommandExecutor, TabCompleter {

    private final VanillaModelLibraryPlugin plugin;

    public VanillaMobsCommand(VanillaModelLibraryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vanillamobs.admin")) {
            sender.sendMessage("You need vanillamobs.admin.");
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "status" -> status(sender);
            case "verify" -> verify(sender);
            case "reload" -> {
                plugin.reloadLibrary();
                status(sender);
            }
            case "selftest" -> {
                boolean ok = Selftest.run(plugin, sender);
                sender.sendMessage(ok ? "Selftest PASSED." : "Selftest FAILED; see console and selftest.json.");
            }
            case "items" -> items(sender);
            default -> sender.sendMessage("Usage: /vanillamobs <status|verify|reload|selftest|items>");
        }
        return true;
    }

    private void status(CommandSender sender) {
        ValidatedAssets assets = plugin.assets();
        sender.sendMessage("VanillaModelLibrary: ready=" + plugin.ready()
                + " asset=" + (plugin.assetVersion().isEmpty() ? "-" : plugin.assetVersion())
                + " minecraft=" + plugin.minecraftVersion()
                + " rpEngine=" + plugin.rpeVersion());
        sender.sendMessage("assets dir=" + plugin.assetsDirectory() + " content=" + plugin.contentRoot());
        if (assets != null) {
            sender.sendMessage("package=" + assets.root() + " files=" + assets.files().size()
                    + " catalog=" + assets.catalog().models().size() + " models");
        }
        if (!plugin.ready()) {
            sender.sendMessage("last error: " + plugin.lastError());
        }
    }

    private void verify(CommandSender sender) {
        try {
            Path root = AssetBundle.locate(plugin.assetsDirectory(),
                    plugin.getConfig().getString("assets.package", ""), plugin.getLogger());
            if (root == null) {
                sender.sendMessage("No package found in " + plugin.assetsDirectory());
                return;
            }
            ValidatedAssets validated = AssetBundle.validate(root, org.bukkit.Bukkit.getMinecraftVersion(),
                    plugin.getLogger());
            sender.sendMessage("Package OK: asset=" + validated.assetVersion()
                    + " minecraft=" + validated.catalog().minecraftVersion()
                    + " files=" + validated.files().size());
            sender.sendMessage("Models: " + validated.catalog().models().stream()
                    .map(model -> model.modelId() + (model.supported() ? "" : " (unsupported)"))
                    .collect(Collectors.joining(", ")));
        } catch (AssetException error) {
            sender.sendMessage("Package REJECTED: " + error.getMessage());
        }
    }

    private void items(CommandSender sender) {
        if (plugin.models().isEmpty()) {
            sender.sendMessage("No catalog loaded.");
            return;
        }
        for (ModelRecord model : plugin.models()) {
            String state = !model.supported() ? "unsupported"
                    : plugin.createItem(model.modelId()).map(stack -> "ITEM " + stack.getType()).orElse("MISSING");
            sender.sendMessage(model.modelId() + " [" + model.entityType() + "] -> " + state);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "verify", "reload", "selftest", "items").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
