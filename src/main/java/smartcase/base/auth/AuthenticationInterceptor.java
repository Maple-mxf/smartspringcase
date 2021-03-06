package smartcase.base.auth;

import static org.springframework.web.servlet.HandlerMapping.LOOKUP_PATH;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

public class AuthenticationInterceptor extends BaseInterceptor {

  private static Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

  /** @see PathMatcher {@link AntPathMatcher} */
  private final PathMatcher pathMatcher = new AntPathMatcher("/");

  /** 当前拦截器的顺序 */
  private int order;

  /** 要拦截的路径 */
  private String[] pathPatterns;

  /** @see UrlPathHelper */
  private final UrlPathHelper urlPathHelper = new UrlPathHelper();

  /** 要排除的路径 */
  private String[] excludePathPatterns;

  /** key表示group */
  private ImmutableMap<String, List<AuthRegistration>> authGroup;

  /** @see AuthContext */
  private AuthContext authContext;

  /** @see AuthImpl */
  public AuthenticationInterceptor(AuthContext authContext, AuthMetadata authMetadata) {
    this.authContext = authContext;

    Collection<AuthRegistration> authRules = authMetadata.setupAuthRules();

    this.authGroup =
        ImmutableMap.copyOf(
            authRules.stream().collect(Collectors.groupingBy(AuthRegistration::getGroup)));

    if (!authGroup.containsKey("Default")) {
      throw new RuntimeException("Server auth config error please setup Default auth rule");
    }
  }

  /**
   * @param request {@link HttpServletRequest}
   * @param response {@link HttpServletResponse}
   * @param handler {@link org.springframework.web.method.HandlerMethod}
   * @return {@link Boolean} if true pass else throw a new RuntimeException
   * @see org.springframework.web.util.pattern.PathPattern
   */
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    Verify verify = super.getApiServiceAnnotation(Verify.class, handler);
    if (verify != null) {
      boolean require = verify.require();
      List<AuthRegistration> authRules = this.authGroup.get(verify.group());

      if (authRules == null || authRules.isEmpty()) {
        throw new RuntimeException(
            String.format(
                "Server config Error Please setup auth rules of group %s", verify.group()));
      }

      // 获取请求路径
      String lookupPath = this.urlPathHelper.getLookupPathForRequest(request, LOOKUP_PATH);

      AuthRegistration rule =
          authRules.stream()
              .filter(
                  authRule ->
                      authRule.getPathPatterns().stream()
                          .anyMatch(pathPattern -> this.matches(pathPattern, lookupPath)))
              .findFirst()
              .orElse(null);

      if (rule == null) {
        log.error(
            "Server config error Please setup AuthRegistration of lookup path {} group {}  ",
            lookupPath,
            verify.group());
        return true;
      }

      if (rule.getCredentialFunction() == null) {
        log.error(
            "Server config error Please setup CredentialFunction of lookup path {} group {}  ",
            lookupPath,
            verify.group());
        return true;
      }

      CredentialFunction credentialFunction = rule.getCredentialFunction();

      Credential credential = credentialFunction.apply(request);

      if (checkupCredential(credential, verify, credentialFunction, require)) {
        authContext.setCredential(request, credential);
      }
    }
    return true;
  }

  private boolean checkupCredential(
      Credential credential,
      Verify verify,
      CredentialFunction credentialFunction,
      boolean require) {
    if (!credential.getValid()) {
      if (require) {
        throw credentialFunction.ifErrorThrowing();
      }
      return true;
    }

    // 没有设定角色 || 或者设定了*号  任何角色都可以访问
    String[] requireAllowRoles = verify.role();

    if (requireAllowRoles.length == 0 || "*".equals(requireAllowRoles[0])) return true;

    // 用户角色
    String[] roles = credential.getRoles();
    // 求两个数组的交集
    List<String> requireAllowRoleList = Arrays.asList(requireAllowRoles);
    if (Arrays.stream(roles).anyMatch(requireAllowRoleList::contains)) return true;

    if (require) throw credentialFunction.ifErrorThrowing();

    return true;
  }

  /**
   * Determine a match for the given lookup path.
   *
   * @param pathPattern setup pathPattern
   * @param lookupPath the current request path
   * @return {@code true} if the interceptor applies to the given request path
   */
  private boolean matches(String pathPattern, @NonNull String lookupPath) {
    return this.pathMatcher.match(pathPattern, lookupPath);
  }

  public PathMatcher getPathMatcher() {
    return pathMatcher;
  }

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public String[] getPathPatterns() {
    return pathPatterns;
  }

  public void setPathPatterns(String[] pathPatterns) {
    this.pathPatterns = pathPatterns;
  }

  public UrlPathHelper getUrlPathHelper() {
    return urlPathHelper;
  }

  public String[] getExcludePathPatterns() {
    return excludePathPatterns;
  }

  public void setExcludePathPatterns(String[] excludePathPatterns) {
    this.excludePathPatterns = excludePathPatterns;
  }

  public ImmutableMap<String, List<AuthRegistration>> getAuthGroup() {
    return authGroup;
  }

  public void setAuthGroup(ImmutableMap<String, List<AuthRegistration>> authGroup) {
    this.authGroup = authGroup;
  }

  public AuthContext getAuthContext() {
    return authContext;
  }

  public void setAuthContext(AuthContext authContext) {
    this.authContext = authContext;
  }
}
