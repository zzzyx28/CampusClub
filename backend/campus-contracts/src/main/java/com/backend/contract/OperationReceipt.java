package com.backend.contract;
public record OperationReceipt(String action, RegistrationDto registration, boolean cancelled) {}
