package com.example.crm;

import com.example.crm.controller.TestController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestControllerUnitTest {

    @Test
    void testEndpointReturnsExpectedString() {
        TestController controller = new TestController();
        String resp = controller.test();
        assertEquals("Application is running", resp);
    }
}
