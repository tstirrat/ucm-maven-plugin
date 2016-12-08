package org.ucmtwine.maven.plugin;


import java.lang.String;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *  Resumes WCC as a Managed Server
 *  @author txburton
 *  @version Nov 18, 2016
 */
@Mojo(name = "resume" )
public class ResumeWLSServer extends AbstractWLSServerControlMojo
{

   /** Default Constructor - Does Nothing */
   public ResumeWLSServer() { }

   /* (non-Javadoc)
    * @see org.apache.maven.plugin.Mojo#execute()
    */
   public void execute() throws MojoExecutionException, MojoFailureException
   { super.resumeServer(); }

}
