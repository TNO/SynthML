package com.github.tno.pokayoke.uml.profile.cif;

import org.eclipse.escet.common.java.TextPosition;

public class TypeException extends RuntimeException {
	private static final long serialVersionUID = 6860730254242254224L;

	private final TextPosition position;

	/**
	 * @param message
	 */
	public TypeException(String message) {
		this(message, null);
	}

	/**
	 * @param message
	 */
	public TypeException(String message, TextPosition position) {
		super(message);
		this.position = position;
	}

	/**
	 * @return the position
	 */
	public TextPosition getPosition() {
		return position;
	}

	@Override
	public String getMessage() {
		if (position == null) {
			return super.getMessage();
		}
		return String.format("Type error at line %d, column %d: %s", position.startLine, position.startColumn,
				super.getMessage());
	}
}
