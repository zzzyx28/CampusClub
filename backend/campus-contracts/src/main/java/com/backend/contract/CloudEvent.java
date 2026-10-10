package com.backend.contract;
public record CloudEvent(String eventId,String type,String source,String traceId,String payload) {}
