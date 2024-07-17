package com.securite.urbaine.controller;

import com.securite.urbaine.service.MLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Contrôleur pour gérer les opérations de téléchargement de fichiers et de prédiction.
 *
 * @author Ahmed BAHRI
 * @author Mejda Sliman
 */
@Controller
public class UploadController {

    @Autowired
    private MLService mlService;

    /**
     * Affiche la page d'index.
     *
     * @return le nom de la vue d'index.
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Gère le téléchargement du fichier et l'entraînement du modèle.
     *
     * @param model      le modèle pour transmettre des attributs à la vue.
     * @param file       le fichier à télécharger.
     * @param numThreads le nombre de threads à utiliser pour l'entraînement du modèle.
     * @return le nom de la vue d'index.
     */
    @PostMapping("/upload")
    public String uploadFile(Model model, @RequestParam("file") MultipartFile file,
                             @RequestParam(name = "numThreads", defaultValue = "0") int numThreads) {
        try {
            if (numThreads > 0) {
                mlService.setNumThreads(numThreads);
            }
            long startTime = System.nanoTime();
            mlService.trainModel(file);
            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;
            long minutes = TimeUnit.NANOSECONDS.toMinutes(durationNs);
            long seconds = TimeUnit.NANOSECONDS.toSeconds(durationNs) - TimeUnit.MINUTES.toSeconds(minutes);
            model.addAttribute("message", "File uploaded and model trained successfully in : " + minutes + " min " + seconds + " sec");
        } catch (IOException e) {
            model.addAttribute("message", "Failed to upload file: " + e.getMessage());
        }
        return "index";
    }

    /**
     * Récupère la progression de l'entraînement du modèle.
     *
     * @return la progression en pourcentage.
     */
    @GetMapping("/progress")
    @ResponseBody
    public ResponseEntity<Integer> getProgress() {
        return ResponseEntity.ok(mlService.getProgress());
    }

    /**
     * Gère les prédictions de risque en fonction des paramètres fournis.
     *
     * @param zone  la zone à prédire.
     * @param time  l'heure à prédire.
     * @param day   le jour à prédire.
     * @param model le modèle pour transmettre des attributs à la vue.
     * @return le nom de la vue d'index.
     */
    @GetMapping("/predict")
    public String predict(@RequestParam("zone") int zone, @RequestParam("time") int time,
                          @RequestParam("day") int day, Model model) {
        String risk = mlService.predictRisk(zone, time, day);
        model.addAttribute("risk", risk);
        return "index";
    }
}