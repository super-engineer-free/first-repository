package plugin.treasurehunt.command;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import plugin.treasurehunt.TreasureHuntCommand;

public class GameStartCountdown extends BukkitRunnable {
  private int nowTime;
  private final Player player;
  private final TreasureHuntCommand treasureHuntCommand;

  public GameStartCountdown(TreasureHuntCommand treasureHuntCommand, Player player) {
    this.treasureHuntCommand = treasureHuntCommand;
    this.nowTime = 10;  // タイトル表示５秒＋カウントダウン５秒
    this.player = player;
  }


  @Override
  public void run() {
    if (nowTime <= 0) {
      player.sendTitle(ChatColor.BLACK + "START!!", "", 0, TreasureHuntCommand.SECONDS_TO_TICKS, 0);
      cancel();
      treasureHuntCommand.gameStart(player);
      return;
    } else if (nowTime == 10) {
      player.sendTitle("エメラルドを探せ",
          "1分以内にチャストから見つけて。",
          0, 5 * TreasureHuntCommand.SECONDS_TO_TICKS, TreasureHuntCommand.SECONDS_TO_TICKS);
    } else if(nowTime <= 5) {
      player.sendTitle(ChatColor.BLACK + "開始まであと" + nowTime + "秒",
          "ダイヤをチャストから見つけれたらボーナススコア！",
          0, TreasureHuntCommand.SECONDS_TO_TICKS,0);
    }
    nowTime--;
  }
}
