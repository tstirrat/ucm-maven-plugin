package org.ucmtwine.maven.plugin;

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

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.io.IOException;

import oracle.stellent.ridc.IdcClient;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.protocol.ServiceResponse;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/** Deploy a component to a server */
@Mojo(name = "deploy" )
//@Execute(goal="build", phase = LifecyclePhase.PACKAGE)
public class DeployComponent extends AbstractServerAwareMojo
{

  public void execute() throws MojoExecutionException
  {
    IdcServerDefinition server = getSelectedServer();
    
    File componentZipFile = getComponentZipAsFile();

    if (componentZipFile == null)
    {
      throw new MojoExecutionException( "Unable to determine appropriate "
                                      + "component zip file from root project folder.");
    }

    if (componentName == null)
    {
      throw new MojoExecutionException( "Unable to determine component name. "
                                      + "Does a zip file exist?");
    }

    getLog().info("Deploying component " + componentName + " to " + server.getId()
                 +" from zip: " + componentZipFile);

    IdcClientManager manager = new IdcClientManager();

    try
    {
      @SuppressWarnings("rawtypes")
      IdcClient idcClient = manager.createClient(server.getUrl());

      IdcContext userContext = new IdcContext(server.getUsername(), 
                                              server.getPassword());

      DataBinder binder = idcClient.createBinder();

      // 1. GET_COMPONENT_INSTALL_FORM

      binder.putLocal("IdcService", "GET_COMPONENT_INSTALL_FORM");
      binder.putLocal("IDC_Id", server.getId());

      try { binder.addFile("ComponentZipFile", componentZipFile); }
      catch (IOException ioe)
      {
        throw new MojoExecutionException( "Error reading zip file: " 
                                        + componentZipFile, ioe);
      }

      ServiceResponse response = idcClient.sendRequest(userContext, binder);

      DataBinder responseBinder = response.getResponseAsBinder();

      // 2. UPLOAD_NEW_COMPONENT

      // pass through component location and name to next service
      binder.putLocal("IdcService",    "UPLOAD_NEW_COMPONENT");
      binder.putLocal("ComponentName", responseBinder.getLocal("ComponentName"));
      binder.putLocal("location",      responseBinder.getLocal("location"));
      // needed for 11g
      binder.putLocal("componentDir",  responseBinder.getLocal("componentDir"));
      binder.removeFile("ComponentZipFile");

      response = idcClient.sendRequest(userContext, binder);

      responseBinder = response.getResponseAsBinder();

      // 3. ENABLE COMPONENT  ADMIN_TOGGLE_COMPONENTS
      
      binder.putLocal("IdcService", "ADMIN_TOGGLE_COMPONENTS");
      binder.putLocal("isEnable",   "1");
      
      binder.putLocal("IDC_Id", server.getAdminServer().getWlsServerName());
      binder.putLocal("ComponentNames", componentName);
      
      response = idcClient.sendRequest(userContext, binder);

      responseBinder = response.getResponseAsBinder();

      // 4. Restart WCC - moved to Separate goals
    }
    catch (IdcClientException ice)
    { throw new MojoExecutionException(ice.getMessage()); }
  }
}
