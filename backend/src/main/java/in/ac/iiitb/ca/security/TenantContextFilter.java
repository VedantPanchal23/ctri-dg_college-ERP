package in.ac.iiitb.ca.security;

import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.identity.UserAccount;
import in.ac.iiitb.ca.identity.UserAccountRepository;
import in.ac.iiitb.ca.tenant.Tenant;
import in.ac.iiitb.ca.tenant.TenantRepository;
import in.ac.iiitb.ca.tenant.TenantStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final UserAccountRepository userAccountRepository;
    private final TenantRepository tenantRepository;

    public TenantContextFilter(UserAccountRepository userAccountRepository, TenantRepository tenantRepository) {
        this.userAccountRepository = userAccountRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getPrincipal() instanceof Jwt jwt) {
                AuthUser authUser = resolveAuthUser(jwt);
                if (authUser.tenantId() != null) {
                    TenantContext.setTenantId(authUser.tenantId());
                    Tenant tenant = tenantRepository.findById(authUser.tenantId())
                            .orElseThrow(() -> ApiException.notFound("Tenant not found"));
                    if (tenant.getStatus() == TenantStatus.SUSPENDED && !authUser.isPlatformAdmin()) {
                        throw ApiException.tenantSuspended();
                    }
                }
                if (authUser.isPlatformAdmin()) {
                    TenantContext.setPlatformScope(true);
                }
                Set<SimpleGrantedAuthority> authorities = authUser.roles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toSet());
                UsernamePasswordAuthenticationToken enriched =
                        new UsernamePasswordAuthenticationToken(authUser, "n/a", authorities);
                SecurityContextHolder.getContext().setAuthentication(enriched);
            }
            filterChain.doFilter(request, response);
        } catch (ApiException ex) {
            response.setStatus(ex.getStatus().value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"code":"%s","message":"%s"}
                    """.formatted(ex.getCode(), ex.getMessage()));
        } finally {
            TenantContext.clear();
        }
    }

    private AuthUser resolveAuthUser(Jwt jwt) {
        String sub = jwt.getSubject();
        Set<String> jwtRoles = KeycloakJwtAuthenticationConverter.extractRoles(jwt);
        Optional<UserAccount> existing = userAccountRepository.findByKeycloakSubAndDeletedAtIsNull(sub);
        if (existing.isPresent()) {
            UserAccount account = existing.get();
            Set<String> roles = account.getRoles().isEmpty() ? jwtRoles : account.getRoles();
            return new AuthUser(
                    account.getId(),
                    account.getKeycloakSub(),
                    account.getEmail(),
                    account.getDisplayName(),
                    account.getTenantId(),
                    account.getCompanyId(),
                    roles
            );
        }

        UUID tenantId = parseUuid(jwt.getClaimAsString("tenant_id"));
        String email = firstNonBlank(jwt.getClaimAsString("email"), jwt.getClaimAsString("preferred_username"), sub);
        String name = firstNonBlank(jwt.getClaimAsString("name"), email);
        UserAccount created = new UserAccount();
        created.setKeycloakSub(sub);
        created.setEmail(email);
        created.setDisplayName(name);
        created.setTenantId(tenantId);
        created.setStatus(in.ac.iiitb.ca.identity.UserStatus.ACTIVE);
        created.setRoles(jwtRoles);
        UserAccount saved = userAccountRepository.save(created);
        return new AuthUser(
                saved.getId(),
                saved.getKeycloakSub(),
                saved.getEmail(),
                saved.getDisplayName(),
                saved.getTenantId(),
                saved.getCompanyId(),
                jwtRoles
        );
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown";
    }
}
