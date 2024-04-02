package com.example.app.exception;

//@RestControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(ConstraintViolationException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @ResponseBody
//    ValidationErrorResponse onConstraintValidationException(ConstraintViolationException e) {
//        return ValidationErrorResponse.builder()
//                .violations(e.getConstraintViolations()
//                        .stream()
//                        .map(violation -> Violation.builder()
//                                .fieldName(violation.getPropertyPath().toString())
//                                .message(violation.getMessage())
//                                .build())
//                        .collect(Collectors.toList()))
//                .build();
//    }
//
//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    @ResponseBody
//    ValidationErrorResponse onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
//        return ValidationErrorResponse.builder()
//                .violations(e.getBindingResult().getFieldErrors()
//                        .stream()
//                        .map(error -> Violation.builder()
//                                .fieldName(error.getField())
//                                .message(error.getDefaultMessage())
//                                .build())
//                        .collect(Collectors.toList()))
//                .build();
//    }
}
