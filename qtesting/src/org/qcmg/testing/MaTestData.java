/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class MaTestData {
    public static final String FILE_NAME_A = "F3_1.ma";
    public static final String FILE_NAME_B = "R3_1.ma";

    public static final void createAllTestFiles()
        throws Exception
    {
        createFirstF3Ma();
        createFirstR3Ma();
    }

    public static final void deleteAllTestFiles()
        throws Exception
    {
	try {
	    File fileA = new File(FILE_NAME_A);
	    fileA.delete();
	} catch (Exception e) {
	    //Swallow quietly
	}

	try {
	    File fileB = new File(FILE_NAME_B);
	    fileB.delete();
	} catch (Exception e) {
	    //Swallow quietly
	}
    }

    public static final void createFirstF3Ma()
       throws Exception {
        File file = new File(FILE_NAME_A);

        OutputStream os = new FileOutputStream(file);
        PrintStream ps = new PrintStream(os);


        ps.close();
        os.close();
    }

    public static final void createFirstR3Ma()
        throws Exception {
        File file = new File(FILE_NAME_B);

        OutputStream os = new FileOutputStream(file);
        PrintStream ps = new PrintStream(os);

        ps.close();
        os.close();
    }
}
