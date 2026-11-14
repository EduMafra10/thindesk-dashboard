package br.com.thindesk.dashboard.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class TratadorGlobalDeErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalDeErros.class);
    private static final HttpStatusCode REGRA_DE_NEGOCIO = HttpStatusCode.valueOf(422);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail tratarRegraNegocio(RegraNegocioException ex) {
        return problema(REGRA_DE_NEGOCIO, "Regra de negócio violada", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErroInesperado(Exception ex) {
        log.error("Erro inesperado ao processar a requisição", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<CampoInvalido> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new CampoInvalido(erro.getField(), mensagemDe(erro)))
                .toList();
        return handleExceptionInternal(ex, problemaDeValidacao(status, erros), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<CampoInvalido> erros = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(erro -> new CampoInvalido(nomeDoCampo(resultado, erro), erro.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(ex, problemaDeValidacao(status, erros), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = problema(status, "Requisição inválida",
                "O corpo da requisição está ausente ou em formato inválido.");
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        CampoInvalido erro = new CampoInvalido(ex.getPropertyName(), "Valor inválido para o campo.");
        return handleExceptionInternal(ex, problemaDeValidacao(status, List.of(erro)), headers, status, request);
    }

    private static ProblemDetail problemaDeValidacao(HttpStatusCode status, List<CampoInvalido> erros) {
        ProblemDetail problema = problema(status, "Dados inválidos", "Um ou mais campos estão inválidos.");
        problema.setProperty("erros", erros);
        return problema;
    }

    private static ProblemDetail problema(HttpStatusCode status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("timestamp", Instant.now());
        return problema;
    }

    private static String mensagemDe(FieldError erro) {
        return erro.isBindingFailure() ? "Valor inválido para o campo." : erro.getDefaultMessage();
    }

    private static String nomeDoCampo(ParameterValidationResult resultado, MessageSourceResolvable erro) {
        return erro instanceof FieldError campo
                ? campo.getField()
                : resultado.getMethodParameter().getParameterName();
    }

    public record CampoInvalido(String campo, String mensagem) {
    }
}