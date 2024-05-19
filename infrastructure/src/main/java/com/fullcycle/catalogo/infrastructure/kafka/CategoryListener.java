package com.fullcycle.catalogo.infrastructure.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fullcycle.catalogo.application.category.delete.DeleteCategoryUseCase;
import com.fullcycle.catalogo.application.category.save.SaveCategoryUseCase;
import com.fullcycle.catalogo.infrastructure.category.CategoryClient;
import com.fullcycle.catalogo.infrastructure.category.models.CategoryEvent;
import com.fullcycle.catalogo.infrastructure.configuration.json.Json;
import com.fullcycle.catalogo.infrastructure.kafka.models.connect.MessageValue;
import com.fullcycle.catalogo.infrastructure.kafka.models.connect.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CategoryListener {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryListener.class);
    public static final TypeReference<MessageValue<CategoryEvent>> CATEGORY_MESSAGE = new TypeReference<>() {
    };

    private final CategoryClient categoryClient;
    private final SaveCategoryUseCase saveCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public CategoryListener(
            final CategoryClient categoryClient,
            final SaveCategoryUseCase saveCategoryUseCase,
            final DeleteCategoryUseCase deleteCategoryUseCase
    ) {
        this.categoryClient = Objects.requireNonNull(categoryClient);
        this.saveCategoryUseCase = Objects.requireNonNull(saveCategoryUseCase);
        this.deleteCategoryUseCase = Objects.requireNonNull(deleteCategoryUseCase);
    }

    @KafkaListener(
            concurrency = "${kafka.consumers.categories.concurrency}", // should be the same number as the partitions
            containerFactory = "kafkaListenerFactory",
            topics = "${kafka.consumers.categories.topics}",
            groupId = "${kafka.consumers.categories.group-id}",
            id = "${kafka.consumers.categories.id}",
            properties = {
                    "auto.offset.reset=${kafka.consumers.categories.auto-offset-reset}"
            }
    )
    @RetryableTopic(
            backoff = @Backoff(delay = 1000, multiplier = 2), // delay time strategy to retry
            attempts = "${kafka.consumers.categories.max-attempts}", // numbers of attempts to read the message
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE // define the dead letter topic name strategy
    )
    public void onMessage(@Payload(required = false) final String payload, final ConsumerRecordMetadata metadata) {
        if (payload == null) {
            LOG.info("Message received from Kafka [topic:{}] [partition:{}] [offset:{}]: EMPTY", metadata.topic(), metadata.partition(), metadata.offset());
            return;
        }

        LOG.info("Message received from Kafka [topic:{}] [partition:{}] [offset:{}]: {}", metadata.topic(), metadata.partition(), metadata.offset(), payload);
        final var messagePayload = Json.readValue(payload, CATEGORY_MESSAGE).payload();
        final var op = messagePayload.operation();

        if (Operation.isDelete(op)) {
            this.deleteCategoryUseCase.execute(messagePayload.before().id());
        } else {
            this.categoryClient.categoryOfId(messagePayload.after().id())
                    .ifPresentOrElse(this.saveCategoryUseCase::execute, () -> {
                        LOG.warn("Category was not found {}", messagePayload.after().id());
                    });
        }
    }

    @DltHandler
    public void onDLTMessage(@Payload final String payload, final ConsumerRecordMetadata metadata) {
        LOG.warn("Message received from Kafka at DLT [topic:{}] [partition:{}] [offset:{}]: {}", metadata.topic(), metadata.partition(), metadata.offset(), payload);
        final var messagePayload = Json.readValue(payload, CATEGORY_MESSAGE).payload();
        final var op = messagePayload.operation();

        if (Operation.isDelete(op)) {
            this.deleteCategoryUseCase.execute(messagePayload.before().id());
        } else {
            this.categoryClient.categoryOfId(messagePayload.after().id())
                    .ifPresentOrElse(this.saveCategoryUseCase::execute, () -> {
                        LOG.warn("Category was not found {}", messagePayload.after().id());
                    });
        }
    }
}
