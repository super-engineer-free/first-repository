package plugin.treasurehunt.command;

import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import plugin.treasurehunt.TreasureHunt;

public class TreasureHuntCommand implements Listener, CommandExecutor {


  private final TreasureHunt Treasurehunt;
  private Player player;
  private int score;

  public TreasureHuntCommand(TreasureHunt treasureHunt) {
    this.Treasurehunt = treasureHunt;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player){
      this.player = player;
      World world= player.getWorld();
      Location playerLocation = player.getLocation();
      double x = playerLocation.getX();
      double y = playerLocation.getY();
      double z = playerLocation.getZ();

     int random = new SplittableRandom().nextInt(20) - 10;

      FallingBlock fallingBlock = world.spawnFallingBlock(new Location(world, (x + random),y,(z + random)), Material.CHEST.createBlockData());
      fallingBlock.setDropItem(false);


// Add diamond to chest
      world.getBlockAt(fallingBlock.getLocation()).setType(Material.CHEST);
      BlockState state = world.getBlockAt(fallingBlock.getLocation()).getState();
      if (state instanceof Chest chest) {
        chest.getBlockInventory().addItem(new ItemStack(Material.DIAMOND));
      }

      // Add event listener for player picking up diamond
      Bukkit.getPluginManager().registerEvents(new Listener() {
        @EventHandler
        public void onPlayerPickupItem(EntityPickupItemEvent event) {
          if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            if (item.getType() == Material.DIAMOND) {
              // Add score to player
              // Example: player.sendMessage("You picked up a diamond! Score +1");
              // Implement your scoring logic here
            }
          }
        }
      }, Treasurehunt);


    }

    return false;
  }

  @EventHandler
  public void onEnemyDeath(EntityPickupItemEvent event){
          Player player = (Player) event.getEntity().getItemInUse();
          if (this.player.getName().equals(player.getName())){
            score +=10;
            player.sendMessage("ダイヤモンドget　現在のスコアは"+score+"点!");
          }
        }
  }




