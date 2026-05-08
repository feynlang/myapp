package com.example.myapp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.myapp.question.QuestionService;

@SpringBootTest
class SimpleCrudPracticeApplicationTests {

	@Autowired
	private QuestionService questionService;

	@Test
	void testJpa() {
		for(int i=1; i<=300; i++){
			String subject=String.format("테스트 데이터입니다:[%03d]", i);
			String content="내용없음";
			this.questionService.create(subject, content);
		}

	}

	// CI 실패 감지 검증용 임시 테스트 — 확인 후 되돌릴 것
	@Test
	void cicdFailureProbe() {
		Assertions.fail("의도된 실패: CI 파이프라인이 빌드 실패를 제대로 감지하는지 확인");
	}
}