package org.ucmtwine.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.String;

import org.apache.maven.plugin.MojoExecutionException;

/**
 *  Helper class to contain the file hda file update method
 *  @author txburton
 *  @version Dec 1, 2016
 */
public class FileUpdateHelper 
{
   /**
    *  replaces the line with the specified prefix with the new value
    * 
    *  @param prefix line to replace (ends with '=', will be added if missing.)
    *  @param newValue  The new value to set after prefix
    *  @param hdaFile  file to be processed
    *  @throws IOException 
    *  @throws MojoExecutionException
    *
    *  TODO: heavily based on UpdateClasspath code, refactor/reuse for both.
    */
   public static void replaceLine(String prefix, String newValue, File hdaFile)
          throws IOException, MojoExecutionException
   {
        File tempFile = new File("temp.hda");
        
        BufferedReader reader = new BufferedReader(new FileReader(hdaFile));
        PrintWriter writer = new PrintWriter(new FileWriter(tempFile, false));
        String line = null;
        
        //force post prefix =
        if ( !prefix.endsWith("=") ) { prefix = prefix+"="; }
        
        while ((line = reader.readLine()) != null)
        {
          if (line.startsWith(prefix)) { writer.println(prefix + newValue); }
          else { writer.println(line); }
        }
        
        reader.close();
        writer.flush();
        writer.close();
        
        File oldFile = new File(hdaFile.getName()+".old.hda");
        
        if (oldFile.exists()) { oldFile.delete(); }
        
        if (!hdaFile.renameTo(oldFile))
        {
           throw new MojoExecutionException( "Unable to rename " + hdaFile.getName()
                                           + " to " + oldFile.getName());
        }
        
        if (!tempFile.renameTo(hdaFile))
        {
           throw new MojoExecutionException( "Unable to rename " + tempFile.getName()
                                           + " to " + hdaFile.getName());
        }
        
        //TODO: should I add file clean-up here?
     }

}