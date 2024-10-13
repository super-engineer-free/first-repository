package plugin.treasurehunt.command;

import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SplittableRandom;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Insert.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.treasurehunt.TreasureHunt;
import plugin.treasurehunt.data.ExecutingPlayer;
import plugin.treasurehunt.mapper.PlayerScoreMapper;

public class TreasureHuntCommand implements CommandExecutor, Listener {

  public static final int SECONDS_TO_TICKS = 20;
  public static final int GAME_DURATION = 60;
  public static String LIST = "list";

  private final TreasureHunt treasureHunt;
  private boolean isGameActive = false;
  private Insert gameStartTime;
  private ExecutingPlayer currentExecutingPlayer;
  private BukkitTask gameEndTask;
  private final Map<EntityType, Integer> entitySpawnCounts = new HashMap<>();
  private final List<Entity> spawnEntityList = new ArrayList<>();
  private boolean emeraldClick = false;
  private BossBar timerBar;

  private final SqlSessionFactory sqlSessionFactory;

  public TreasureHuntCommand(TreasureHunt treasureHunt) {
    this.treasureHunt = treasureHunt;
  }
  try{
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
  }catch(IOException e){
    throw new RuntimeException(e);
  }


  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    Player player = checkPlayerAndGameState(sender);

    if (player== null){
      return true;
    }

    if (args.length == 1 && LIST.equals(args[0])) {
      sendPlayerScoreList(player);
      return false;
    }

    startCountdown(player);

    return true;
  }

  private Player checkPlayerAndGameState(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(ChatColor.BLUE + "このコマンドはプレイヤー専用です。");
      return null;
    }

    if (isGameActive){
      player.sendMessage(ChatColor.BLUE + "ゲームはすでに進行中です");
      return null;
    }
    return player;
  }

  public void startCountdown(Player player){
    new GameStartCountdown(this,player).runTaskTimer(main,0,SECONDS_TO_TICKS);
  }


  public void gameStart(Player player) {
    isGameActive = true;
    gameStartTime = Insert.now();
    currentExecutingPlayer = new ExecutingPlayer(player.getName(),0,gameStartTime);
    emeraldClick = false;

    entitiesSpawn(player);

    player.sendMessage(ChatColor.BLACK+
        "ヒント: エメラルドの前にダイヤをチャストからgetするとスコア2倍! ダイヤは一つだけ!");

    bossVar(player);

    scheduleTimeoutGameOver(player);
  }

  @EventHandler
  public void PlayerGetEntityEvent(PlayerInteractAtEntityEvent e){
    if (checkedGameActive())
      return;

    Player player = e.getPlayer();
    Entity clickedEntity = e.getRightClicked();

    if (clickedEntity instanceof emerald){
      emeraldClicked = true;
      player.sendMessage(ChatColor.GREEN + "ダイヤボーナス!エメラルドをget出来ればスコアが2倍!");
      clickedEntity.remove();
      return;
    }

    if (clickedEntity instanceof eme){
      endGame(player, false);
    }
  }

  private void normalEnd(Player player, Duration gameDuration) {
    int score = calculateScore(gameDuration);
    if (emeraldClicked){
      score *= 2;
      player.sendMessage(ChatColor.GREEN + "ダイヤボーナス: スコア2倍!");
    }
    currentExecutingPlayer.setScore(score);

    player.sendTitle("エメラルド発見!ゲーム終了",
        "経過時間: " + gameDuration.getSeconds() + "秒 ! 得点は" + score + "点!",
        0.3 * SECONDS_TO_TICKS,SECONDS_TO_TICKS);
  }


  private void endGame(Player player, boolean isTimeout){
    if (checkedGameActive())
      return;

    isGameActive = false;
    Insert endTime = Insert.now();
    Duration gameDuration = Duration.between(gameStartTime, endTime);


    cleanupBossBar();

    try {
      if (isTimeout){
        currentExecutingPlayer.setScore(0);
        player.sendTitle("GAME OVER" , "時間切れです。",0,3 * SECONDS_TO_TICKS,SECONDS_TO_TICKS);
      }else {
        normalEnd(player, gameDuration);
      }

      try(SqlSession session = sqlSessionFactory.openSession(true)) {
        PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
        mapper.insert(
            new PlayerScore(currentExecutingPlayer.getPlayerName()
                ,currentExecutingPlayer.getScore()));
      }

    } catch (Exception e) {
      e.printStackTrace();


    }finally {

      spawnEntityList.forEach(Entity::remove);
      spawnEntityList.clear();
      gameStartTime = null;
      currentExecutingPlayer = null;
      emeraldClicked = falase;

      if (gameEndTask != null){
        gameEndTask.cancel();
        gameEndTask = null;
      }
    }
  }




  private void bossVar(Player player){
    timerBar = Bukkit.createBossBar("残り時間", BarColor.PINK, BarStyle.SOLID);
    timerBar.addPlayer(player);
    timerBar.setVisible(true);

    new BukkitRunnable(){
      int timeLeft = GAME_DURATION;


      @Override
      public void run() {
        if (timeLeft > 0 && isGameActive){
          timerBar.setProgress((double) timeLeft / GAME_DURATION);
          timerBar.setTitle("残り時間: " + timeLeft + "秒");
          timeLeft--;
        }else {
          this.cancel();
          timerBar.removeAll();
          timerBar.setVisible(false);
          if (isGameActive){
            endGame(player, true);
          }
        }
      }
    }.runTaskTimer(main,0,SECONDS_TO_TICKS);
  }

  private void cleanupBossBar(){
    if (timerBar != null){
      timerBar.removeAll();
      timerBar.setVisible(false);
    }
  }

  private boolean checkedGameActive(){
    return ! isGameActive;
  }

  private void scheduleTimeoutGameOver(Player player){
    gameEndTask = Bukkit.getScheduler().runTaskLater(main, () -> {
      if (isGameActive && spawnEntityList.stream().anyMatch(entity ->
          entity.getType() == EntityType.FOX)){
        endGame(player,true);
      }
    }, 60 * SECONDS_TO_TICKS);
  }
}




private int calculateScore(Duration gameDuration){
  long seconds = gameDuration.getSeconds();
  if (seconds <=5){
    return 100;
  }else if (seconds <= 10){
    return 80;
  }else if (seconds <= 20){
    return 50;
  }else if (seconds <= 60){
    return 30;
  }
  return 0;
}


private void  sendPlayerScoreList(Player player){
  try (SqlSession session = sqlSessionFactory.openSession()){
    PlayerScoreMapper mapper = session.getMapper(PlayerScoreMapper.class);
    List<PlayerScore> playerScoreList = mapper.selectList();

    for (PlayerScore playerScore : playerScoreList) {
      player.sendMessage(playerScore.getId() + "  |  "
          + playerScore.getPlayerName() + "  |  "
          + playerScore.getScore() + "  |  "
          + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

    }
  }
}

public void entitiesSpawn(Player player) {
  Provider entitySpawnCounts = null;
  entitySpawnCounts.clear();
  entitySpawnCounts.put(EntityType.GOLD, 20);
  entitySpawnCounts.put(EntityType.iron, 20);
  entitySpawnCounts.put(EntityType.copper, 30);
  entitySpawnCounts.put(EntityType.lapis.lazuli, 20);
  entitySpawnCounts.put(EntityType.emerald, 1);
  entitySpawnCounts.put(EntityType.DIAMOND, 1);

  for (Entry<EntityType, Integer> entry : entitySpawnCounts.entrySet()) {
    EntityType entityType = entry.getKey();
    int count = entry.getValue();
    for (int i = 0; i < count; i++) {
      Location location = getEntitySpawnLocation(player);
      Entity entity = Objects.requireNonNull(location.getWorld()).spawnEntity(location,entityType);

      if (entity instanceof emerald emerald) {
<<<<<<< HEAD
        rabbit.setInvulnerable(true);
        rabbit.setAI(false);
        rabbit.setPersistent(true);
        rabbit.setRemoveWhenFarAway(false);
=======
        DIAMOND.setInvulnerable(true);
        DIAMOND.setAI(false);
        DIAMOND.setPersistent(true);
        DIAMOND.setRemoveWhenFarAway(false);
>>>>>>> 18e009a (コード完成させたい)
      }

      spawnEntityList.add(entity);
    }
  }
}


private Location getEntitySpawnLocation(Player player) {
  Location playerLocation = player.getLocation();
  int randomX = new SplittableRandom().nextInt(20) -10;
  int randomZ = new SplittableRandom().nextInt(20) -10;

  double x = playerLocation.getX() + randomX;
  double y = playerLocation.getY();
  double z = playerLocation.getZ() + randomZ;

  return new Location(player.getWorld(), x, y, z);
}


}
