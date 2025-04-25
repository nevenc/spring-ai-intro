package dev.tanzu.demo.raffle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
class RaffleController {

    private static final int TOP_K = 1000;

    private final Logger logger = LoggerFactory.getLogger(RaffleController.class);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:prompts/raffle.st")
    private Resource rafflePrompt;

    RaffleController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/raffle/chat")
    String question(
            @RequestParam(
                    name = "question",
                    defaultValue = "pick a random name from the list?"
            ) String question) {

        logger.info("Asking a question: {}", question);

        var searchBuilder = SearchRequest.builder().topK(TOP_K).similarityThresholdAll().build();
        var foundDocuments = vectorStore.similaritySearch(searchBuilder);

        logger.info("Loading {} documents from the vectorstore", foundDocuments.size());

        // get all matched entries to be context for the conversation
        String documents = foundDocuments
                .stream()
                .map(Document::getText)
                .collect(Collectors.joining("---" + System.lineSeparator()));

        logger.debug("Documents: \n {} \n\n", documents);

        return chatClient
                .prompt()
                .user(u -> u.text(rafflePrompt)
                        .param("documents", documents)
                        .param("question", question))
                .call()
                .content();
    }


}
