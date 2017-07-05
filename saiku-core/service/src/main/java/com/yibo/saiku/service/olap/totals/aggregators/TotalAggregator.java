package com.yibo.saiku.service.olap.totals.aggregators;

import com.yibo.saiku.olap.util.SaikuProperties;
import mondrian.util.Format;
import org.olap4j.Cell;
import org.olap4j.metadata.Measure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class TotalAggregator {
  private static final Map<String, TotalAggregator> all;

  static {
    Map<String, TotalAggregator> tmp = new HashMap<String, TotalAggregator>();
    tmp.put( "sum", new SumAggregator( null ) );
    tmp.put( "max", new MaxAggregator( null ) );
    tmp.put( "min", new MinAggregator( null ) );
    tmp.put( "avg", new AvgAggregator( null ) );
    all = Collections.unmodifiableMap( tmp );
  }

  private String formattedValue;
  protected Format format;

  protected TotalAggregator( Format format ) {
    this.format = format;
  }

  public void addData( Cell cell ) {
    try {
      // FIXME - maybe we should try to do fetch the format here, but seems to cause some issues? infinite loop? make
      // sure we try this only once to override existing format?
      //		if (format == null) {
      //			String formatString = (String) cell.getPropertyValue(Property.StandardCellProperty.FORMAT_STRING);
      //			this.format = Format.get(formatString, SaikuProperties.locale);
      //
      //		}
      Object value = cell.getValue();
      if ( value instanceof Number ) {
        double doubleVal;

        doubleVal = cell.getDoubleValue();
        addData( doubleVal );
      }
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }

  }

  protected abstract void addData( double data );

  public abstract Double getValue();

  public abstract TotalAggregator newInstance( Format format, Measure measure );

  public String getFormattedValue() {
    if ( formattedValue != null ) {
      return formattedValue;
    } else {
      Double value = getValue();
      if ( value != null ) {
        return format.format( value );
      }
      return "";
    }
  }

  public void setFormattedValue( String formattedValue ) {
    this.formattedValue = formattedValue;
  }

  public TotalAggregator newInstance() {
    return newInstance( "" );
  }

  public TotalAggregator newInstance( String formatString ) {
    return newInstance( new Format( formatString, SaikuProperties.locale ), null );
  }

  public static TotalAggregator newInstanceByFunctionName( final String functionName ) {
    return all.get( functionName );
  }
}