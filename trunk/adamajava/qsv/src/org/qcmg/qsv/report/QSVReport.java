/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.report;

import java.io.File;
import java.io.IOException;



public abstract class QSVReport {
    
    protected File file;
    
    protected boolean append = false;

    
    public QSVReport(File file) {
        this.file = file;
    }    
        
    public abstract void writeHeader() throws IOException;

    public abstract void writeReport() throws Exception;

	public abstract String getHeader();

}
