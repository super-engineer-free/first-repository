package plugin.treasurehunt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface PlayerScoreMapper<PlayerScore> {


  @Select("select * from treasure_hunt")
  List<PlayerScore> selectList();


  @Insert("insert treasure_hunt (player_name, score, registered_at) values(#{playerName}, #{score}, now())")
  int insert(PlayerScore playerScore);

}
