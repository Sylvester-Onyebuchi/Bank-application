package com.sylvester.bankapp.beneficiary.repository;

import com.sylvester.bankapp.beneficiary.entity.Beneficiary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    boolean existsByNameAndBeneficiaryOwner_Id(String name, String beneficiaryOwnerId);

    Optional<Beneficiary> findByIdAndBeneficiaryOwner_Id(Long id, String beneficiaryOwnerId);

    Optional<Beneficiary> findByNameAndBeneficiaryOwner_Id(String name, String beneficiaryOwnerId);

    List<Beneficiary> getAllByBeneficiaryOwner_Id(String id, Pageable pageable);
}
