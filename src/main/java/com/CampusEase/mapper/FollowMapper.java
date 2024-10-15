package com.CampusEase.mapper;

import com.CampusEase.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;

public interface FollowMapper extends BaseMapper<Follow> {

    @Delete("delete from tb_follow where user_id = #{userId} and follow_user_id = #{followUserId}")
    boolean delete(Long userId, Long followUserId);
}
