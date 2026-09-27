package com.aitms.domain.user;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /** 이름이 같은 사용자 중 가장 먼저 만들어진 id */
    Optional<Long> findIdByName(String name);

    void insertGuest(@Param("loginId") String loginId, @Param("name") String name);

    Optional<Long> findIdByLoginId(String loginId);
}
