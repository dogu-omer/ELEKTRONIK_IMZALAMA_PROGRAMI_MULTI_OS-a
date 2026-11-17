package io.github.doguomer.esign.validation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Tek bir imzanın doğrulama sonucu. Result for one signature.
 *
 * @param indication     ETSI göstergesi (TOTAL_PASSED, INDETERMINATE, TOTAL_FAILED)
 * @param subIndication  ayrıntı (ör. NO_CERTIFICATE_CHAIN_FOUND, HASH_FAILURE, REVOKED_NO_POE)
 * @param format         bulunan seviye (ör. CAdES-BASELINE-T)
 * @param claimedSigningTime imzacının beyan ettiği zaman (kanıt değildir)
 * @param timestampTime  zaman damgasıyla kanıtlanan en erken zaman (varsa)
 */
public record SignatureResult(
        String id,
        Verdict verdict,
        String indication,
        Optional<String> subIndication,
        String signedBy,
        String format,
        Optional<Instant> claimedSigningTime,
        Optional<Instant> timestampTime,
        int timestampCount,
        List<String> errors,
        List<String> warnings) {}
