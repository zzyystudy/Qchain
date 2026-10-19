package com.zxyy.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxyy.user.domain.entity.Admin;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AdminMapper extends BaseMapper<Admin> {

    @Select("SELECT " +
            "DISTINCT m.`perms` " +
            "FROM " +
            "sys_user_role ur " +
            "LEFT JOIN `sys_role` r ON ur.`role_id` = r.`id` " +
            "LEFT JOIN `sys_role_menu` rm ON ur.`role_id` = rm.`role_id` " +
            "LEFT JOIN `sys_menu` m ON m.`id` = rm.`menu_id` " +
            "WHERE " +
            "user_id = #{userId} " +
            "AND r.`status` = 0 " +
            "AND m.`status` = 0")
    List<String> getPermissionList(@Param("userId") Long id);
}
