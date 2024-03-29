/*
 * Copyright 2016-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
package at.schrottner.gradle.auths

/**
 * TODO:
 * 	- rework tokens - this subclassing is ridiculous and should be reflected by a enum
 */
public class Token {
	GitLabTokenType type
	String value
	String key

	Token(GitLabTokenType type) {
		this.type = type
	}
}