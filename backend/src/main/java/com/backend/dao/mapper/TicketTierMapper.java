package com.backend.dao.mapper;

import com.backend.entity.TicketTier;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TicketTierMapper extends BaseMapper<TicketTier> {

    @Select("SELECT * FROM ticket_tier WHERE ticket_code = #{ticketCode} LIMIT 1")
    TicketTier getByTicketCode(@Param("ticketCode") String ticketCode);

    @Select("SELECT * FROM ticket_tier WHERE activity_id = #{activityId} AND tier_name = #{tierName} LIMIT 1")
    TicketTier getByActivityIdAndTierName(@Param("activityId") Long activityId, @Param("tierName") String tierName);

    @Select("SELECT * FROM ticket_tier WHERE activity_id = #{activityId} AND is_default = 1 LIMIT 1")
    TicketTier getDefaultByActivityId(@Param("activityId") Long activityId);

    @Select("SELECT * FROM ticket_tier WHERE activity_id = #{activityId} ORDER BY is_default DESC, id ASC")
    List<TicketTier> listByActivityId(@Param("activityId") Long activityId);

    @Delete("DELETE FROM ticket_tier WHERE activity_id = #{activityId}")
    int deleteByActivityId(@Param("activityId") Long activityId);

    @Select("SELECT COUNT(*) FROM registration WHERE ticket_tier_id = #{tierId}")
    int countRegistrationRefsByTierId(@Param("tierId") Long tierId);

    @Update("UPDATE ticket_tier SET capacity = capacity - 1 WHERE id = #{id} AND capacity > 0")
    int decrementCapacityIfAvailable(@Param("id") Long id);

    @Update("UPDATE ticket_tier SET capacity = capacity + #{count} WHERE id = #{id}")
    int incrementCapacity(@Param("id") Long id, @Param("count") int count);
}
