package in.gov.ipie.service.iam.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import in.gov.ipie.common.events.deadletter.DeadLetterSupport;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cross-service RabbitMQ wiring for every consumer of ipie-user-service's own exchange (not this
 * service's, unlike the template's self-consumption {@code RabbitConsumerConfig}/{@code
 * RabbitUserEventLogConsumer}) - {@link
 * in.gov.ipie.service.iam.messaging.UserVerifiedEventConsumer} and {@code
 * in.gov.ipie.service.iam.messaging.PillarResolutionEventConsumer}, each filtered to only
 * the routing key(s) it actually cares about. Reuses {@code RabbitConsumerConfig}'s {@code
 * rabbitListenerContainerFactory} bean (not redeclared here) - {@code @RabbitListener} needs
 * exactly one factory in the context, and {@code @EnableRabbit} is already active via that class.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class UserServiceEventConsumerConfig {

    @Bean
    public TopicExchange userServiceEventsExchange(
            @Value("${ipie.integrations.user-service.rabbitmq.exchange:ipie-user-service.events}") String exchange) {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue accountProvisioningRequestedQueue(
            @Value("${ipie.integrations.user-service.rabbitmq.account-provisioning-queue:"
                    + "ipie-iam-service.events.account-provisioning-requested}") String queue) {
        return DeadLetterSupport.workQueue(queue);
    }

    @Bean
    public Binding accountProvisioningRequestedBinding(
            Queue accountProvisioningRequestedQueue, TopicExchange userServiceEventsExchange) {
        return BindingBuilder.bind(accountProvisioningRequestedQueue)
                .to(userServiceEventsExchange)
                .with("ACCOUNT_PROVISIONING_REQUESTED");
    }

    @Bean
    public Queue userVerifiedQueue(
            @Value("${ipie.integrations.user-service.rabbitmq.user-verified-queue:ipie-iam-service.events.user-verified}") String queue) {
        return DeadLetterSupport.workQueue(queue);
    }

    @Bean
    public Binding userVerifiedBinding(Queue userVerifiedQueue, TopicExchange userServiceEventsExchange) {
        return BindingBuilder.bind(userVerifiedQueue).to(userServiceEventsExchange).with("USER_VERIFIED");
    }

    @Bean
    public Queue accountLinkedQueue(
            @Value("${ipie.integrations.user-service.rabbitmq.account-linked-queue:ipie-iam-service.events.account-linked}")
                    String queue) {
        return DeadLetterSupport.workQueue(queue);
    }

    @Bean
    public Binding accountLinkedBinding(Queue accountLinkedQueue, TopicExchange userServiceEventsExchange) {
        return BindingBuilder.bind(accountLinkedQueue).to(userServiceEventsExchange).with("ACCOUNT_LINKED");
    }

    @Bean
    public Queue accountUnlinkedQueue(
            @Value("${ipie.integrations.user-service.rabbitmq.account-unlinked-queue:ipie-iam-service.events.account-unlinked}")
                    String queue) {
        return DeadLetterSupport.workQueue(queue);
    }

    @Bean
    public Binding accountUnlinkedBinding(Queue accountUnlinkedQueue, TopicExchange userServiceEventsExchange) {
        return BindingBuilder.bind(accountUnlinkedQueue).to(userServiceEventsExchange).with("ACCOUNT_UNLINKED");
    }
}
