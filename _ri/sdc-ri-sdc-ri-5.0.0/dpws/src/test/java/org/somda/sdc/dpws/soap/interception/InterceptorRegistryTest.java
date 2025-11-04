package org.somda.sdc.dpws.soap.interception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.somda.sdc.dpws.DpwsTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterceptorRegistryTest extends DpwsTest {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void testAddInterceptor() {
        final String action = "http://action";
        InterceptorRegistry registry = getInjector().getInstance(InterceptorRegistry.class);
        registry.addInterceptor(new Interceptor() {
            @MessageInterceptor
            void test(RequestResponseObject rrInfo) {
            }
        });
        registry.addInterceptor(new Interceptor() {
            @MessageInterceptor(sequenceNumber = 5)
            void test(RequestResponseObject rrInfo) {
            }
        });

        registry.addInterceptor(new Interceptor() {
            @MessageInterceptor(action)
            void test(RequestResponseObject rrInfo) {
            }
        });

        List<InterceptorInfo> defaultInterceptors = registry.getDefaultInterceptors();
        assertEquals(2, defaultInterceptors.size());
        assertEquals(5, defaultInterceptors.get(0).getSequenceNumber());
        assertEquals(Integer.MAX_VALUE, defaultInterceptors.get(1).getSequenceNumber());
        List<InterceptorInfo> interceptors = registry.getInterceptors(action);
        assertEquals(1, interceptors.size());
    }
}