package org.zaproxy.zapmavenplugin;

import org.apache.maven.plugins.annotations.Parameter;

public class Context {

	/**
	 * Name for this context
	 */
	@Parameter(defaultValue = "")
	private String contextName;

	/**
	 * string to set the authentication method
	 */
	@Parameter(defaultValue = "formBasedAuthentication")
	private String contextAuthenticationMethod;

	/**
	 * A regex to determine if the session has logged out
	 */
	@Parameter(defaultValue = "")
	private String contextLoggedOutIndicator;

	/**
	 * The url to log in
	 */
	@Parameter(defaultValue = "")
	private String contextAuthenticationLoginUrl;

	/**
	 * The authentication name to be used in this context
	 */
	@Parameter(defaultValue = "username")
	private String contextAuthenticationLoginUsernameTag;

	/**
	 * The authentication password to be used in this context
	 */
	@Parameter(defaultValue = "password")
	private String contextAuthenticationLoginPasswordTag;
	
	/**
	 * A Regex to determine what will be included in this context
	 */
	@Parameter(defaultValue = "")
	private String contextIncludeRegex;
	
	/**
	 *  A Regex to determine what will be excluded from this context
	 */
	@Parameter(defaultValue = "")
	private String contextExcludeRegex;

	/**
	 * A Id for this context
	 */
	private int contextId;

	public String getContextName() {
		return contextName;
	}

	public void setContextName(String contextName) {
		this.contextName = contextName;
	}

	public String getContextAuthenticationMethod() {
		return contextAuthenticationMethod;
	}

	public void setContextAuthenticationMethod(
			String contextAuthenticationMethod) {
		this.contextAuthenticationMethod = contextAuthenticationMethod;
	}

	public String getContextLoggedOutIndicator() {
		return contextLoggedOutIndicator;
	}

	public void setContextLoggedOutIndicator(String contextLoggedOutIndicator) {
		this.contextLoggedOutIndicator = contextLoggedOutIndicator;
	}

	public int getContextId() {
		return contextId;
	}

	public void setContextId(int contextId) {
		this.contextId = contextId;
	}

	public String getContextAuthenticationLoginUrl() {
		return contextAuthenticationLoginUrl;
	}

	public void setContextAuthenticationLoginUrl(
			String contextAuthenticationLoginUrl) {
		this.contextAuthenticationLoginUrl = contextAuthenticationLoginUrl;
	}

	public String getContextAuthenticationLoginUsernameTag() {
		return contextAuthenticationLoginUsernameTag;
	}

	public void setContextAuthenticationLoginUsernameTag(
			String contextAuthenticationLoginUsernameTag) {
		this.contextAuthenticationLoginUsernameTag = contextAuthenticationLoginUsernameTag;
	}

	public String getContextAuthenticationLoginPasswordTag() {
		return contextAuthenticationLoginPasswordTag;
	}

	public void setContextAuthenticationLoginPasswordTag(
			String contextAuthenticationLoginPasswordTag) {
		this.contextAuthenticationLoginPasswordTag = contextAuthenticationLoginPasswordTag;
	}

	public String getContextIncludeRegex() {
		return contextIncludeRegex;
	}

	public void setContextIncludeRegex(String contextIncludeRegex) {
		this.contextIncludeRegex = contextIncludeRegex;
	}

	public String getContextExcludeRegex() {
		return contextExcludeRegex;
	}

	public void setContextExcludeRegex(String contextExcludeRegex) {
		this.contextExcludeRegex = contextExcludeRegex;
	}

}
