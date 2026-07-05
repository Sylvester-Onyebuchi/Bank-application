package com.sylvester.bankapp.transaction.controller;


import com.sylvester.bankapp.transaction.dto.BankStatementRequest;
import com.sylvester.bankapp.transaction.dto.TransferRequest;
import com.sylvester.bankapp.transaction.dto.DepositAndWithdrawRequest;
import com.sylvester.bankapp.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;


    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request, @RequestHeader("idempotency-key")  String idempotencyKey, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");
        transactionService.transfer(request, idempotencyKey, userId);
        return ResponseEntity.ok("Transfer successful");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody DepositAndWithdrawRequest  request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");
        transactionService.withDrawMoney(request, userId);
        return ResponseEntity.ok("Withdraw successful");
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> transfer(@RequestBody DepositAndWithdrawRequest request, @AuthenticationPrincipal Jwt jwt){
        String userId = jwt.getClaimAsString("sub");
        transactionService.depositMoney(request,userId);
        return ResponseEntity.ok("Deposit successful");
    }

    @GetMapping("/receipt/{transactionId}")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable String transactionId){
        byte[] receipt = transactionService.getReceipt(transactionId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+
                transactionId + ".pdf")
                .body(receipt);
    }

    @PostMapping("/statement")
    public ResponseEntity<?> statement(@RequestBody BankStatementRequest request, @AuthenticationPrincipal Jwt jwt) throws Exception {
        String userId = jwt.getClaimAsString("sub");
        transactionService.statementOfAccount(request.accountNumber(),request.startDate(), request.endDate(),userId);
        return ResponseEntity.ok("Statement successful sent");
    }

    @GetMapping("/me/{accountId}")
    public ResponseEntity<?> findAllByAccountNumber(@PathVariable Long accountId, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(transactionService.getLastTenTransactions(jwt.getSubject(), accountId));
    }



}
