package com.backend.service;
import com.backend.client.RegistrationClient;
import com.backend.client.ActivityClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.contract.ActivityDto;
import com.backend.contract.RegistrationDto;
import com.backend.exception.UnauthorizedException;
import com.backend.service.impl.AiServiceImpl;
import com.backend.util.AiRequestContext;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock
    private RegistrationClient registrationService;

    @Mock
    private ActivityClient activityService;

    @InjectMocks
    private AiServiceImpl aiService;

    @AfterEach
    void tearDown() {
        AiRequestContext.clear();
    }

    @Test
    void createRegistration_usesAuthenticatedUserFromContext() {
        AiRequestContext.set(new AiRequestContext.Holder(7L, "alice123", 1));

        ActivityDto activity = new ActivityDto();
        activity.setId(10L);
        when(activityService.getById(10L)).thenReturn(activity);

        RegistrationDto saved = new RegistrationDto();
        saved.setId(99L);
        when(registrationService.createRegistration(7L, 10L, "普通票")).thenReturn(saved);

        RegistrationDto result = aiService.createRegistration(10L, null, "普通票");

        assertEquals(99L, result.getId());
        verify(registrationService).createRegistration(7L, 10L, "普通票");
    }

    @Test
    void createRegistration_requiresLogin() {
        assertThrows(UnauthorizedException.class, () -> aiService.createRegistration(10L, null, null));
    }

    @Test
    void createRegistration_resolvesActivityByCode() {
        AiRequestContext.set(new AiRequestContext.Holder(7L, "alice123", 1));

        ActivityDto activity = new ActivityDto();
        activity.setId(10L);
        when(activityService.getByActivityCode("ACT-ALGO-001")).thenReturn(activity);
        when(registrationService.createRegistration(eq(7L), eq(10L), eq(null))).thenReturn(new RegistrationDto());

        aiService.createRegistration(null, "ACT-ALGO-001", null);

        verify(registrationService).createRegistration(7L, 10L, null);
    }
}
