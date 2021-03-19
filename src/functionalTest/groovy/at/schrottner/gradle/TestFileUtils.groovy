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

class TestFileUtils {
	static File getTestResource(String filePath, String resourcePath) {
		File file = new File(filePath)
		getTestResource(file, resourcePath)
	}

	static File getTestResource(File file, String resourcePath) {
		ClassLoader.getSystemClassLoader().getResource(resourcePath).withInputStream { inputStream ->
			file.withOutputStream { outputStream ->
				outputStream << inputStream
			}
		}
		file
	}
}
