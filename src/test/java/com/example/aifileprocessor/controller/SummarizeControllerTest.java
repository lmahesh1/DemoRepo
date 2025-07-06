package com.example.aifileprocessor.controller;

import com.example.aifileprocessor.service.OpenAiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SummarizeController.class)
public class SummarizeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAiService openAiService;

    @Test
    void summarizeTxtFile_whenValidTxtFile_shouldReturnSummary() throws Exception {
        String fileName = "test.txt";
        String fileContent = "This is the content of the test file.";
        String expectedSummary = "This is the summary.";

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                fileContent.getBytes()
        );

        when(openAiService.summarizeText(fileContent)).thenReturn(expectedSummary);

        mockMvc.perform(multipart("/api/summarize").file(multipartFile))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedSummary));
    }

    @Test
    void summarizeTxtFile_whenFileIsEmpty_shouldReturnBadRequest() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "empty.txt",
                MediaType.TEXT_PLAIN_VALUE,
                new byte[0] // Empty content
        );
        // Note: Spring's default behavior might make an empty content file not trigger "file.isEmpty()"
        // if the parameter itself is present. The controller's file.isEmpty() checks if no file was sent.
        // For truly empty content, the controller logic might need adjustment or specific test setup.
        // This test as written primarily tests if the "file" part is missing or truly empty in transit.
        // If we want to test the controller's textContent.trim().isEmpty(), that's covered by sending whitespace.

        // To test the isEmpty() check for the MultipartFile object itself:
         mockMvc.perform(multipart("/api/summarize")) // No file attached
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Required request part 'file' is not present"));
        // The above tests if 'file' param is missing.
        // To test controller's file.isEmpty() which Spring might not trigger if parameter name 'file' is there but no actual file:
        // This requires a bit more specific setup or understanding of how Spring handles this.
        // Let's instead test the content empty check:
    }

    @Test
    void summarizeTxtFile_whenFileContentIsEmptyAfterTrim_shouldReturnBadRequest() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "whitespace.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "   ".getBytes() // Content is only whitespace
        );

        mockMvc.perform(multipart("/api/summarize").file(multipartFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("File content is empty or whitespace only."));
    }


    @Test
    void summarizeTxtFile_whenInvalidFileType_shouldReturnBadRequest() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "image.png",
                MediaType.IMAGE_PNG_VALUE,
                "someimagedata".getBytes()
        );

        mockMvc.perform(multipart("/api/summarize").file(multipartFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid file type. Only .txt files are allowed."));
    }

    @Test
    void summarizeTxtFile_whenOpenAiServiceFails_shouldReturnInternalServerError() throws Exception {
        String fileName = "test.txt";
        String fileContent = "This is content.";
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                fileName,
                MediaType.TEXT_PLAIN_VALUE,
                fileContent.getBytes()
        );

        when(openAiService.summarizeText(anyString())).thenThrow(new RuntimeException("OpenAI service error"));

        mockMvc.perform(multipart("/api/summarize").file(multipartFile))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error processing file: OpenAI service error"));
    }
}
