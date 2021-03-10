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
