package com.audio_to_lines_project.audio_to_lines_project;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
@Service
@Component
public class ServiceClass {
    private static final String bucketName = "spechproject_buvket";
    String transcription = "";

    //////////////uploading file from frontend to local server ////////////////


    public ResponseEntity<String> uploadFileonLocalhost(MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("please select a file");
        }
        if (!file.getContentType().startsWith("audio/")) {
            System.out.println("Content Type: " + file.getContentType());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Request must contain an audio file");
        }
        Path localHostUpload = Paths.get("src/main/resources/static/SpeechFiles/").toAbsolutePath();
        String fileName = file.getOriginalFilename();
        try {
            Files.copy(file.getInputStream(), Paths.get(localHostUpload + File.separator + file.getOriginalFilename()), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("I'm running on local host");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(file.getOriginalFilename());
        transcription = convertMP3toLinear16(fileName);

        return ResponseEntity.ok("working");
    }

    ///////////converting mp3/ ogg to wav //////////////////////////////


    static String mp3FilePath = "src/main/resources/static/SpeechFiles/";
    static String WavFilePath = "src/main/resources/static/SpeechFiles/output.wav";
    public static String convertMP3toLinear16(String fileName) {
        String transcribedText ="";
        try {
             System.out.println(mp3FilePath + fileName);
            // FFmpeg command
            String ffmpegCommand = "ffmpeg -i " + mp3FilePath + fileName + " -acodec pcm_s16le -ar 16000 " + WavFilePath;
            // Execute the FFmpeg command using ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", ffmpegCommand);
            Process process = processBuilder.start();
            // Wait for the process to complete
            int exitCode = process.waitFor();
            // Check if the conversion was successful
            if (exitCode == 0) {
                System.out.println("Conversion successful");
                transcribedText = uploadFileToCloudStorage(bucketName, WavFilePath);
                deleteFile(mp3FilePath + fileName);
                return transcribedText;
            } else {
                System.out.println("Error during conversion. Exit code: " + exitCode);
                return "Error during conversion. Exit code: " + exitCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during conversion.";
        }
    }


////////////////////////////////////uploading .wav file on gcloud bucket /////////////////////


    public static String uploadFileToCloudStorage(String bucketName, String filePath) throws Exception {
        String transcribedText = "";
        // Initialize Google Cloud Storage client with credentials
        Storage storage = StorageOptions.getDefaultInstance().getService();
        Path path = Paths.get(filePath);
        String objectName = path.getFileName().toString();

        Blob blob = storage.get(bucketName, objectName);
        if (blob == null) {
            Blob uploadedBlob = storage.create(Blob.newBuilder(bucketName, objectName).build(), Files.readAllBytes(path));
            System.out.println("File uploaded successfully: " + uploadedBlob.getName());

            try {
                transcribedText = transcribeAudioFile(bucketName);
                deleteFile(filePath);
                return transcribedText;

            } catch (IOException e) {
                e.printStackTrace();
                return "error runnning transcribeAudioFile method";
            }
        } else {
            System.out.println("File already exists in the bucket: " + blob.getName());

            return "File already exists in the bucket: " + blob.getName();
        }
    }


    ////////////////////////////////////transcripting/////////////////////


    private static final String objectName = "output.wav";

    private static String transcribeAudioFile(String bucketName) throws IOException {
    // Initialize Google Cloud Storage client with credentials
        Storage storage = initializeStorage();

        // Download the audio file from Google Cloud Storage
        ByteString audioBytes = downloadAudioFromBucket(storage, bucketName, objectName);

    // Initialize Google Cloud Speech-to-Text client
    try (SpeechClient speechClient = SpeechClient.create()) {
        // Configure Speech-to-Text request
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(16000)  // Adjust according to your audio file's sample rate
                .setLanguageCode("en-US")   // Adjust according to the language of the audio
                .build();

        RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();
        String transcript = "";
        // Perform speech recognition
        RecognizeResponse response = speechClient.recognize(config, audio);
        StringBuilder transcriptBuilder = new StringBuilder();

        for (SpeechRecognitionResult result : response.getResultsList()) {
            // Append each transcription to the StringBuilder
            transcriptBuilder.append(result.getAlternatives(0).getTranscript()).append(" ");
        }

        // Convert the StringBuilder to a String
        transcript = transcriptBuilder.toString().trim();

        // Print or return the final transcript
        System.out.println("Transcription: " + transcript);
        deleteObject(bucketName, objectName);
        return transcript;

    } catch (Exception e) {
        System.out.println("Error during transcription: " + e.getMessage());
        return "Error during transcription";
    }

}

private static ByteString downloadAudioFromBucket(Storage storage, String bucketName, String objectName) {
    Blob blob = storage.get(bucketName, objectName);
    return ByteString.copyFrom(blob.getContent());
}

private static Storage initializeStorage() throws IOException {
        // Retrieve the path to the JSON credentials file from the environment variable
        String credentialsFilePath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

        // Read the content of the JSON credentials file
        Path path = Paths.get(credentialsFilePath);
        byte[] credentialsBytes = Files.readAllBytes(path);

        // Create GoogleCredentials from the JSON content
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(credentialsBytes));

        // Initialize StorageOptions with the created credentials
        return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
    }


    ////////////////////////////////////Deleting file from localhost & Gcloud after usage/////////////////////


    private static void deleteObject(String bucketName, String objectName) {
        // Initialize Google Cloud Storage client
        Storage storage = StorageOptions.getDefaultInstance().getService();

        // Create BlobId for the object to be deleted
        BlobId blobId = BlobId.of(bucketName, objectName);

        // Delete the object
        boolean deleted = storage.delete(blobId);

        if (deleted) {
            System.out.println("Object deleted successfully: gs://" + bucketName + "/" + objectName);
        } else {
            System.out.println("Object deletion failed: gs://" + bucketName + "/" + objectName);
        }
    }
    private static void deleteFile(String filePath) throws IOException {
        // Create a Path object from the file path
        Path path = Paths.get(filePath);

        // Delete the file
        boolean deleted = Files.deleteIfExists(path);

        if (deleted) {
            System.out.println("File deleted successfully: " + filePath);
        } else {
            System.out.println("File deletion failed or file doesn't exist: " + filePath);
        }
    }
}



