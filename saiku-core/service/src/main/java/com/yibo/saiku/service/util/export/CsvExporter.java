/*  
 *   Copyright 2012 OSBI Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.yibo.saiku.service.util.export;

import com.yibo.saiku.olap.dto.resultset.AbstractBaseCell;
import com.yibo.saiku.olap.dto.resultset.MemberCell;
import com.yibo.saiku.olap.util.SaikuProperties;
import org.apache.commons.lang.StringUtils;
import org.olap4j.CellSet;
import com.yibo.saiku.olap.dto.resultset.CellDataSet;
import com.yibo.saiku.olap.dto.resultset.DataCell;
import com.yibo.saiku.olap.util.OlapResultSetUtil;
import com.yibo.saiku.olap.util.formatter.CellSetFormatter;
import com.yibo.saiku.olap.util.formatter.ICellSetFormatter;
import com.yibo.saiku.service.util.KeyValue;
import com.yibo.saiku.service.util.exception.SaikuServiceException;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CsvExporter {

  public static byte[] exportCsv( CellSet cellSet ) {
    return exportCsv( cellSet, SaikuProperties.webExportCsvDelimiter, SaikuProperties.webExportCsvTextEscape );
  }

  public static byte[] exportCsv( CellSet cellSet, String delimiter, String enclosing ) {
    return exportCsv( cellSet, delimiter, enclosing, new CellSetFormatter() );
  }

  public static byte[] exportCsv( CellSet cellSet, String delimiter, String enclosing, ICellSetFormatter formatter ) {
    CellDataSet table = OlapResultSetUtil.cellSet2Matrix( cellSet, formatter );
    return getCsv( table, delimiter, enclosing );
  }

  public static byte[] exportCsv( ResultSet rs ) {
    return getCsv( rs, SaikuProperties.webExportCsvDelimiter, SaikuProperties.webExportCsvTextEscape, true, null );

  }

  public static byte[] exportCsv( ResultSet rs, String delimiter, String enclosing ) {
    return getCsv( rs, delimiter, enclosing, true, null );
  }

  public static byte[] exportCsv( ResultSet rs, String delimiter, String enclosing, boolean printHeader,
                                  List<KeyValue<String, String>> additionalColumns ) {
    return getCsv( rs, delimiter, enclosing, printHeader, additionalColumns );
  }

  private static byte[] getCsv( ResultSet rs, String delimiter, String enclosing, boolean printHeader,
                                List<KeyValue<String, String>> additionalColumns ) {
    Integer width = 0;

    Integer height = 0;
    StringBuilder sb = new StringBuilder();
    String addCols = null;
    ResultSetHelper rsch = new ResultSetHelper();
    try {
      while ( rs.next() ) {
        if ( height == 0 ) {
          width = rs.getMetaData().getColumnCount();
          String header = null;
          if ( additionalColumns != null ) {
            for ( KeyValue<String, String> kv : additionalColumns ) {
              if ( header == null ) {
                header = "";
                addCols = "";
              } else {
                header += delimiter;
              }
              header += enclosing + kv.getKey() + enclosing;
              addCols += enclosing + kv.getValue() + enclosing + delimiter;
            }
          }
          for ( int s = 0; s < width; s++ ) {
            if ( header != null ) {
              header += delimiter;
            } else {
              header = "";
            }
            header += enclosing + rs.getMetaData().getColumnName( s + 1 ) + enclosing;
          }
          if ( header != null && printHeader ) {
            header += "\r\n";
            sb.append( header );
          }
        }
        if ( addCols != null ) {
          sb.append( addCols );
        }
        for ( int i = 0; i < width; i++ ) {
          int colType = rs.getMetaData().getColumnType( i + 1 );
          String content = rsch.getValue( rs, colType, i + 1 );
          if ( content == null ) {
            content = "";
          }
          if ( i > 0 ) {
            sb.append( delimiter );
          }
          content = content.replace( "\"", "\"\"" );
          sb.append( enclosing + content + enclosing );
        }
        sb.append( "\r\n" );
        height++;
      }
      return sb.toString().getBytes( SaikuProperties.webExportCsvTextEncoding ); //$NON-NLS-1$
    } catch ( Exception e ) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new byte[ 0 ];
  }

  private static byte[] getCsv( CellDataSet table, String delimiter, String enclosing ) {
    if ( table != null ) {
      AbstractBaseCell[][] rowData = table.getCellSetBody();
      AbstractBaseCell[][] rowHeader = table.getCellSetHeaders();


      boolean offset = rowHeader.length > 0;
      String[][] result = new String[ ( offset ? 1 : 0 ) + rowData.length ][];
      if ( offset ) {
        List<String> cols = new ArrayList<String>();
        for ( int x = 0; x < rowHeader[ 0 ].length; x++ ) {
          String col = null;
          for ( int y = rowHeader.length - 1; y >= 0; y-- ) {
            String value = rowHeader[ y ][ x ].getFormattedValue();
            if ( value == null || "null".equals( value ) )  //$NON-NLS-1$
            {
              value = ""; //$NON-NLS-1$
            }
            if ( col == null && StringUtils.isNotBlank( value ) ) {
              col = value;
            } else if ( col != null && StringUtils.isNotBlank( value ) ) {
              col = value + "/" + col;
            }
          }
          cols.add( enclosing + col + enclosing );
        }
        result[ 0 ] = cols.toArray( new String[ cols.size() ] );
      }
      String[] lastKnownHeader = null;
      for ( int x = 0; x < rowData.length; x++ ) {
        int xTarget = ( offset ? 1 : 0 ) + x;
        if ( lastKnownHeader == null ) {
          lastKnownHeader = new String[ rowData[ x ].length ];
        }
        List<String> cols = new ArrayList<String>();
        for ( int y = 0; y < rowData[ x ].length; y++ ) {
          String value = rowData[ x ][ y ].getFormattedValue();
          if ( !SaikuProperties.webExportCsvUseFormattedValue ) {
            if ( rowData[ x ][ y ] instanceof DataCell && ( (DataCell) rowData[ x ][ y ] ).getRawNumber() != null ) {
              value = ( (DataCell) rowData[ x ][ y ] ).getRawNumber().toString();
            }
          }
          if ( rowData[ x ][ y ] instanceof MemberCell && StringUtils.isNotBlank( value ) && !"null".equals( value ) ) {
            lastKnownHeader[ y ] = value;
          } else if ( rowData[ x ][ y ] instanceof MemberCell && ( StringUtils.isBlank( value ) || "null"
            .equals( value ) ) ) {
            value = ( StringUtils.isNotBlank( lastKnownHeader[ y ] ) ? lastKnownHeader[ y ] : null );
          }

          if ( value == null || "null".equals( value ) ) {
            value = "";
          }
          value = value.replace( "\"", "\"\"" );
          value = enclosing + value + enclosing;
          cols.add( value );
        }
        result[ xTarget ] = cols.toArray( new String[ cols.size() ] );

      }
      return export( result, delimiter );
    }
    return new byte[ 0 ];
  }

  private static byte[] export( String[][] resultSet, String delimiter ) {
    try {
      String output = "";
      StringBuffer buf = new StringBuffer();
      if ( resultSet.length > 0 ) {
        for ( int i = 0; i < resultSet.length; i++ ) {
          String[] vs = resultSet[ i ];

          for ( int j = 0; j < vs.length; j++ ) {
            String value = vs[ j ];

            if ( j > 0 ) {
              buf.append( delimiter + value );
              //output += delimiter + value;
            } else {
              buf.append( value );
              //output += value;
            }
          }
          buf.append( "\r\n" );
          //output += "\r\n"; //$NON-NLS-1$
        }
        output = buf.toString();
        return output.getBytes( SaikuProperties.webExportCsvTextEncoding ); //$NON-NLS-1$
      }
    } catch ( Throwable e ) {
      throw new SaikuServiceException( "Error creating csv export for query" ); //$NON-NLS-1$
    }
    return new byte[ 0 ];
  }
}
