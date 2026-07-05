package com.sylvester.bankapp.beneficiary.service;

import com.sylvester.bankapp.account.repository.AccountRepository;
import com.sylvester.bankapp.beneficiary.dto.BeneficiaryRequest;
import com.sylvester.bankapp.beneficiary.entity.Beneficiary;
import com.sylvester.bankapp.beneficiary.repository.BeneficiaryRepository;
import com.sylvester.bankapp.exception.AlreadyExistException;
import com.sylvester.bankapp.exception.NotFoundException;
import com.sylvester.bankapp.user.entity.User;
import com.sylvester.bankapp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    @Test
    void addBeneficiarySavesBeneficiaryForUserWithAccount() {
        User user = User.builder().id("user-123").build();
        BeneficiaryRequest request = new BeneficiaryRequest("Rent", "HR123");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(beneficiaryRepository.existsByNameAndBeneficiaryOwner_Id("Rent", "user-123")).thenReturn(false);
        when(accountRepository.existsByOwner_Id("user-123")).thenReturn(true);

        beneficiaryService.addBeneficiary(request, "user-123");

        ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(beneficiaryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Rent");
        assertThat(captor.getValue().getAccountNumber()).isEqualTo("HR123");
        assertThat(captor.getValue().getBeneficiaryOwner()).isEqualTo(user);
    }

    @Test
    void addBeneficiaryRejectsDuplicateNameAndUserWithoutAccount() {
        User user = User.builder().id("user-123").build();
        BeneficiaryRequest request = new BeneficiaryRequest("Rent", "HR123");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(beneficiaryRepository.existsByNameAndBeneficiaryOwner_Id("Rent", "user-123")).thenReturn(true);

        assertThatThrownBy(() -> beneficiaryService.addBeneficiary(request, "user-123"))
                .isInstanceOf(AlreadyExistException.class)
                .hasMessage("Beneficiary with name Rent already exists");

        when(beneficiaryRepository.existsByNameAndBeneficiaryOwner_Id("Rent", "user-123")).thenReturn(false);
        when(accountRepository.existsByOwner_Id("user-123")).thenReturn(false);
        assertThatThrownBy(() -> beneficiaryService.addBeneficiary(request, "user-123"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("A user without an account cannot have beneficiary");
    }

    @Test
    void removeAndUpdateBeneficiaryMutateOwnedBeneficiary() {
        Beneficiary beneficiary = Beneficiary.builder().id(5L).name("Old").accountNumber("HR1").build();
        when(beneficiaryRepository.findByIdAndBeneficiaryOwner_Id(5L, "user-123")).thenReturn(Optional.of(beneficiary));
        when(beneficiaryRepository.findByNameAndBeneficiaryOwner_Id("New", "user-123")).thenReturn(Optional.of(beneficiary));

        beneficiaryService.removeFromBeneficiary(5L, "user-123");
        beneficiaryService.updateBeneficiary(new BeneficiaryRequest("New", "HR2"), "user-123");

        verify(beneficiaryRepository).delete(beneficiary);
        verify(beneficiaryRepository).save(beneficiary);
        assertThat(beneficiary.getName()).isEqualTo("New");
        assertThat(beneficiary.getAccountNumber()).isEqualTo("HR2");
    }
}
