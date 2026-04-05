package org.example.ask.conversation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ask.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final OpenAIClient openAIClient;
    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;

    @Value("${app.audio.max-size-bytes}")
    private long maxSizeBytes;

    public Conversation process(MultipartFile audio, User user) throws IOException {
        validateAudio(audio);
        // Step 1: Speech-to-Text via Whisper — then discard audio bytes, keep only text
        String transcript = transcribe(audio);
        log.info("Transcript: {}", transcript);

        // Step 2 + 3: Combined classify + answer in one ChatGPT call
        AiResult aiResult = classifyAndAnswer(transcript);
        log.info("Type: {}, Result length: {}", aiResult.type(), aiResult.result().length());

        // Step 4: Persist only the text (no audio stored)
        Conversation conversation = Conversation.builder()
                .user(user)
                .transcript(transcript)
                .type(aiResult.type())
                .result(aiResult.result())
                .build();

        return conversationRepository.save(conversation);
    }

    private void validateAudio(MultipartFile audio) {
        if (audio.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                    "Audio file exceeds the maximum allowed size of " + (maxSizeBytes / (1024 * 1024)) + " MB.");
        }


    }

    private String transcribe(MultipartFile audio) throws IOException {
        String originalFilename = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.mp3";
        Path tempFile = Files.createTempFile("audio-", "-" + originalFilename);
        try {
            audio.transferTo(tempFile.toFile());
            TranscriptionCreateParams params = TranscriptionCreateParams.builder()
                    .file(tempFile)
                    .model(AudioModel.WHISPER_1)
                    .build();
            // Union type — default response is Transcription when no extra params given
            return openAIClient.audio().transcriptions().create(params).asTranscription().text();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private AiResult classifyAndAnswer(String transcript) {
        String systemPrompt = """
                You are an AI assistant. Given a text, you must:
                1. Classify it as one of: QUESTION, IDEA, or OTHER.
                   - QUESTION: the text is asking something and expects an answer.
                   - IDEA: the text proposes an idea, concept, or suggestion and could benefit from a guideline or elaboration.
                   - OTHER: the text is neither a question nor an idea.
                2. If QUESTION: provide a clear, helpful answer.
                   If IDEA: provide a practical guideline or actionable steps.
                   If OTHER: result must be an empty string "".
                
                Respond ONLY with valid JSON in this exact format:
                {"type":"QUESTION","result":"your answer here"}
                """;

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .addSystemMessage(systemPrompt)
                .addUserMessage(transcript)
                .responseFormat(ResponseFormatJsonObject.builder().build())
                .build();

        String json = openAIClient.chat().completions().create(params)
                .choices().getFirst()
                .message().content()
                .orElse("{\"type\":\"OTHER\",\"result\":\"\"}");

        try {
            RawAiResponse raw = objectMapper.readValue(json, RawAiResponse.class);
            ConversationType type;
            try {
                type = ConversationType.valueOf(raw.type().toUpperCase());
            } catch (IllegalArgumentException e) {
                type = ConversationType.OTHER;
            }
            String result = (type == ConversationType.OTHER) ? "" : (raw.result() != null ? raw.result() : "");
            return new AiResult(type, result);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", json, e);
            return new AiResult(ConversationType.OTHER, "");
        }
    }

    private record RawAiResponse(
            @JsonProperty("type") String type,
            @JsonProperty("result") String result) {}

    private record AiResult(ConversationType type, String result) {}
}
