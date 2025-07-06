package com.example.aifileprocessor.service;

import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionResult;
import com.theokanning.openai.service.OpenAiService as SdkOpenAiService; // Alias for SDK's service
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OpenAiServiceTest {

    @Mock
    private SdkOpenAiService mockSdkService; // Mock for the actual SDK service

    @InjectMocks
    private OpenAiService openAiService; // The service we are testing

    @BeforeEach
    void setUp() {
        // Manually inject the API key for testing purposes, or ensure env var is set in test environment
        // For this example, let's assume the API key is correctly loaded or use ReflectionTestUtils
        // if it's read from @Value or a field directly.
        // If API key is crucial for logic *before* calling mockSdkService, set it up.
        // Here, we are primarily testing the interaction with mockSdkService.
        ReflectionTestUtils.setField(openAiService, "apiKey", "TEST_API_KEY");
        // Re-initialize the service with the mock if the constructor logic depends on the key
        // For the current OpenAiService, the SdkOpenAiService instance is created in the constructor.
        // So, we need to inject the mock SdkOpenAiService into our OpenAiService instance.
        // This is a bit tricky because the SdkOpenAiService is created with `new` in the constructor.
        // A better design would be to inject SdkOpenAiService or a factory for it.
        // For now, we'll re-set the 'service' field in openAiService with our mock.
        ReflectionTestUtils.setField(openAiService, "service", mockSdkService);
    }

    @Test
    void summarizeText_shouldReturnSummaryFromApi() {
        String inputText = "This is a long text to be summarized.";
        String expectedSummaryText = "Summary of text.";

        CompletionChoice choice = new CompletionChoice();
        choice.setText(expectedSummaryText);
        // choice.setIndex(0); // some SDK versions might require this or other fields

        CompletionResult completionResult = new CompletionResult();
        completionResult.setChoices(Collections.singletonList(choice));

        when(mockSdkService.createCompletion(any(CompletionRequest.class))).thenReturn(completionResult);

        String actualSummary = openAiService.summarizeText(inputText);

        assertEquals(expectedSummaryText, actualSummary.trim());

        ArgumentCaptor<CompletionRequest> captor = ArgumentCaptor.forClass(CompletionRequest.class);
        verify(mockSdkService).createCompletion(captor.capture());
        assertTrue(captor.getValue().getPrompt().contains(inputText));
        assertEquals("text-davinci-003", captor.getValue().getModel()); // Or whatever model is set
    }

    @Test
    void summarizeText_whenApiKeyNotConfigured_shouldReturnPlaceholder() {
        // Force apiKey to be null for this test case
        ReflectionTestUtils.setField(openAiService, "apiKey", null);
        // Also ensure the service instance uses a token that indicates it's a dummy for this path
        // This part depends on how the actual OpenAiService is constructed if apiKey is null
        // In the current implementation, it creates a new SdkOpenAiService with "DUMMY_KEY_UNTIL_CONFIGURED"
        // So, we need to simulate that the 'service' field in 'openAiService' has this dummy token.
        // This is tricky to test perfectly without refactoring OpenAiService constructor for better testability.
        // For now, let's assume the logic path for "DUMMY_KEY_UNTIL_CONFIGURED" is taken.
        // We can create a dummy SdkOpenAiService and set its token.
        SdkOpenAiService dummySdk = mock(SdkOpenAiService.class);
        when(dummySdk.getToken()).thenReturn("DUMMY_KEY_UNTIL_CONFIGURED");
        ReflectionTestUtils.setField(openAiService, "service", dummySdk);


        String inputText = "Some text.";
        String summary = openAiService.summarizeText(inputText);

        assertTrue(summary.contains("OpenAI API key not configured"));
        verify(mockSdkService, never()).createCompletion(any()); // Should not call the actual API
    }

    @Test
    void summarizeText_whenApiCallFails_shouldReturnErrorMessage() {
        String inputText = "Text that causes API failure.";
        when(mockSdkService.createCompletion(any(CompletionRequest.class)))
            .thenThrow(new RuntimeException("OpenAI API Error"));

        String summary = openAiService.summarizeText(inputText);

        assertTrue(summary.contains("Error during summarization: OpenAI API Error"));
    }
}
