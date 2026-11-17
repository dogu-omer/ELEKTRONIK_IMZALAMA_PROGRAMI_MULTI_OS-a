package io.github.doguomer.esign.api;

import io.github.doguomer.esign.core.TokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Hataları RFC 9457 problem+json olarak döndürür; iç ayrıntı (yığın izi) sızdırılmaz. */
@RestControllerAdvice
class ApiErrors {

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestPartException.class})
    ProblemDetail badRequest(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RemoteSigningService.SessionNotFoundException.class)
    ProblemDetail notFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(SessionStore.TooManySessionsException.class)
    ProblemDetail tooMany(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "Dosya çok büyük");
    }

    @ExceptionHandler(TokenException.class)
    ProblemDetail unprocessable(TokenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    /**
     * Beklenmeyen hata: istemciye ayrıntı verilmez. Log'a yalnızca hata sınıfı ve bir izleme kimliği yazılır;
     * belge içeriği, sertifika sahibi gibi kişisel veriler loglanmaz (KVKK/GDPR).
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail internal(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse er) { // Spring'in 4xx hataları (ör. bozuk JSON)
            return er.getBody();
        }
        if (e instanceof org.springframework.http.converter.HttpMessageNotReadableException
                || e instanceof org.springframework.beans.TypeMismatchException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "İstek gövdesi veya parametre geçersiz");
        }
        String ref = java.util.UUID.randomUUID().toString();
        LOG.error("İç hata ref={} type={}", ref, e.getClass().getName());
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Beklenmeyen hata / internal error (ref " + ref + ")");
        return pd;
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ApiErrors.class);
}
