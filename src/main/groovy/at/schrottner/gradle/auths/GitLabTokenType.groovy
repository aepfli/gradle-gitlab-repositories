/*
 * Copyright 2016-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
package at.schrottner.gradle.auths;

enum GitLabTokenType {
	JOB("job", "Job-Token"),
	PRIVATE("private", "Private-Token"),
	DEPLOY("deploy", "Deploy-Token"),
	NO_VALUE("no value", "NO-VALUE")

	String name
	String headerName

	GitLabTokenType(String name, String headerName) {
		this.name = name
		this.headerName = headerName
	}

	@Override
	String toString() {
		return headerName
	}
}
