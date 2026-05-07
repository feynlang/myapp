package com.example.myapp;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class MainController {
    @GetMapping("/scp")
    @ResponseBody
    public String index() {
        return "안녕하세요 simple crud practice입니당";
    }
    
    @GetMapping("/")
    public String root() {
        return "redirect:/question/list";
    }
    
}
