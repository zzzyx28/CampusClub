package com.backend.service.impl;

import com.backend.dao.mongo.PromptTemplateRepository;
import com.backend.entity.PromptTemplate;
import com.backend.service.PromptTemplateService;
import com.backend.service.SystemPromptProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final PromptTemplateRepository promptDao;
    private final SystemPromptProvider systemPromptProvider;

    public PromptTemplateServiceImpl(PromptTemplateRepository promptDao,
                                     @Lazy SystemPromptProvider systemPromptProvider) {
        this.promptDao = promptDao;
        this.systemPromptProvider = systemPromptProvider;
    }

    @Override
    public PromptTemplate createTemplate(String name, String template, String description) {
        PromptTemplate pt = new PromptTemplate();
        pt.setName(name);
        pt.setTemplate(template);
        pt.setDescription(description);
        pt.setUpdatedAt(new Date());
        PromptTemplate saved = promptDao.save(pt);
        if (SystemPromptProvider.SYSTEM_PROMPT_NAME.equals(name)) {
            systemPromptProvider.invalidateCache();
        }
        return saved;
    }

    @Override
    public Optional<PromptTemplate> getByName(String name) {
        return promptDao.findByName(name);
    }

    @Override
    public List<PromptTemplate> listAll() {
        return promptDao.findAll();
    }

    @Override
    public void updateTemplate(String id, PromptTemplate newTemplate) {
        PromptTemplate pt = promptDao.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在"));
        pt.setName(newTemplate.getName());
        pt.setDescription(newTemplate.getDescription());
        pt.setTemplate(newTemplate.getTemplate());
        pt.setUpdatedAt(new Date());
        promptDao.save(pt);
        if (SystemPromptProvider.SYSTEM_PROMPT_NAME.equals(pt.getName())
                || SystemPromptProvider.SYSTEM_PROMPT_NAME.equals(newTemplate.getName())) {
            systemPromptProvider.invalidateCache();
        }
    }

    @Override
    public void deleteTemplate(String id) {
        promptDao.findById(id).ifPresent(pt -> {
            if (SystemPromptProvider.SYSTEM_PROMPT_NAME.equals(pt.getName())) {
                systemPromptProvider.invalidateCache();
            }
            promptDao.deleteById(id);
        });
    }
}
