package com.example.app.exception;

import lombok.Builder;

@Builder
public class Violation {
	private final String fieldName;
	private final String message;

}
