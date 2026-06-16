package com.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BackendApplicationTests {

    @Test
    void mainClassLoads() {
        assertDoesNotThrow(() -> Class.forName("com.backend.BackendApplication"));
    }
}
