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

import com.yibo.saiku.olap.dto.SaikuDimensionSelection;
import com.yibo.saiku.olap.dto.resultset.CellDataSet;
import com.yibo.saiku.olap.util.OlapResultSetUtil;
import com.yibo.saiku.olap.util.formatter.FlattenedCellSetFormatter;
import com.yibo.saiku.olap.util.formatter.HierarchicalCellSetFormatter;
import com.yibo.saiku.olap.util.formatter.ICellSetFormatter;
import com.yibo.saiku.service.util.export.excel.ExcelBuilderOptions;
import com.yibo.saiku.service.util.export.excel.ExcelWorksheetBuilder;
import org.olap4j.CellSet;

import java.util.List;

public class ExcelExporter {

  public static byte[] exportExcel( CellSet cellSet, List<SaikuDimensionSelection> filters ) {
    return exportExcel( cellSet, new HierarchicalCellSetFormatter(), filters );
  }

  public static byte[] exportExcel( CellSet cellSet,
                                    ICellSetFormatter formatter,
                                    List<SaikuDimensionSelection> filters ) {
    CellDataSet table = OlapResultSetUtil.cellSet2Matrix( cellSet, formatter );
    ExcelBuilderOptions exb = new ExcelBuilderOptions();
    exb.repeatValues = ( formatter instanceof FlattenedCellSetFormatter);
    return getExcel( table, filters, exb );
  }

  private static byte[] getExcel( CellDataSet table, List<SaikuDimensionSelection> filters,
                                  ExcelBuilderOptions options ) {
    // TBD Sheet name is parametric. Useful for future ideas or improvements
    ExcelWorksheetBuilder worksheetBuilder = new ExcelWorksheetBuilder( table, filters, options );
    return worksheetBuilder.build();
  }
}
