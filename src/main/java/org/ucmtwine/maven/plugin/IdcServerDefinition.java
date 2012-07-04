package org.ucmtwine.maven.plugin;

public class IdcServerDefinition {

  /**
   * UCM server id
   * 
   * @parameter
   * @required
   */
  private String id;

  /**
   * UCM url and (idc) port e.g. idc://localhost:4444
   * 
   * @parameter default="idc://localhost:4444"
   */
  private String url;

  /**
   * UCM username
   * 
   * @parameter default="sysadmin"
   */
  private String username;

  /**
   * UCM user's password
   * 
   * @parameter default="idc"
   */
  private String password;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

}
