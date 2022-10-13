package ru.search.mga.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController {

    public static  String webInterfaceUrl;

    public static void setWebInterfaceUrl(String webInterfaceUrl) {
        DefaultController.webInterfaceUrl = webInterfaceUrl;
    }

    public static String getWebInterfaceUrl() {
        return webInterfaceUrl;
    }

    @RequestMapping(value = "${webInterfaceUrl}")
    public String index (Model model) {
        model.addAttribute("adminName", webInterfaceUrl);
        return "index";
    }
}
