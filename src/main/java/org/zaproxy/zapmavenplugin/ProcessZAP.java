package org.zaproxy.zapmavenplugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "process-zap", defaultPhase = LifecyclePhase.INTEGRATION_TEST, threadSafe = true)
public class ProcessZAP extends AbstractMojo
{
    private static final String NONE_FORMAT = "none";

    private static final String JSON_FORMAT = "json";

    private static final String HTML_FORMAT = "html";

    private static final String XML_FORMAT  = "xml";

    private ClientApi           zapClientAPI;
    private Proxy               proxy;

    /**
     * API token of the ZAP proxy
     * Change this if you have set the apikey in ZAP via Options / API
     */
    @Parameter(defaultValue = "ZAP-MAVEN-PLUGIN")
    public String               apiKey;

    /**
     * Location of the host of the ZAP proxy
     */
    @Parameter(defaultValue = "localhost", required = true)
    private String              zapProxyHost;

    /**
     * Location of the port of the ZAP proxy
     */
    @Parameter(defaultValue = "8090", required = true)
    private int                 zapProxyPort;

    /**
     * Location of the port of the ZAP proxy
     */
    @Parameter(required = true)
    private String              targetURL;

    /**
     * Switch to spider the URL
     */
    @Parameter(defaultValue = "true")
    private boolean             spiderURL;

    /**
     * Switch to scan the URL
     */
    @Parameter(defaultValue = "true")
    private boolean             scanURL;

    /**
     * Save session of scan
     */
    @Parameter(defaultValue = "true")
    private boolean             saveSession;

    /**
     * Switch to shutdown ZAP
     */
    @Parameter(defaultValue = "true")
    private boolean             shutdownZAP;

    /**
     * Save session of scan
     */
    @Parameter(defaultValue = "true")
    private boolean             reportAlerts;

    /**
     * Location to store the ZAP reports
     */
    @Parameter(defaultValue = "${project.build.directory}/zap-reports")
    private String              reportsDirectory;

    /**
     * Set the output format type, in addition to the XML report. Must be one of "none", "json", "xml", "html".
     */
    @Parameter(defaultValue = "none")
    private String              format;

    /**
     * Set the plugin to skip its execution.
     */
    @Parameter(defaultValue = "false")
    private boolean             skip;

    /**
     * create a Timestamp
     *
     * @return
     */
    private String dateTimeString()
    {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(cal.getTime());
    }

    /**
     * create a temporary filename
     *
     * @param prefix
     * if null, then default "temp"
     * @param suffix
     * if null, then default ".tmp"
     * @return
     */
    private String createTempFilename(String prefix, String suffix)
    {
        StringBuilder sb = new StringBuilder("");
        if (prefix != null)
            sb.append(prefix);
        else
            sb.append("temp");

        // append date time and random UUID
        sb.append(dateTimeString()).append("_").append(UUID.randomUUID().toString());

        if (suffix != null)
            sb.append(suffix);
        else
            sb.append(".tmp");

        return sb.toString();
    }

    /**
     * Change the ZAP API status response to an integer
     *
     * @param response the ZAP APIresponse code
     * @return
     */
    private int statusToInt(ApiResponse response)
    {
        return Integer.parseInt(((ApiResponseElement) response).getValue());
    }

    /**
     * Search for all links and pages on the URL
     *
     * @param url the to investigate URL
     * @throws ClientApiException
     */
    private void spiderURL(String url) throws ClientApiException
    {

        try
        {
            ApiResponse resp = zapClientAPI.spider.scan(apiKey, url, null);

            // The scan now returns a scan id to support concurrent scanning
            String scanid = ((ApiResponseElement) resp).getValue();

            int progress;

            // Poll the status until it completes
            while (true)
            {
                Thread.sleep(1000);
                progress = statusToInt((ApiResponseElement) zapClientAPI.spider.status(scanid));
                getLog().info("Spider progress : " + progress + "%");
                if (progress >= 100)
                {
                    break;
                }
            }
            getLog().info("Spider complete");

            // Give the passive scanner a chance to complete
            Thread.sleep(2000);

            while (statusToInt(zapClientAPI.spider.status(scanid)) < 100)
            {
                try
                {
                    Thread.sleep(1000);
                } catch (InterruptedException e)
                {

                }
            }

        } catch (Exception e)
        {
            getLog().error("Exception : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Scan all pages found at url
     *
     * @param url the url to scan
     * @throws ClientApiException
     */
    private void scanURL(String url) throws ClientApiException
    {

        try
        {
            ApiResponse resp = zapClientAPI.ascan.scan(apiKey, url, "true", "false", "");

            // The scan now returns a scan id to support concurrent scanning
            String scanid = ((ApiResponseElement) resp).getValue();

            /*
             * if (!"OK".equals(((ApiResponseElement) resp).getValue()))
             * {
             * System.out.println("Failed to Active Scan target : " + resp.toString(0));
             * return;
             * }
             */

            int progress;

            // Poll the status until it completes
            while (true)
            {
                Thread.sleep(5000);
                progress = statusToInt((ApiResponseElement) zapClientAPI.ascan.status(scanid));
                System.out.println("Active Scan progress : " + progress + "%");
                if (progress >= 100)
                {
                    break;
                }
            }

            getLog().info("Active Scan complete");

            getLog().info("Alerts:");
            getLog().info(new String(zapClientAPI.core.xmlreport(apiKey)));
        } catch (Exception e)
        {
            getLog().error("Exception : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get all alerts from ZAP proxy
     *
     * @param json true for json form, false for xml format
     * @return all alerts from ZAProxy
     * @throws Exception
     */
    private String getAllAlerts(final String format) throws Exception
    {
        URL url;
        String result = "";

        // String url_base = "http://" + zapProxyHost + ":" + zapProxyPort;
        String url_base = "http://zap";

        if (format.equalsIgnoreCase(XML_FORMAT) || format.equalsIgnoreCase(HTML_FORMAT) || format.equalsIgnoreCase(JSON_FORMAT))
        {
            url = new URL(url_base + "/" + format + "/core/view/alerts");
        } else
        {
            url = new URL(url_base + "/xml/core/view/alerts");
        }

        getLog().info("Open URL: " + url.toString());

        final HttpURLConnection uc = (HttpURLConnection) url.openConnection(proxy);
        uc.connect();

        final BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null)
        {
            result = result + inputLine;
        }

        in.close();
        return result;

    }

    /**
     * Get all alerts from ZAP proxy
     *
     * @param json true for json form, false for xml format
     * @return all alerts from ZAProxy
     * @throws Exception
     */
    private String getAllAlertsFormat(final String format) throws Exception
    {

        if (format.equalsIgnoreCase(XML_FORMAT) || format.equalsIgnoreCase(HTML_FORMAT) || format.equalsIgnoreCase(JSON_FORMAT))
        {
            return format;
        } else
        {
            return XML_FORMAT;
        }

    }

    /**
     * execute the whole shabang
     *
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException
    {

        if (skip)
        {
            getLog().info("Skipping zap exection");
            return;
        }

        try
        {

            zapClientAPI = getZapClient();
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(zapProxyHost, zapProxyPort));

            if (spiderURL)
            {
                getLog().info("Spider the site [" + targetURL + "] with apikey [" + apiKey + "]");
                spiderURL(targetURL);
            } else
            {
                getLog().info("skip spidering the site [" + targetURL + "]");
            }

            if (scanURL)
            {
                getLog().info("Scan the site [" + targetURL + "] with apikey [" + apiKey + "]");
                scanURL(targetURL);
            } else
            {
                getLog().info("skip scanning the site [" + targetURL + "]");
            }

            // filename to share between the session file and the report file
            String fileName = "";
            if (saveSession)
            {

                fileName = createTempFilename("ZAP", "");

                zapClientAPI.core.saveSession(apiKey, fileName, "true");
            } else
            {
                getLog().info("skip saveSession");
            }

            if (reportAlerts)
            {

                // reuse fileName of the session file
                if ((fileName == null) || (fileName.length() == 0))
                    fileName = createTempFilename("ZAP", "");

                String fileName_no_extension = FilenameUtils.concat(reportsDirectory, fileName);

                try
                {
                    String alerts = getAllAlerts(getAllAlertsFormat(format));
                    final String fullFileName = fileName_no_extension + "." + getAllAlertsFormat(format);
                    FileUtils.writeStringToFile(new File(fullFileName), alerts);

                    getLog().info("File save in format in [" + getAllAlertsFormat(format) + "]");
                } catch (Exception e)
                {
                    getLog().error(e.toString());
                    e.printStackTrace();
                }
            }

        } catch (Exception e)
        {
            getLog().error(e.toString());
            throw new MojoExecutionException("Processing with ZAP failed", e);
        } finally
        {
            if (shutdownZAP && (zapClientAPI != null))
            {
                try
                {
                    getLog().info("Shutdown ZAProxy");
                    zapClientAPI.core.shutdown(apiKey);
                } catch (Exception e)
                {
                    getLog().error(e.toString());
                    e.printStackTrace();
                }
            } else
            {
                getLog().info("No shutdown of ZAP");
            }
        }
    }

    protected ClientApi getZapClient()
    {
        return new ClientApi(zapProxyHost, zapProxyPort);
    }

}
