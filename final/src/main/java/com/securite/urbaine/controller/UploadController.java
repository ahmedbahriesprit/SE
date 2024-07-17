package com.securite.urbaine.controller;


import com.securite.urbaine.service.MLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 * @author Ahmed BAHRi
 * @author Mejda Sliman
 */
@Controller
public class UploadController {

    @Autowired
    private MLService mlService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(Model model, MultipartFile file) {
        try {
            long startTime = System.nanoTime();
            mlService.trainModel(file);
            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;
            long minutes = TimeUnit.NANOSECONDS.toMinutes(durationNs);
            long seconds = TimeUnit.NANOSECONDS.toSeconds(durationNs) - TimeUnit.MINUTES.toSeconds(minutes);
            model.addAttribute("message", "File uploaded and model trained successfully in : "+minutes+" min "+seconds+" sec");
        } catch (IOException e) {
            model.addAttribute("message", "Failed to upload file: " + e.getMessage());
        }
        return "index";
    }
}