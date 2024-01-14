package com.audio_to_lines_project.audio_to_lines_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class TranscriptController {

    @Autowired
    private ServiceClass serviceClass;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("transcription", null);


        return "HomePage";
    }

    @PostMapping("/upload")
    public String transcribe(@RequestParam("file") MultipartFile file, Model model) {
        try {
            // Upload the file to localhost and convert to Linear16
            ResponseEntity<String> uploadResponse = serviceClass.uploadFileonLocalhost(file);

            if (uploadResponse != null && uploadResponse.getStatusCode().is2xxSuccessful()) {
                // Transcribe the uploaded file from Google Cloud Storage
                String transcription = serviceClass.transcription;

                // Add the transcribed text to the model
                model.addAttribute("transcription", transcription);
            } else {
                // Handle upload failure
                String errorMessage = "Error during upload: " +
                        (uploadResponse != null ? uploadResponse.getBody() : "Unknown error");
                model.addAttribute("transcription", errorMessage);
            }
        } catch (Exception e) {
            // Handle other exceptions
            e.printStackTrace();
            model.addAttribute("transcription", "Error during transcription: " + e.getMessage());
        }
        return "HomePage";
    }
    @Autowired private EmailServiceImpl emailService;
    @PostMapping("/sendMail")
    public String sendMail(@RequestParam String email,@RequestParam String editorText, Model model) {
        model.addAttribute("editorText", editorText);
        emailService.sendEmail(email, editorText);
        model.addAttribute("Email", "Email sent successfully..");
        return"HomePage";
    }
}
