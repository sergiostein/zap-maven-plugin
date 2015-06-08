package org.zaproxy.zapmavenplugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ApiResponseList;
import org.zaproxy.clientapi.core.ApiResponseSet;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;
import org.zaproxy.clientapi.gen.Users;

/**
 * Goal which will start ZAP proxy.
 */
@Mojo(name = "start-zap", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class StartZAP extends AbstractMojo
{
    /**
     * API KEY.
     */
    @Parameter(defaultValue = "ZAP-MAVEN-PLUGIN")
    public String   apiKey;

    /**
     * Location of the ZAProxy program.
     */
    @Parameter(required = true)
    private String  zapProgram;

    /**
     * Location of the host of the ZAP proxy
     */
    @Parameter(defaultValue = "localhost", required = true)
    private String  zapProxyHost;

    /**
     * Location of the port of the ZAP proxy
     */
    @Parameter(defaultValue = "8090", required = true)
    private int     zapProxyPort;

    /**
     * New session when you don't want to start ZAProxy.
     */
    @Parameter(defaultValue = "false")
    private boolean newSession;

    /**
     * Sleep to wait to start ZAProxy
     */
    @Parameter(defaultValue = "4000")
    private int     zapSleep;

    /**
     * Set the plugin to skip its execution.
     */
    @Parameter(defaultValue = "false")
    private boolean skip;
    
	
	/**
	 * An optional defined directory for this session
	 */
	@Parameter (required = true, defaultValue = "/")
	private String sessionDirectory;
	

	/**
	 * An optional name for this session
	 */
	@Parameter(required = true, defaultValue = "zapSession")
	private String sessionName;
	

	/**
	 * Contexts to be used
	 */
	@Parameter(defaultValue = "")
	private List<Context> contexts;
	

	/**
	 * List of users for different logins
	 */
	@Parameter(defaultValue = "")
	private List<ZAPUser> zapUsers;

	/**
	 * URL Test Target URL
	 */
	@Parameter(required = true)
	private String targetURL;


    // @Override
    public void execute() throws MojoExecutionException, MojoFailureException
 {
		if (skip) {
			getLog().info("Skipping zap execution");
			return;
		}
		try {
			if (newSession) {
				startNewSessionOnRunningClient();
			} else {
				final Process ps = startZap();
				logZapProcess(ps);
			}
			Thread.currentThread();
			Thread.sleep(zapSleep);
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("Unable to start ZAP ["
					+ zapProgram + "]");
		}

	}

    protected Runtime getRuntime()
    {
        Runtime runtime = java.lang.Runtime.getRuntime();
        return runtime;
    }
    
    private String getDate() {
		SimpleDateFormat dt = new SimpleDateFormat("yyyyy-mm-dd_hh-mm-ss");
		Date date = new Date();
		return dt.format(date);
	}

    protected ClientApi getZapClient()
    {
        return new ClientApi(zapProxyHost, zapProxyPort);
    }
    
	private String getAuthencicationParams(Context context) {

		return "loginUrl=" + targetURL + "&"
				+ context.getContextAuthenticationLoginUrl()
				+ "&loginRequestData="
				+ context.getContextAuthenticationLoginUsernameTag()
				+ "&loginRequestData="
				+ context.getContextAuthenticationLoginUsernameTag()
				+ "={%username%}"
				+ context.getContextAuthenticationLoginPasswordTag()
				+ "={%password%}";
	}

	private int getContextId(ClientApi zapClient, String contextName)
			throws ClientApiException {
		String contexts = ((ApiResponseElement) zapClient.context.contextList())
				.getValue();
		if (contexts.contains(contextName)) {
			List<String> contextArray = Arrays.asList(contexts.replaceAll(
					"[\\[\\] ]", "").split(","));
			return contextArray.indexOf(contextName) + 1;
		}
		return 0;
	}

    private void startNewSessionOnRunningClient() throws IOException, ClientApiException, MojoExecutionException
    {
    	try {
    		ClientApi zapClient = getZapClient();
			zapClient.core.newSession(apiKey, sessionDirectory
					+ sessionName + getDate(), "true");
			int id = 1;
			if (null != contexts && !contexts.isEmpty()) {
				getLog().info("Create new Contexts");
				for (Context context : contexts) {
					id++;
					String authenticationParams = getAuthencicationParams(context);
					zapClient.context.newContext(apiKey,
							context.getContextName());
					zapClient.authentication.setAuthenticationMethod(
							apiKey, String.valueOf(id),
							context.getContextAuthenticationMethod(),
							authenticationParams);
					zapClient.authentication.setLoggedOutIndicator(apiKey,
							String.valueOf(id),
							context.getContextLoggedOutIndicator());
					context.setContextId(getContextId(zapClient,
							context.getContextName()));
					if (null != context.getContextIncludeRegex()
							&& !context.getContextIncludeRegex().isEmpty()) {
						zapClient.context.includeInContext(apiKey,
								context.getContextName(),
								context.getContextIncludeRegex());
					}
					if (null != context.getContextExcludeRegex()
							&& !context.getContextExcludeRegex().isEmpty()) {
						zapClient.context.excludeFromContext(apiKey,
								context.getContextName(),
								context.getContextExcludeRegex());
					}
				}
				if (null != zapUsers && !zapUsers.isEmpty()) {
					getLog().info("Create new Users");
					Users users = zapClient.users;
					for (ZAPUser zapUser : zapUsers) {
						String contextId = String.valueOf(getContextId(
								zapClient, zapUser.getUserContext()));
						users.newUser(apiKey, contextId, zapUser.getUser());
						String userId = ((ApiResponseSet) (((ApiResponseList) users
								.usersList(contextId)).getItems().get(0)))
								.getAttribute("id");
						users.setUserEnabled(apiKey, contextId, userId,
								"true");
						users.setAuthenticationCredentials(apiKey,
								contextId, userId, zapUser.getUserParams());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException("Unable to start new Session");
		}
    }

    private Process startZap() throws IOException
    {
        File pf = new File(zapProgram);
        Runtime runtime = getRuntime();
        getLog().info("Start ZAProxy [" + zapProgram + "]");
        getLog().info("Using working directory [" + pf.getParentFile().getPath() + "]");
        final Process ps = runtime.exec(zapProgram, null, pf.getParentFile());
        return ps;
    }

    private void logZapProcess(final Process ps)
    {
        logNormalOutput(ps);

        logErrorOutput(ps);
    }

    private void logErrorOutput(final Process ps)
    {
        // Consommation de la sortie standard de l'application externe dans un
        // Thread separe
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    if (ps != null)
                    {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getErrorStream()));
                        String line = "";
                        try
                        {
                            while ((line = reader.readLine()) != null)
                            {
                                // Traitement du flux de sortie de l'application si
                                // besoin est
                                getLog().info(line);
                            }
                        } finally
                        {
                            reader.close();
                        }
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void logNormalOutput(final Process ps)
    {
        // Consommation de la sortie d'erreur de l'application externe dans un
        // Thread separe
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    if (ps != null)
                    {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
                        String line = "";
                        try
                        {
                            while ((line = reader.readLine()) != null)
                            {
                                // Traitement du flux d'erreur de l'application si
                                // besoin est
                                getLog().info(line);
                            }
                        } finally
                        {
                            reader.close();
                        }
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
