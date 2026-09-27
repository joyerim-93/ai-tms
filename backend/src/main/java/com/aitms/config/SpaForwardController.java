package com.aitms.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 배포용 단일 jar 구성(프론트 빌드 결과를 resources/static 에 패키징) 전용.
 * Vue Router가 history 모드라 /cycles/1 같은 경로를 새로고침·직접 진입하면 정적 파일이 없어 404가 남 —
 * 확장자가 없는(=API 도, 정적 자산도 아닌) 요청은 전부 index.html 로 포워드해서 SPA가 라우팅을 처리하게 함.
 * 실제 경로 보호는 서버가 아니라 (1) /api/** 인증(SecurityConfig) (2) 라우터 가드(router/index.js)가 담당 —
 * 여기서는 정적 껍데기만 내려주면 됨. 로컬 개발(프론트는 Vite, 백엔드엔 static 없음)에서는 이 컨트롤러가 매칭될 일이 없음.
 */
@Controller
public class SpaForwardController {

    @RequestMapping(value = {"/{path:[^.]*}", "/**/{path:[^.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
