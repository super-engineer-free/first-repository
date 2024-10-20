package plugin.treasurehunt;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this,this);
    TreasureHuntCommand treasureHuntCommand = new TreasureHuntCommand(this);
    getCommand("TreasureHunt").setExecutor(treasureHuntCommand);
    getServer().getPluginManager().registerEvents(treasureHuntCommand, this);
  }
}