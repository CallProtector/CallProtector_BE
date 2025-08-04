package callprotector.spring.global.twilio.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InboundPageController {
    @GetMapping("/popup")
    public String showPopup() {
        return "popup";
    }
}
