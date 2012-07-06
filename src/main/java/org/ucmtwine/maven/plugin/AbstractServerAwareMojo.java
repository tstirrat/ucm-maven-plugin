package org.ucmtwine.maven.plugin;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Extend this if you need your goal to be aware of the servers config and
 * interpret the command line argument to select one of the defined servers.
 */
abstract class AbstractServerAwareMojo extends AbstractComponentMojo {

  /**
   * Content server definitions
   * 
   * @parameter
   * @required
   */
  protected List<IdcServerDefinition> servers;

  /**
   * Chosen server when executing a deploy
   * 
   * @parameter expression="${server}" default=""
   */
  protected String requestedServer;

  /**
   * Determines the server that is selected out of the list of configured
   * servers. If no -Dserver=id is supplied, the first server is selected.
   * 
   * @return The selected server or the first server if none is specified.
   * @throws MojoExecutionException
   *           if an invalid server id is given.
   */
  protected IdcServerDefinition getSelectedServer() throws MojoExecutionException {
    if (servers.size() == 0) {
      throw new MojoExecutionException("You have not defined any servers in your configuration");
    }

    IdcServerDefinition server = null;

    // use specified server
    if (requestedServer != null && requestedServer.length() > 0) {
      for (IdcServerDefinition s : servers) {
        if (s.getId().equalsIgnoreCase(requestedServer)) {
          server = s;
          getLog().info("Selected server " + s.getId() + ": " + server.getUsername() + " @ " + server.getUrl());
        }
      }

    } else {
      server = servers.get(0);
      getLog()
          .info(
              "No server specified, using first (" + server.getId() + "):" + server.getUsername() + " @ "
                  + server.getUrl());
    }

    if (server == null) {
      throw new MojoExecutionException("Unable to find the server \"" + String.valueOf(requestedServer)
          + "\" specified by \"server\" property");
    }
    return server;
  }
}
