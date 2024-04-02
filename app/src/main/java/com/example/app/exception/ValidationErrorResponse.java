package com.example.app.exception;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ValidationErrorResponse {
	private final List<Violation> violations;
	private final String message;
	private final String trace;
}
