package com.backend.service;

import com.backend.config.CampusClubAgentPrompts;
import com.backend.entity.PromptTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemPromptProvider {

    public static final String SYSTEM_PROMPT_NAME = "campus-club-system";

    private final PromptTemplateService promptTemplateService;
    private volatile String cachedTemplate;

    public String getSystemPrompt() {
        if (cachedTemplate != null) {
            return cachedTemplate;
        }
        return loadSystemPrompt();
    }

    public synchronized String loadSystemPrompt() {
        cachedTemplate = promptTemplateService.getByName(SYSTEM_PROMPT_NAME)
                .map(PromptTemplate::getTemplate)
                .orElse(CampusClubAgentPrompts.SYSTEM_TEMPLATE);
        return cachedTemplate;
    }

    public void invalidateCache() {
        cachedTemplate = null;
    }
}
