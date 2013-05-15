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

import java.io.IOException;

import oracle.stellent.ridc.IdcClient;
import oracle.stellent.ridc.IdcClientException;
import oracle.stellent.ridc.IdcClientManager;
import oracle.stellent.ridc.IdcContext;
import oracle.stellent.ridc.model.DataBinder;
import oracle.stellent.ridc.protocol.ServiceResponse;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Deploy a component to a server
 * 
 * @goal deploy
 * @execute goal="build"
 */
public class DeployComponent extends AbstractServerAwareMojo {

  public void execute() throws MojoExecutionException {

    IdcServerDefinition server = getSelectedServer();

    // if not supplied, find the component zip in the project folder
    determineComponentName();
    determineComponentZip();

    if (componentZip == null) {
      throw new MojoExecutionException("Unable to determine appropriate component zip file from root project folder.");
    }

    if (componentName == null) {
      throw new MojoExecutionException("Unable to determine component name. Does a zip file exist in project root?");
    }

    getLog().info("Deploying component " + componentName + " to " + server.getId() + " from zip: " + componentZip);

    IdcClientManager manager = new IdcClientManager();

    try {
      @SuppressWarnings("rawtypes")
      IdcClient idcClient = manager.createClient(server.getUrl());

      IdcContext userContext = new IdcContext(server.getUsername(), server.getPassword());

      DataBinder binder = idcClient.createBinder();

      // 1. GET_COMPONENT_INSTALL_FORM

      binder.putLocal("IdcService", "GET_COMPONENT_INSTALL_FORM");
      binder.putLocal("IDC_Id", server.getId());

      try {
        binder.addFile("ComponentZipFile", componentZip);
      } catch (IOException e) {
        throw new MojoExecutionException("Error reading zip file: " + componentZip, e);
      }

      ServiceResponse response = idcClient.sendRequest(userContext, binder);

      DataBinder responseBinder = response.getResponseAsBinder();

      // 2. UPLOAD_NEW_COMPONENT

      // pass through component location and name to next service
      binder.putLocal("IdcService", "UPLOAD_NEW_COMPONENT");
      binder.putLocal("ComponentName", responseBinder.getLocal("ComponentName"));
      binder.putLocal("location", responseBinder.getLocal("location"));
      // needed for 11g
      binder.putLocal("componentDir", responseBinder.getLocal("componentDir"));
      binder.removeFile("ComponentZipFile");

      response = idcClient.sendRequest(userContext, binder);

      responseBinder = response.getResponseAsBinder();

      // 3. ENABLE COMP.
      // TODO: enable component

    } catch (IdcClientException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }
}
