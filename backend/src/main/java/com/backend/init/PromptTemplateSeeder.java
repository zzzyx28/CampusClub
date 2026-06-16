package com.backend.init;

import com.backend.config.CampusClubAgentPrompts;
import com.backend.service.PromptTemplateService;
import com.backend.service.SystemPromptProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PromptTemplateSeeder {

    private final PromptTemplateService promptTemplateService;

    @Bean
    ApplicationRunner seedSystemPrompt() {
        return args -> {
            if (promptTemplateService.getByName(SystemPromptProvider.SYSTEM_PROMPT_NAME).isEmpty()) {
                promptTemplateService.createTemplate(
                        SystemPromptProvider.SYSTEM_PROMPT_NAME,
                        CampusClubAgentPrompts.SYSTEM_TEMPLATE,
                        "CampusClub AI 系统提示词");
            }
        };
    }
}
