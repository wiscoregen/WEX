package com.wex.assessment.web;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiErrorControllerTest {

    @Test
    void errorControllerReturnsJsonEnvelopeForRouteNotFound() throws Exception {
        ErrorAttributes errorAttributes = new StubErrorAttributes(Map.of(
                "status", 404,
                "path", "/api/v1/unknown"
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ApiErrorController(errorAttributes)).build();

        mockMvc.perform(get("/error")
                        .with(errorAttributes(404, "/api/v1/unknown"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("route_not_found"))
                .andExpect(jsonPath("$.error.status").value(404))
                .andExpect(jsonPath("$.error.message").value("no route found for GET /api/v1/unknown"));
    }

    private RequestPostProcessor errorAttributes(int statusCode, String path) {
        return request -> {
            MockHttpServletRequest servletRequest = (MockHttpServletRequest) request;
            servletRequest.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, statusCode);
            servletRequest.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path);
            return servletRequest;
        };
    }

    private static final class StubErrorAttributes implements ErrorAttributes {
        private final Map<String, Object> attributes;

        private StubErrorAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public Throwable getError(WebRequest webRequest) {
            return null;
        }

        @Override
        public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
            return attributes;
        }
    }
}
