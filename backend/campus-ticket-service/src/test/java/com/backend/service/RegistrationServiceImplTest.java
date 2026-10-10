package com.backend.service;

import com.backend.dao.mapper.RegistrationMapper;
import com.backend.dao.mapper.TicketTierMapper;
import com.backend.entity.Activity;
import com.backend.entity.Registration;
import com.backend.entity.TicketTier;
import com.backend.contract.UserDto;
import com.backend.service.impl.RegistrationServiceImpl;
import com.backend.messaging.OutboxStore;
import com.backend.client.UserClient;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserClient userService;
    @Mock
    private ActivityService activityService;
    @Mock
    private RegistrationMapper registrationMapper;
    @Mock
    private TicketTierMapper ticketTierMapper;
    @Mock
    private OutboxStore outbox;

    @Spy
    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void createRegistration_rejectsWhenCapacityDecrementFails() {
        UserDto user = new UserDto();
        user.setId(1L);
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setStatus(1);
        TicketTier tier = new TicketTier();
        tier.setId(100L);
        tier.setCapacity(5);
        tier.setPrice(java.math.BigDecimal.ZERO);

        doReturn(0L).when(registrationService).count(any());
        when(activityService.getById(10L)).thenReturn(activity);
        when(ticketTierMapper.getDefaultByActivityId(10L)).thenReturn(tier);
        when(ticketTierMapper.decrementCapacityIfAvailable(100L)).thenReturn(0);

        assertThrows(RuntimeException.class, () ->
                registrationService.createRegistration(1L, 10L, null));
    }

    @Test
    void createRegistration_rejectsDuplicateRegistration() {
        UserDto user = new UserDto();
        user.setId(1L);
        Activity activity = new Activity();
        activity.setId(10L);
        activity.setStatus(1);

        doReturn(1L).when(registrationService).count(any());
        when(activityService.getById(10L)).thenReturn(activity);

        assertThrows(RuntimeException.class, () ->
                registrationService.createRegistration(1L, 10L, null));
    }

    @Test
    void cancelRegistration_doesNotReleaseCapacityWhenConditionalUpdateLosesRace() {
        Registration registration = activeRegistration();
        doReturn(registration).when(registrationService).getById(9L);
        doReturn(false).when(registrationService).update(ArgumentMatchers.<Wrapper<Registration>>any());

        assertThrows(RuntimeException.class, () -> registrationService.cancelRegistration(9L));

        verify(ticketTierMapper, never()).incrementCapacity(anyLong(), anyInt());
    }

    @Test
    void cancelRegistration_releasesCapacityExactlyAfterSuccessfulStateTransition() {
        Registration registration = activeRegistration();
        doReturn(registration).when(registrationService).getById(9L);
        doReturn(true).when(registrationService).update(ArgumentMatchers.<Wrapper<Registration>>any());

        var detail=new com.backend.vo.RegistrationDetail();
        when(registrationMapper.getRegistrationDetailById(9L)).thenReturn(detail);
        registrationService.cancelRegistration(9L);

        verify(ticketTierMapper).incrementCapacity(100L, 1);
    }

    private Registration activeRegistration() {
        Registration registration = new Registration();
        registration.setId(9L);
        registration.setUserId(1L);
        registration.setTicketTierId(100L);
        registration.setTicketCount(1);
        registration.setStatus(1);
        return registration;
    }
}
