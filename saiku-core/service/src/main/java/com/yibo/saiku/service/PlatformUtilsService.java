package com.yibo.saiku.service;

import com.yibo.saiku.service.util.Plugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

/**
 * Created by bugg on 30/04/14.
 */
public class PlatformUtilsService {

  private String filePath;

  @Autowired
  public void setPath(String path) {
    this.filePath = path;
  }

  public String getPath(){
    return filePath;
  }


  public ArrayList getAvailablePlugins(){
    ArrayList l = new ArrayList<Plugin>(  );
    File f = new File(filePath);

    String[] directories = f.list(new FilenameFilter() {
      public boolean accept(File current, String name) {
        return new File(current, name).isDirectory();
      }
    });

    if(directories != null && directories.length>0) {
      for ( String d : directories ) {
        File subdir = new File( filePath+"/"+d );
        File[] subfiles = subdir.listFiles();

        /**
         * TODO use a metadata.js file for alternative details.
         */
        if ( subfiles != null ) {
          for ( File s : subfiles ) {
            if ( s.getName().equals( "plugin.js" ) ) {
              Plugin p = new Plugin( s.getParentFile().getName(), "", "js/saiku/plugins/" + s.getParentFile().getName() + "/plugin.js" );
              l.add( p );
            }
          }
        }
      }
    }
    return l;
  }
}
