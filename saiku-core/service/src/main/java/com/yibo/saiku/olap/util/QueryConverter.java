package com.yibo.saiku.olap.util;

import com.yibo.saiku.olap.util.exception.SaikuIncompatibleException;
import org.olap4j.mdx.SelectNode;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.query.Query;
import org.olap4j.query.QueryAxis;
import org.olap4j.query.QueryDimension;
import org.olap4j.query.Selection;
import org.olap4j.query.Selection.Operator;
import com.yibo.saiku.query.QueryHierarchy;
import com.yibo.saiku.query.SortOrder;
import com.yibo.saiku.query.mdx.GenericFilter;
import com.yibo.saiku.query.mdx.IFilterFunction.MdxFunctionType;
import com.yibo.saiku.query.mdx.NFilter;

public class QueryConverter {

  public static SelectNode convert( Query query ) throws Exception {

    com.yibo.saiku.query.Query sQuery = new com.yibo.saiku.query.Query( query.getName(), query.getCube() );

    for ( QueryAxis axis : query.getAxes().values() ) {
      if ( axis.getLocation() != null ) {
        com.yibo.saiku.query.QueryAxis sAxis = sQuery.getAxis( axis.getLocation() );
        convertAxis( axis, sAxis, sQuery );
      }

    }


    return sQuery.getSelect();
  }

  private static void convertAxis( QueryAxis axis, com.yibo.saiku.query.QueryAxis sAxis, com.yibo.saiku.query.Query sQuery )
    throws Exception {

    for ( QueryDimension qD : axis.getDimensions() ) {
      convertDimension( qD, sAxis, sQuery );
    }

    if ( axis.getSortOrder() != null ) {
      SortOrder so = SortOrder.valueOf( axis.getSortOrder().toString() );
      sAxis.sort( so, axis.getSortIdentifierNodeName() );
    }

    if ( axis.getFilterCondition() != null ) {
      sAxis.addFilter( new GenericFilter( axis.getFilterCondition() ) );
    }

    if ( axis.getLimitFunction() != null ) {
      NFilter nf = new NFilter( MdxFunctionType.valueOf(
        axis.getLimitFunction().toString() ),
        axis.getLimitFunctionN().intValue(),
        axis.getLimitFunctionSortLiteral()
      );
      sAxis.addFilter( nf );
    }

    sAxis.setNonEmpty( axis.isNonEmpty() );


  }

  private static void convertDimension( QueryDimension qD, com.yibo.saiku.query.QueryAxis sAxis,
                                        com.yibo.saiku.query.Query sQuery ) throws Exception {
    boolean first = true;
    String hierarchyName = null;
    QueryHierarchy qh = null;
    for ( Selection sel : qD.getInclusions() ) {
      if ( first ) {
        if ( ( sel.getRootElement() instanceof Member ) ) {
          hierarchyName = ( (Member) sel.getRootElement() ).getHierarchy().getName();
        } else {
          hierarchyName = ( (Level) sel.getRootElement() ).getHierarchy().getName();
        }

        qh = sQuery.getHierarchy( hierarchyName );
        first = false;
      }

      if ( sel.getSelectionContext() != null ) {
        throw new SaikuIncompatibleException( "Cannot convert queries with selection context" );
      }
      if ( ( sel.getRootElement() instanceof Member ) ) {
        if ( sel.getOperator().equals( Operator.MEMBER ) ) {
          qh.includeMember( sel.getRootElement().getUniqueName() );
        } else {
          throw new SaikuIncompatibleException(
            "Cannot convert member selection using operator: " + sel.getOperator() );
        }
      } else {
        qh.includeLevel( sel.getRootElement().getName() );
      }
    }
    sAxis.addHierarchy( qh );

  }

}
