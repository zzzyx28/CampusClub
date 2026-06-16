package com.backend.service;

import com.backend.dao.mapper.RegistrationMapper;
import com.backend.dao.mapper.TicketTierMapper;
import com.backend.entity.Activity;
import com.backend.entity.TicketTier;
import com.backend.entity.User;
import com.backend.service.impl.RegistrationServiceImpl;
import com.backend.util.EmailUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private ActivityService activityService;
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private TicketTierMapper ticketTierMapper;
    @Mock
    private EmailUtil emailUtil;

    @Spy
    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void createRegistration_rejectsWhenCapacityDecrementFails() {
        User user = new User();
        user.setId(1L);
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setStatus(1);
        TicketTier tier = new TicketTier();
        tier.setId(100L);
        tier.setCapacity(5);
        tier.setPrice(java.math.BigDecimal.ZERO);

        doReturn(0L).when(registrationService).count(any());
        when(userService.getById(1L)).thenReturn(user);
        when(activityService.getById(10L)).thenReturn(activity);
        when(ticketTierMapper.getDefaultByActivityId(10L)).thenReturn(tier);
        when(ticketTierMapper.decrementCapacityIfAvailable(100L)).thenReturn(0);

        assertThrows(RuntimeException.class, () ->
                registrationService.createRegistration(1L, 10L, null));
    }

    @Test
    void createRegistration_rejectsDuplicateRegistration() {
        User user = new User();
        user.setId(1L);
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setStatus(1);

        doReturn(1L).when(registrationService).count(any());
        when(userService.getById(1L)).thenReturn(user);
        when(activityService.getById(10L)).thenReturn(activity);

        assertThrows(RuntimeException.class, () ->
                registrationService.createRegistration(1L, 10L, null));
    }
}
