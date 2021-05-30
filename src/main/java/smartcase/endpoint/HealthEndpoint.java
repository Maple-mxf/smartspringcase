package smartcase.endpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import smartcase.base.auth.Verify;
import smartcase.core.ApiResponse;
import smartcase.core.ApiStandard;

import static smartcase.core.StandardKt.error;
import static smartcase.core.StandardKt.ok;

@RestController
public class HealthEndpoint {

    @GetMapping("/echo")
    public ApiResponse echo() {
        return ok();
    }

    @GetMapping("/echoError")

    public ApiResponse echoError() {
        return error(ApiStandard.BAD_REQUEST);
    }

    @Verify
    @GetMapping("/auth")
    public ApiResponse basedAuthEcho() {
        return ok();
    }

}
