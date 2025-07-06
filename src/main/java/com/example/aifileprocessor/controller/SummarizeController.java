package com.example.aifileprocessor.controller;

import com.example.aifileprocessor.service.OpenAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * REST Controller for handling file summarization requests.
 * It provides an endpoint for uploading .txt files, which are then
 * passed to the OpenAiService for summarization.
 */
@RestController
@RequestMapping("/api") // Base path for all endpoints in this controller
public class SummarizeController {

    private final OpenAiService openAiService;

    /**
     * Constructs the SummarizeController with a dependency on OpenAiService.
     * @param openAiService The service used to perform text summarization.
     */
    @Autowired
    public SummarizeController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    /**
     * Endpoint to upload a .txt file and return its summary.
     * Accepts multipart file uploads, specifically expecting a part named "file".
     * Validates the file (must be .txt, not empty).
     * Reads the file content and sends it to OpenAiService for summarization.
     *
     * @param file The MultipartFile (.txt) uploaded by the client.
     * @return A ResponseEntity containing the summary as a string if successful,
     *         or an appropriate error message and HTTP status code if not.
     */
    @PostMapping("/summarize")
    public ResponseEntity<String> summarizeTxtFile(@RequestParam("file") MultipartFile file) {
        // Check if a file was actually uploaded
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty. Please select a .txt file to upload.");
        }

        // Validate file type (must be .txt)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".txt")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file type. Only .txt files are allowed.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Read content from the uploaded file
            String textContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            // Check if the content is just whitespace or empty after reading
            if (textContent.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File content is empty or whitespace only.");
            }

            // Call the service to get the summary
            String summary = openAiService.summarizeText(textContent);

            // Return the summary with HTTP 200 OK
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            // Log the exception for server-side diagnostics.
            // In a production app, use a proper logging framework (e.g., SLF4J with Logback/Log4j2).
            System.err.println("Error processing file in SummarizeController: " + e.getMessage());
            // e.printStackTrace(); // For more detailed debugging if needed

            // Return a generic internal server error to the client
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error processing file: An unexpected error occurred on the server. (" + e.getMessage() + ")");
        }
    }
}
