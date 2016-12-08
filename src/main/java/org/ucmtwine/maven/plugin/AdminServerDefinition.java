package org.ucmtwine.maven.plugin;

import org.apache.maven.plugins.annotations.Parameter;

/**
 *  Object that defines the configured Admin Server
 */
public class AdminServerDefinition 
{
   /** Port the Adminserver runs on */
   @Parameter(defaultValue = "7001")
   private String port;

   /** hostname for the adminserver */
   @Parameter(defaultValue = "localhost")
   private String hostname;

   /** webLogic Managed Server name */
   @Parameter(defaultValue = "adminserver")
   private String serverName;
   
  /** WLS username - defaults to WCC value */
  @Parameter(defaultValue = "${servers[0].username}") private String username;

  /** WLS password - defaults to WCC value */
  @Parameter(defaultValue = "${servers[0].password}") private String password;
  
  /** Name of the UCM instance as deployed into WLS */
  @Parameter(defaultValue = "ucm") private String wlsServerName;
  
  private IdcServerDefinition parent;

  public String getPort() 
  {
     if ( null == port ) { port = "7001"; }
     return port; 
  }

  public void setPort(String port) { this.port = port; }

  public String getHostname() 
  {
     if (null == hostname) { hostname = "localhost"; }
     return hostname; 
  }
  
  public void setHostname(String hostname) { this.hostname = hostname; }

  public String getServerName() 
  {
     if ( null == serverName ) { serverName = "adminserver"; }
     return serverName; 
  }

  public void setServerName(String serverName) { this.serverName = serverName; }

  public String getUsername() 
  {
     if (null == username && null != parent) { username = parent.getUsername(); }
     return username; 
  }

  public void setUsername(String username) { this.username = username; }

  public String getPassword() 
  { 
     if (null == password && null != parent ) { password = parent.getPassword(); }
     return password; 
  }

  public void setPassword(String password) { this.password = password; }

  public String getWlsServerName() 
  { 
     if ( null == wlsServerName ) { wlsServerName = "ucm"; }
     return wlsServerName; 
  }

  public void setWlsServerName(String wlsName) { this.wlsServerName = wlsName; }
  
  public void setParentIdcServerDefinition(IdcServerDefinition parent)
  { this.parent = parent; }
}
