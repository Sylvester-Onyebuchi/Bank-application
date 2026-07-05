package com.sylvester.bankapp.beneficiary.controller;




import com.sylvester.bankapp.beneficiary.dto.BeneficiaryRequest;
import com.sylvester.bankapp.beneficiary.dto.BeneficiaryResponse;
import com.sylvester.bankapp.beneficiary.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping("/add-beneficiary")
    public ResponseEntity<?> addBeneficiary(@Valid @RequestBody BeneficiaryRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        beneficiaryService.addBeneficiary(request,userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBeneficiary(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        beneficiaryService.removeFromBeneficiary(id,userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owner/all")
    public ResponseEntity<List<BeneficiaryResponse>> getBeneficiariesOfUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(beneficiaryService.getAllBeneficiaries(jwt.getSubject()));
    }

    @GetMapping("/beneficiary/{id}")
    public ResponseEntity<BeneficiaryResponse> getBeneficiary(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        BeneficiaryResponse response = beneficiaryService.getBeneficiary(id, jwt.getSubject());
        return ResponseEntity.ok(response);
    }

}
