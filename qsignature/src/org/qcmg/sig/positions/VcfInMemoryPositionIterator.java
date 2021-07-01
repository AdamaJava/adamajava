package org.qcmg.sig.positions;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.qio.record.StringFileReader;

public class VcfInMemoryPositionIterator extends PositionIterator<ChrPosition> implements Closeable {
	
	public static final int REF_POSITION = 3;
	
	List<ChrPosition> list = new ArrayList<>();
	
	private String id;
	private String ref;
	private String chr;
	private int position;
	
	private int listPosition;
	
	public VcfInMemoryPositionIterator(File file) throws IOException {
		this(file, REF_POSITION);
	}
	
	public VcfInMemoryPositionIterator(File file, int refPosition) throws IOException {
		
		try (StringFileReader reader = new StringFileReader(file);) {
			for (String vcf : reader) {
				int tabPosition = vcf.indexOf("\t");
				int lastTabPosition = -1;
				int loops = 0;
				
				/*
				 * reset some values
				 */
				id = null;
				chr = null;
				ref = null;
				position = -1;
				
				/*
				 * re-populate values
				 */
				while (tabPosition > -1) {
					if (loops == 0) {
						chr = vcf.substring(lastTabPosition + 1, tabPosition);
					} else if (loops == 1) {
						position = Integer.parseInt(vcf.substring(lastTabPosition + 1, tabPosition));
					} else if (loops == 2) {
						id = vcf.substring(lastTabPosition + 1, tabPosition);
					} else if (loops == refPosition) {
						ref = vcf.substring(lastTabPosition + 1, tabPosition);
					}
					
					if (loops >= refPosition) {
						break;
					}
					
					loops++;
					lastTabPosition = tabPosition;
					tabPosition = vcf.indexOf("\t", lastTabPosition + 1);
				}
				
				if (null != chr && position > -1 && null != ref) {
					list.add(new ChrPositionName(chr, position, position, id + "\t" + ref));
				}
			}
			
			/*
			 * sort!!!
			 */
			
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
	public void close() throws IOException {
	}

	@Override
	public void sort(List<String> contigOrder) {
		list.sort(ChrPositionComparator.getComparator(ChrPositionComparator.getChrNameComparator(contigOrder)));
	}

}
