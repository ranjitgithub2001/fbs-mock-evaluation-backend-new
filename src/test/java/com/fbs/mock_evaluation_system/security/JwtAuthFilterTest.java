package com.fbs.mock_evaluation_system.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsServiceImpl userDetailsService;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtil, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicTrainerRegistration_skipsJwtEvenWhenBearerTokenPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-requests");
        request.addHeader("Authorization", "Bearer stale.jwt.for.deleted.user");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertTrue(JwtAuthFilter.isPublicUnauthenticatedPath(request));

        filter.doFilter(request, response, chain);

        verify(jwtUtil, never()).isTokenValid(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(chain.getRequest() != null);
    }

    @Test
    void publicTrainerRegistration_doesNotRequireAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-requests");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(userDetailsService, never()).loadUserByUsername(any());
        assertTrue(chain.getRequest() != null);
    }

    @Test
    void trainerRequestApprove_isNotTreatedAsPublic() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/trainer-requests/4/approve");
        assertFalse(JwtAuthFilter.isPublicUnauthenticatedPath(request));
    }

    @Test
    void deletedUserJwt_doesNotThrow500_andContinuesUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Bearer stale.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtil.isTokenValid("stale.jwt")).thenReturn(true);
        when(jwtUtil.extractEmail("stale.jwt")).thenReturn("deleted.trainer@example.com");
        when(userDetailsService.loadUserByUsername("deleted.trainer@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));

        verify(userDetailsService).loadUserByUsername("deleted.trainer@example.com");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(chain.getRequest() != null);
    }

    @Test
    void existingUserJwt_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Bearer valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        UserDetails userDetails = new User(
                "admin@fbs.com",
                "hashed",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(jwtUtil.isTokenValid("valid.jwt")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.jwt")).thenReturn("admin@fbs.com");
        when(userDetailsService.loadUserByUsername("admin@fbs.com")).thenReturn(userDetails);

        filter.doFilter(request, response, chain);

        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        assertTrue(chain.getRequest() != null);
    }
}
