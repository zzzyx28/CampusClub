package com.backend.contract;
public record RegistrationNotification(Long userId,Long registrationId,String ticketCode,String tierName,String eventStart,int status) {}
