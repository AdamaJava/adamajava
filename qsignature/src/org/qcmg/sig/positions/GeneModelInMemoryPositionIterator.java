package org.qcmg.sig.positions;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.picard.ReferenceUtils;

public class GeneModelInMemoryPositionIterator extends PositionIterator<ChrPosition> implements Closeable {
	
	private final List<ChrPosition> list = new ArrayList<>();
	private int listPosition;
	
	public GeneModelInMemoryPositionIterator(File file, String reference) throws IOException {
		
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			
			String line;
			while ((line = in.readLine()) != null) {
				final String[] params = TabTokenizer.tokenize(line);
				if (params.length < 5) {
					System.out.println("params: " + Arrays.deepToString(params) + ", line: " + line);
				}
				String chr = params[0];			//note that this does not have the preceding "chr" and so will need to add
				int start = Integer.parseInt(params[3]);
				int end = Integer.parseInt(params[4]);
				
				/*
				 * get reference bases for this gene position
				 */
				byte[] referenceBases = ReferenceUtils.getRegionFromReferenceFile(reference, chr, start, end);
				/*
				 * loop through start to end building ChrPositionName's and add to list
				 */
				
				for (int i = start, j = 0 ; i < end ; i++, j++) {
					if (null != chr && i > -1) {
						list.add(new ChrPositionName(chr, i, i, ".\t" + (char)(referenceBases[j])));
					}
				}
			}
		}
	}

	@Override
	public boolean hasNext() {
		return listPosition < list.size();
	}

	@Override
	public ChrPosition next() {
		if (listPosition < list.size()) {
			return list.get(listPosition++);
		}
		return null;
	}

	@Override
	public void close() {
	}
	
	@Override
	public void sort(List<String> contigOrder) {
		list.sort(ChrPositionComparator.getComparator(ChrPositionComparator.getChrNameComparator(contigOrder)));
	}

}
