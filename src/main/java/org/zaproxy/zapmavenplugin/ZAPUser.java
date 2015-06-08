package org.zaproxy.zapmavenplugin;

import org.apache.maven.plugins.annotations.Parameter;

public class ZAPUser {

	/**
	 * User name to identify in OWASP ZAP.
	 */
	@Parameter(defaultValue = "")
	private String user;

	/**
	 * The context to be used
	 */
	private String userContext;

	/**
	 * User name to use on log in (OWASP ZAP Application)
	 */
	@Parameter(defaultValue = "")
	private String userName;

	/**
	 * User password to use on log in (OWASP ZAP Application)
	 */
	@Parameter(defaultValue = "")
	private String userPassword;

	public String getUser() {
		return user;
	}

	public void setUser(String userName) {
		this.user = userName;
	}

	public String getUserContext() {
		return userContext;
	}

	public void setUserContext(String userContext) {
		this.userContext = userContext;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserPassword() {
		return userPassword;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public String getUserParams() {
		return "username="+this.getUserName()+"&password="+this.getUserPassword();
	}

}
