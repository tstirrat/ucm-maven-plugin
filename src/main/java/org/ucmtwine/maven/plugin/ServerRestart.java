package org.ucmtwine.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Restart a server (only works with idc:// connections)
 * 
 * #goal restart
 */
public class ServerRestart extends AbstractUCMServerAwareMojo {

  public void execute() throws MojoExecutionException, MojoFailureException {
    IdcServerDefinition server = getSelectedServer();
  }
}
