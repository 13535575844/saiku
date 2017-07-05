package com.yibo.saiku.olap.util;

import com.yibo.saiku.olap.dto.ISaikuObject;

import java.util.Comparator;

public class SaikuUniqueNameComparator implements Comparator<ISaikuObject> {

  public int compare( ISaikuObject o1, ISaikuObject o2 ) {
    return o1.getUniqueName().compareTo( o2.getUniqueName() );
  }

}
