package com.sylvester.bankapp.rabbitmq;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {


    @Value("${transfer.queue}")
    private String TRANSFER_QUEUE;
    @Value("${transfer.exchange}")
    private String transferExchange;
    @Value("${transfer.routing-key}")
    private String transferRoutingKey;

    @Value("${withdraw.queue}")
    private String WITHDRAW_QUEUE;
    @Value("${withdraw.exchange}")
    private String withdrawExchange;
    @Value("${withdraw.routing-key}")
    private String withdrawRoutingKey;

    @Value("${deposit.queue}")
    private String DEPOSIT_QUEUE;
    @Value("${deposit.exchange}")
    private String depositExchange;
    @Value("${deposit.routing-key}")
    private String depositRoutingKey;

    @Value("${statement.queue}")
    private String STATEMENT_QUEUE;
    @Value("${statement.exchange}")
    private String statementExchange;
    @Value("${statement.routing-key}")
    private String statementRoutingKey;

    @Value("${account.queue}")
    private String ACCOUNT_QUEUE;
    @Value("${account.exchange}")
    private String accountExchange;
    @Value("${account.routing-key}")
    private String accountRoutingKey;


    @Bean
    public Queue transferQueue() {
        return new Queue(TRANSFER_QUEUE, true);
    }

    @Bean
    public Queue withdrawQueue() {
        return new Queue(WITHDRAW_QUEUE, true);
    }

    @Bean
    public Queue depositQueue() {
        return new Queue(DEPOSIT_QUEUE, true);
    }

    @Bean
    public Queue statementQueue() {
        return new Queue(STATEMENT_QUEUE, true);
    }

    @Bean
    public Queue accountQueue() {
        return new Queue(ACCOUNT_QUEUE, true);
    }



    @Bean
    public TopicExchange transferExchange() {
        return new TopicExchange(transferExchange);
    }

    @Bean
    public TopicExchange withdrawExchange() {
        return new TopicExchange(withdrawExchange);
    }

    @Bean
    public TopicExchange depositExchange() {
        return new TopicExchange(depositExchange);
    }

    @Bean
    public TopicExchange statementExchange() {
        return new TopicExchange(statementExchange);
    }

    @Bean
    public TopicExchange accountExchange() {
        return new TopicExchange(accountExchange);
    }


    @Bean
    public Binding bindingForTransfer(Queue transferQueue, TopicExchange transferExchange) {
        return BindingBuilder.bind(transferQueue).to(transferExchange).with(transferRoutingKey);
    }

    @Bean
    public Binding bindingForWithdraw(Queue withdrawQueue, TopicExchange withdrawExchange) {
        return BindingBuilder.bind(withdrawQueue).to(withdrawExchange).with(withdrawRoutingKey);
    }

    @Bean
    public Binding bindingForDeposit(Queue depositQueue, TopicExchange depositExchange) {
        return BindingBuilder.bind(depositQueue).to(depositExchange).with(depositRoutingKey);
    }

    @Bean
    public Binding bindingForStatement(Queue statementQueue, TopicExchange statementExchange) {
        return BindingBuilder.bind(statementQueue).to(statementExchange).with(statementRoutingKey);
    }

    @Bean
    public Binding bindingForAccount(Queue accountQueue, TopicExchange accountExchange) {
        return BindingBuilder.bind(accountQueue).to(accountExchange).with(accountRoutingKey);
    }


    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

}
