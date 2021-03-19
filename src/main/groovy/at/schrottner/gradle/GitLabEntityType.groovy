/*
 * Copyright 2016-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */
package at.schrottner.gradle

enum GitLabEntityType {
	GROUP("Group", "groups"),
	PROJECT("Project", "projects")

	String name
	String endpoint

	GitLabEntityType(String name, String endpoint) {
		this.name = name
		this.endpoint = endpoint
	}

	@Override
	String toString() {
		return name
	}
}