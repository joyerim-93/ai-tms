package com.aitms.domain.user;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    Optional<User> findByLoginId(String loginId);

    /** 비밀번호가 아직 없는 로그인 가능 사용자(임시 guest- 계정 제외) — 기동 시 초기 비밀번호 설정 대상 */
    List<User> findWithoutPassword();

    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
