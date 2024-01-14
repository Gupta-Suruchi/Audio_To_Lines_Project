package com.audio_to_lines_project.audio_to_lines_project;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter

public class MailStructure {
    private String transcriptedText;
    private String subject = "Thank You for using Audio to Text Service, Please find your Transcribed Text";
    private String body = "Dear User,\n \nGreetings!! \nHere is your transcribed text:  \n" ;
    private String recipient;
}
