package smartcase.base.auth;

import java.io.Serializable;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户身份凭证
 *
 * @author Maple
 * @see Function#identity()
 */
public final class Credential implements Serializable {

  /** 返回一个无效的对象 */
  public static final Credential INVALID_CREDENTIAL = Credential.builder(false).build();

  /** 用户身份的唯一标识符 */
  private Serializable identity;

  /** 凭证是否有效 */
  private boolean valid = true;

  /** 用户角色信息 */
  private String[] roles;

  /** 用户信息缓存 */
  private Object userInfo;

  private Credential(boolean valid) {
    this.valid = valid;
  }

  public boolean getValid() {
    return this.valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public Serializable getIdentity() {
    return identity;
  }

  public String[] getRoles() {
    return roles;
  }

  public Object getUserInfo() {
    return userInfo;
  }

  public static Builder builder(boolean valid) {
    return new Builder(valid);
  }

  public static class Builder {
    private Credential credential;

    Builder(boolean valid) {
      this.credential = new Credential(valid);
    }

    public Builder identity(@NonNull Serializable identity) {
      this.credential.identity = identity;
      return this;
    }

    public Builder roles(@NonNull String... roles) {
      this.credential.roles = roles;
      return this;
    }

    public Builder valid(boolean valid) {
      this.credential.valid = valid;
      return this;
    }

    public Builder userInfo(@Nullable Object userInfo) {
      this.credential.userInfo = userInfo;
      return this;
    }

    public Credential build() {
      // 如果表示当前对象为空 则不做任何校验
      if (this.credential.valid) {
        com.google.common.base.Verify.verify(
            this.credential.roles != null, "user must has an roles");
      }
      return this.credential;
    }
  }
}
