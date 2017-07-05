package com.yibo.saiku.service.olap.totals;

import com.yibo.saiku.olap.util.SaikuProperties;
import mondrian.util.Format;
import org.apache.commons.lang.StringUtils;
import org.olap4j.Axis;
import org.olap4j.Cell;
import org.olap4j.CellSet;
import org.olap4j.OlapException;
import org.olap4j.Position;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Measure;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.Property;
import com.yibo.saiku.service.olap.totals.aggregators.TotalAggregator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotalsListsBuilder implements FormatList {
  private final Member[] memberBranch;
  private final TotalNode totalBranch[];
  private final TotalAggregator[] aggrTempl;
  private final int col, row;
  private final List<TotalNode> totalsLists[];
  private final int measuresAt;
  private final String[] measuresCaptions;
  private final Measure[] measures;
  private final Map<String, Integer> uniqueToSelected;
  private final AxisInfo dataAxisInfo;
  private final AxisInfo totalsAxisInfo;
  private final CellSet cellSet;
  private final Format[] valueFormats;


  public TotalsListsBuilder( Measure[] selectedMeasures, TotalAggregator[] aggrTempl, CellSet cellSet,
                             AxisInfo totalsAxisInfo, AxisInfo dataAxisInfo ) throws Exception {
    Cube cube;
    try {
      cube = cellSet.getMetaData().getCube();
    } catch ( OlapException e ) {
      throw new RuntimeException( e );
    }
    uniqueToSelected = new HashMap<String, Integer>();
    if ( selectedMeasures.length > 0 ) {
      valueFormats = new Format[ selectedMeasures.length ];
      measures = selectedMeasures;
      for ( int i = 0; i < valueFormats.length; i++ ) {
        valueFormats[ i ] = getMeasureFormat( selectedMeasures[ i ] );
        uniqueToSelected.put( selectedMeasures[ i ].getUniqueName(), i );
      }
    } else {
      Measure defaultMeasure = cube.getMeasures().get( 0 );
      if ( cube.getDimensions().get( "Measures" ) != null ) {
        Member ms = cube.getDimensions().get( "Measures" ).getDefaultHierarchy().getDefaultMember();
        if ( ms instanceof Measure ) {
          defaultMeasure = (Measure) ms;
        }
      }
      measures = new Measure[] { defaultMeasure };
      valueFormats = new Format[] { getMeasureFormat( defaultMeasure ) };
    }
    this.cellSet = cellSet;
    this.dataAxisInfo = dataAxisInfo;
    this.totalsAxisInfo = totalsAxisInfo;
    final int maxDepth = dataAxisInfo.maxDepth + 1;
    boolean hasMeasuresOnDataAxis = false;
    int measuresAt = 0;
    int measuresMember = 0;
    final List<Member> members =
      dataAxisInfo.axis.getPositionCount() > 0 ? dataAxisInfo.axis.getPositions().get( 0 ).getMembers() :
        Collections.<Member>emptyList();
    for (; measuresMember < members.size(); measuresMember++ ) {
      Member m = members.get( measuresMember );
      if ( "Measures".equals( m.getDimension().getName() ) ) {
        hasMeasuresOnDataAxis = true;
        break;
      }
      measuresAt += dataAxisInfo.levels[ measuresMember ].size();
    }
    if ( hasMeasuresOnDataAxis ) {
      this.measuresAt = measuresAt;
      measuresCaptions = new String[ selectedMeasures.length ];
      for ( int i = 0; i < measuresCaptions.length; i++ ) {
        measuresCaptions[ i ] = selectedMeasures[ i ].getCaption();
      }
    } else {
      this.measuresAt = Integer.MIN_VALUE;
      measuresCaptions = null;
    }

    totalBranch = new TotalNode[ maxDepth ];
    TotalNode rootNode =
      new TotalNode( measuresCaptions, measures, aggrTempl[ 0 ], this, totalsAxisInfo.fullPositions.size() );
    col = Axis.ROWS.equals( dataAxisInfo.axis.getAxisOrdinal() ) ? 1 : 0;
    row = ( col + 1 ) & 1;
    this.aggrTempl = aggrTempl;

    totalBranch[ 0 ] = rootNode;
    totalsLists = new List[ maxDepth ];
    for ( int i = 0; i < totalsLists.length; i++ ) {
      totalsLists[ i ] = new ArrayList<TotalNode>();
    }
    totalsLists[ 0 ].add( rootNode );
    memberBranch = new Member[ dataAxisInfo.maxDepth + 1 ];
  }

  private Format getMeasureFormat( Measure m ) {
    try {
      String formatString = (String) m.getPropertyValue( Property.StandardCellProperty.FORMAT_STRING );
      if ( StringUtils.isBlank( formatString ) ) {
        Map<String, Property> props = m.getProperties().asMap();
        if ( props.containsKey( "FORMAT_STRING" ) ) {
          formatString = (String) m.getPropertyValue( props.get( "FORMAT_STRING" ) );
        } else if (props.containsKey("FORMAT_EXP_PARSED")) {
            formatString = (String) m.getPropertyValue(props.get("FORMAT_EXP_PARSED"));
        }else if ( props.containsKey( "FORMAT_EXP" ) ) {
          formatString = (String) m.getPropertyValue( props.get( "FORMAT_EXP" ) );
        } else if ( props.containsKey( "FORMAT" ) ) {
          formatString = (String) m.getPropertyValue( props.get( "FORMAT" ) );
        }
	    if (StringUtils.isBlank(formatString)) {
          formatString = "Standard";
        }
    	if (StringUtils.isNotBlank(formatString) && formatString.length() > 1 && formatString.startsWith("\"") && formatString.endsWith("\"")) {
            formatString = formatString.substring(1, formatString.length() - 1);
        }
      }
      return Format.get( formatString, SaikuProperties.locale );
    } catch ( OlapException e ) {
      throw new RuntimeException( e );
    }
  }

  private final void positionMember( final int depth, Member m, final List<Integer> levels, final Member[] branch ) {
    for ( int i = levels.size() - 1; i >= 0; ) {
      branch[ depth + i ] = m;
      i--;
      do {
        m = m.getParentMember();
      }
      while ( i >= 0 && m != null && m.getDepth() != levels.get( i ) );
    }
  }

  private void traverse( List<Integer>[] levels, List<TotalNode>[] totalLists ) {
    int fullPosition = 0;
    final Member[] prevMemberBranch = new Member[ memberBranch.length ];

    nextpos:
    for ( final Position p : dataAxisInfo.axis.getPositions() ) {
      int depth = 1;
      int mI = 0;
      for ( final Member m : p.getMembers() ) {
        final int maxDepth = levels[ mI ].get( levels[ mI ].size() - 1 );
        if ( m.getDepth() < maxDepth ) {
          continue nextpos;
        }
        positionMember( depth, m, levels[ mI ], memberBranch );
        depth += levels[ mI ].size();
        mI++;
      }

      int changedFrom = 1;
      while ( changedFrom < memberBranch.length - 1 && memberBranch[ changedFrom ]
        .equals( prevMemberBranch[ changedFrom ] ) ) {
        changedFrom++;
      }

      for ( int i = totalBranch.length - 1; i >= changedFrom; i-- ) {
        if ( totalBranch[ i ] != null ) {
          totalBranch[ i - 1 ].appendChild( totalBranch[ i ] );
        }
      }

      for ( int i = changedFrom; i < totalBranch.length; i++ ) {
        String[] captions = measuresAt > i - 1 ? measuresCaptions : null;
        totalBranch[ i ] =
          new TotalNode( captions, measures, aggrTempl[ i ], this, totalsAxisInfo.fullPositions.size() );
        totalLists[ i ].add( totalBranch[ i ] );
      }
      System.arraycopy( memberBranch, 0, prevMemberBranch, 0, prevMemberBranch.length );

      totalBranch[ totalBranch.length - 1 ].setSpan( 1 );
      totalBranch[ totalBranch.length - 1 ].setWidth( 1 );


      for ( int t = 0; t < totalsAxisInfo.fullPositions.size(); t++ ) {
        Cell cell = getCellAt( fullPosition, t );
        for ( int branchNode = 0; branchNode < totalBranch.length; branchNode++ ) {
          if ( aggrTempl[ branchNode ] != null ) {
            totalBranch[ branchNode ].addData( getMemberIndex( branchNode, fullPosition ), t, cell );
          }
        }
      }
      fullPosition++;
    }
    for ( int i = totalBranch.length - 1; i > 0; i-- ) {
      totalBranch[ i - 1 ].appendChild( totalBranch[ i ] );
    }

    for ( TotalNode n : totalLists[ totalLists.length - 1 ] ) {
      n.setWidth( 0 );
    }
  }

  public List<TotalNode>[] buildTotalsLists() {
    traverse( dataAxisInfo.levels, totalsLists );
    return totalsLists;
  }

  private Cell getCellAt( int axisCoord, int perpAxisCoord ) {
    final Position[] positions =
      new Position[] { dataAxisInfo.fullPositions.get( axisCoord ), totalsAxisInfo.fullPositions.get( perpAxisCoord ) };
    Cell cell = cellSet.getCell( positions[ col ], positions[ row ] );
    return cell;
  }

  private int getMemberIndex( int depth, int index ) {
    if ( depth - 1 < measuresAt ) {
      Member m = dataAxisInfo.fullPositions.get( index ).getMembers().get( dataAxisInfo.measuresMember );
      if ( uniqueToSelected.containsKey( m.getUniqueName() ) ) {
        return uniqueToSelected.get( m.getUniqueName() );
      }
    }
    return 0;
  }

  public Format getValueFormat( int position, int member ) {
    int formatIndex = 0;
    if ( dataAxisInfo.measuresMember >= 0 ) {
      formatIndex = member;
    } else if ( totalsAxisInfo.measuresMember >= 0 ) {
      Member m = totalsAxisInfo.fullPositions.get( position ).getMembers().get( totalsAxisInfo.measuresMember );
      if ( uniqueToSelected.containsKey( m.getUniqueName() ) ) {
        formatIndex = uniqueToSelected.get( m.getUniqueName() );
      }
    }
    return valueFormats[ formatIndex ];
  }

}