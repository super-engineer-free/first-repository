package plugin.treasurehunt.command;

import java.util.List;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import plugin.treasurehunt.Main;
import org.bukkit.scheduler.BukkitTask;

public class TreasureHuntCommand implements Listener, CommandExecutor {


  private final Main main;
  private Player player;

  private int gameTime = 30; // 30 seconds

  public TreasureHuntCommand(Main main) {
    this.main = main;
  }


  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player) {
      this.player = player;

      BukkitTask countdownTask = Bukkit.getScheduler().runTaskTimer(main, new Runnable(){
        int count = 5;
        @Override
        public void run() {
          if (count > 0) {
            player.sendMessage("ゲーム開始まで " + count);
            count--;
            if (count == 0) {
              player.sendMessage("ゲーム開始ダイヤモンドを30秒以内に見つけて");
            }
          }
        }



      }, 0, 20); // 20 ticks = 1 second
      gameTime = 30;
      World world = player.getWorld();
      Location playerLocation = player.getLocation();
      double x = playerLocation.getX() ;
      double y = playerLocation.getY();
      double z = playerLocation.getZ() ;


      Bukkit.getScheduler().runTaskTimer(main, Runnable -> {
        if (gameTime <= 0) {
          Runnable.cancel();
          player.sendMessage("ゲームが終了しました。");
          Bukkit.getScheduler().cancelTasks(main);
          return;
        }
        world.spawnFallingBlock(playerLocation, Material.DIAMOND_BLOCK.createBlockData());
          gameTime -= 5;
      }, 0,5* 20); // 20 ticks = 1 second
      SplittableRandom random = new SplittableRandom();
      Location[] chestLocations = new Location[4];
      for (int i = 0; i < 4; i++) {
        int offsetX = random.nextInt(20) - 10;
        int offsetZ = random.nextInt(20) - 10;
        chestLocations[i] = new Location(world, x + offsetX, y, z + offsetZ);
        FallingBlock fallingBlock = world.spawnFallingBlock(chestLocations[i],
            Material.CHEST.createBlockData());
        fallingBlock.setDropItem(false);
      }

      // Add items to chests
      int diamondChestIndex = random.nextInt(4);
      int flagChestIndex;
      do {
        flagChestIndex = random.nextInt(4);
      } while (flagChestIndex == diamondChestIndex);

      for (int i = 0; i < 4; i++) {
        world.getBlockAt(chestLocations[i]).setType(Material.CHEST);
        BlockState state = world.getBlockAt(chestLocations[i]).getState();
        if (state instanceof Chest chest) {
          if (i == diamondChestIndex) {
            chest.getBlockInventory().addItem(new ItemStack(Material.DIAMOND));
          } else if (i == flagChestIndex) {
            chest.getBlockInventory().addItem(new ItemStack(Material.WHITE_BANNER));
          }
          }
        }

      // Add event listener for player picking up diamond
      Bukkit.getPluginManager().registerEvents(new Listener() {
        @EventHandler
        public void onPlayerPickupItem(EntityPickupItemEvent event) {
          if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            if (item.getType() == Material.DIAMOND) {
              player.sendMessage("ダイヤモンドを見つけたゲーム終了!! ");
              Bukkit.getScheduler().cancelTasks(main);
            }
          }
        }
      }, main);
    }

    return true;
  }

  private Location getEnemySpawnLocation(Player player, World world) {
    return null;
  }

  private BlockData getEnemy() {
    return Material.DIAMOND.createBlockData(); // Example enemy
  }
}




