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

import java.io.File;
import java.io.FilenameFilter;
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
public class DeployComponent extends AbstractUCMServerAwareMojo {

  /**
   * Name of the component
   * 
   * @parameter
   */
  private String componentName;

  /**
   * The component zip path, relative to the root of the project.
   * 
   * @parameter expression="${project.basedir}/${componentZip}" default=""
   */
  private File componentZip;

  /**
   * The project base dir.
   * 
   * @parameter expression="${project.basedir}"
   * @required
   */
  private File baseDir;

  public void execute() throws MojoExecutionException {

    IdcServerDefinition server = getSelectedServer();

    // if not supplied, find the component zip in the project folder
    determineComponentZip();

    determineComponentName();

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

      getLog().debug(responseBinder.toString());

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

      getLog().debug(responseBinder.toString());

      // 3. ENABLE COMP.
      // TODO: enable component

    } catch (IdcClientException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }

  /**
   * Find an appropriate zip file to use as the componentZip
   */
  private void determineComponentName() {
    if (componentName == null || componentName.length() == 0) {
      if (componentZip != null) {
        componentName = componentZip.getName().replaceAll(".zip", "");
      }
    }

  }

  private void determineComponentZip() {

    if (componentZip == null || !componentZip.exists()
        || (componentZip.exists() && !componentZip.getName().endsWith(".zip"))) {

      // clear the zip
      componentZip = null;

      // find all .zip files in the root folder
      File zipFiles[] = baseDir.listFiles(new FilenameFilter() {
        public boolean accept(File folder, String name) {
          return name.endsWith(".zip");
        }
      });

      for (File f : zipFiles) {
        // <componentName>.zip takes priority
        if (componentName != null && f.getName().replaceAll(".zip", "").equalsIgnoreCase(componentName)) {
          componentZip = f;
          break;
        }

        // otherwise choose any zip, taking blah.zip over manifest.zip if it
        // exists.
        if (f.getName().equalsIgnoreCase("manifest.zip") && componentZip == null) {
          // use manifest.zip if no other zip has been found
          componentZip = f;

        } else {
          // use any other zip
          componentZip = f;
        }
      }
    }
  }
}
