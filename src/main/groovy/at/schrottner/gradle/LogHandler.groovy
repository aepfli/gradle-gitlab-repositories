package at.schrottner.gradle

import org.slf4j.LoggerFactory

trait LogHandler {
	public static final String LOG_PREFIX = "GitLab Repositories"
	static org.slf4j.Logger logger
	String logPrefix

	def getLogger() {
		logger ?: LoggerFactory.getLogger(GitlabRepositoriesExtension.class)
	}

	def getLogPrefix() {
		"$LOG_PREFIX :: $logPrefix ::"
	}
}