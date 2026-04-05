package org.example.ask.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final AuthService authService;

    // ---- Register ----
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new AuthDto.RegisterRequest("", ""));
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute AuthDto.RegisterRequest registerRequest,
                           BindingResult bindingResult,
                           HttpServletResponse response,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            AuthDto.AuthResponse authResponse = authService.register(registerRequest);
            setJwtCookie(response, authResponse.token());
            return "redirect:/home";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    // ---- Login ----
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new AuthDto.LoginRequest("", ""));
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute AuthDto.LoginRequest loginRequest,
                        BindingResult bindingResult,
                        HttpServletResponse response,
                        Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        try {
            AuthDto.AuthResponse authResponse = authService.login(loginRequest);
            setJwtCookie(response, authResponse.token());
            return "redirect:/home";
        } catch (BadCredentialsException e) {
            model.addAttribute("error", "Invalid email or password");
            return "auth/login";
        }
    }

    // ---- Logout ----
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/login";
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400);
        response.addCookie(cookie);
    }
}

