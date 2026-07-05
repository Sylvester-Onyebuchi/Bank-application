package com.sylvester.bankapp.rabbitmq;


import com.sylvester.bankapp.rabbitmq.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Publisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${transfer.exchange}")
    private String transferExchange;
    @Value("${transfer.routing-key}")
    private String transferRoutingKey;

    @Value("${withdraw.exchange}")
    private String withdrawExchange;
    @Value("${withdraw.routing-key}")
    private String withdrawRoutingKey;

    @Value("${deposit.exchange}")
    private String depositExchange;
    @Value("${deposit.routing-key}")
    private String depositRoutingKey;

    @Value("${statement.exchange}")
    private String statementExchange;
    @Value("${statement.routing-key}")
    private String statementRoutingKey;

    @Value("${account.exchange}")
    private String accountExchange;
    @Value("${account.routing-key}")
    private String accountRoutingKey;


    public void publishTransfer(TransferEvent event){
        rabbitTemplate.convertAndSend(transferExchange, transferRoutingKey, event);
        log.info("transfer message sent to queue");
    }

    public void publishWithdraw(WithDrawEvent event){
        rabbitTemplate.convertAndSend(withdrawExchange, withdrawRoutingKey, event);
    }

    public void publishDeposit(DepositEvent event){
        rabbitTemplate.convertAndSend(depositExchange, depositRoutingKey, event);
    }

    public void publishStatement(StatementEvent event){
        rabbitTemplate.convertAndSend(statementExchange, statementRoutingKey, event);
    }

    public void publishAccount(AccountEvent event){
        rabbitTemplate.convertAndSend(accountExchange, accountRoutingKey, event);
    }

}
