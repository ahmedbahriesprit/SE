//package com.securite.urbaine.controller;
//
//import com.securite.urbaine.service.MLService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//
///**
// * @author Ahmed BAHRi
// * @author Mejda Sliman
// */
//@Controller
//public class PredictController {
////    private static final Logger logger = (Logger) LoggerFactory.getLogger(PredictController.class);
//
//    @Autowired
//    private MLService mlService;
//
//    @GetMapping("/predict")
//    public String predict(@RequestParam("zone") int zone, @RequestParam("time") int time,
//                          @RequestParam("day") int day, Model model) {
////        logger.info("Prediction made for zone: "+zone+", time: "+time+", day: "+day);
//        String risk = mlService.predictRisk(zone, time, day);
//        model.addAttribute("risk", risk);
////        logger.info("Prediction result: "+ risk);
//        return "index";
//    }
//}
