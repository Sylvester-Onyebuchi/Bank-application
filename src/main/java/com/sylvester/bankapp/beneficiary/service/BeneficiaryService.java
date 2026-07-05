package com.sylvester.bankapp.beneficiary.service;


import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.beneficiary.dto.BeneficiaryRequest;
import com.sylvester.bankapp.beneficiary.dto.BeneficiaryResponse;
import com.sylvester.bankapp.beneficiary.entity.Beneficiary;
import com.sylvester.bankapp.beneficiary.repository.BeneficiaryRepository;
import com.sylvester.bankapp.exception.AlreadyExistException;
import com.sylvester.bankapp.exception.NotFoundException;
import com.sylvester.bankapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;


    @Transactional
    public void addBeneficiary(BeneficiaryRequest request, String userId) {

        var user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User not found"));
        if (beneficiaryRepository.existsByNameAndBeneficiaryOwner_Id(request.name(), user.getId())) {
            throw new AlreadyExistException("Beneficiary with name " + request.name() + " already exists");
        }

        if (!accountRepository.existsByOwner_Id(userId)){
            throw new NotFoundException("A user without an account cannot have beneficiary");
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .name(request.name())
                .accountNumber(request.accountNumber())
                .beneficiaryOwner(user)
                .build();
        beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary with name " + request.name() + " has been saved");

    }

    @Transactional
    public void removeFromBeneficiary(Long beneficiaryId, String userId) {
        var beneficiary = beneficiaryRepository.findByIdAndBeneficiaryOwner_Id(beneficiaryId, userId).orElseThrow(
                ()-> new NotFoundException("Beneficiary does not exist")
        );
        beneficiaryRepository.delete(beneficiary);
        log.info("Beneficiary with name " + beneficiary.getName() + " has been removed");
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getAllBeneficiaries(String userId) {

        return beneficiaryRepository.getAllByBeneficiaryOwner_Id(userId, PageRequest.of(0,20)).stream()
                .map(this::mapToBeneficiaryResponse)
                .toList();

    }

    public BeneficiaryResponse getBeneficiary(Long beneficiaryId, String userId) {

        var beneficiary = beneficiaryRepository.findByIdAndBeneficiaryOwner_Id(beneficiaryId, userId).orElseThrow(
                ()-> new NotFoundException("Beneficiary does not exist")
        );
        return new BeneficiaryResponse(
                beneficiary.getId(), beneficiary.getName(), beneficiary.getAccountNumber()
        );
    }

    @Transactional
    public void updateBeneficiary(BeneficiaryRequest request, String userId) {

        var beneficiary = beneficiaryRepository.findByNameAndBeneficiaryOwner_Id(request.name(), userId).orElseThrow(
                ()-> new NotFoundException("Beneficiary does not exist")
        );
        if (!request.name().isEmpty() || !request.accountNumber().isEmpty()) {

            beneficiary.setName(request.name());
            beneficiary.setAccountNumber(request.accountNumber());
        }
       beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary has been updated");
    }

    private BeneficiaryResponse mapToBeneficiaryResponse(Beneficiary beneficiary) {

        return new BeneficiaryResponse(
                beneficiary.getId(), beneficiary.getName(), beneficiary.getAccountNumber()
        );
    }
}
