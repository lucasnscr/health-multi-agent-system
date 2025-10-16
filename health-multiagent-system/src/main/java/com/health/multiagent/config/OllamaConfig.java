package com.health.multiagent.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.chat.options.model}")
    private String chatModelName;

    @Value("${spring.ai.ollama.chat.options.temperature}")
    private Double chatTemperature;

    @Value("${spring.ai.ollama.chat.options.top-p}")
    private Double chatTopP;

    @Value("${spring.ai.ollama.chat.options.num-predict}")
    private Integer chatNumPredict;

    @Value("${spring.ai.ollama.chat.options.repeat-penalty}")
    private Double chatRepeatPenalty;

    @Value("${spring.ai.ollama.chat.options.presence-penalty}")
    private Double chatPresencePenalty;



    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }


    @Bean
    @Primary
    public ChatModel chatModel(
            ToolCallingManager toolCallingManager,
            ObservationRegistry observationRegistry,
            ModelManagementOptions modelManagementOptions,
            OllamaApi ollamaApi) {
        OllamaOptions.Builder builder = OllamaOptions.builder()
                .model(chatModelName)
                .temperature(chatTemperature)
                .topP(chatTopP)
                .numPredict(chatNumPredict)
                .repeatPenalty(chatRepeatPenalty)
                .presencePenalty(chatPresencePenalty);

        OllamaOptions chatOptions = builder.build();
        return new OllamaChatModel(
                ollamaApi,
                chatOptions,
                toolCallingManager,
                observationRegistry,
                modelManagementOptions);
    }

    /**
     * Modelo de embeddings para busca vetorial
     */
    @Bean
    public EmbeddingModel embeddingModel(OllamaApi ollamaApi,
                                         ObservationRegistry observationRegistry,
                                         ModelManagementOptions modelManagementOptions) {
        return new OllamaEmbeddingModel(
                ollamaApi,
                OllamaOptions.builder()
                        .model("nomic-embed-text")
                        .build(),
                observationRegistry,
                modelManagementOptions);
    }

    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.create();
    }

    @Bean
    public ModelManagementOptions modelManagementOptions() {
        return ModelManagementOptions.builder().build();
    }

}
