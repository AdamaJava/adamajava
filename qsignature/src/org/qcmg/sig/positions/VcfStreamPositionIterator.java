package org.qcmg.sig.positions;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionName;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qio.record.StringFileReader;
import org.qcmg.qio.vcf.VcfFileReader;

public class VcfStreamPositionIterator extends PositionIterator<ChrPosition> implements Closeable {
	
//	public static final int ID_POSITION = 2;
	public static final int REF_POSITION = 3;
//	public static final int ALT_POSITION = 4;
	
	
	List<ChrPosition> list = new ArrayList<>();
	
	private StringFileReader reader;
	private Iterator<String> iterator;
	private final int refPosition;
//	private int altPosition;
	private String id;
	private String ref;
//	private String alt;
	private String chr;
	private int position;
	
	public VcfStreamPositionIterator(File file) throws IOException {
		this(file, REF_POSITION);
//		this(file, ID_POSITION, REF_POSITION, ALT_POSITION);
	}
	
	public VcfStreamPositionIterator(File file, int refPosition) throws IOException {
//		public VcfStreamPositionIterator(File file, int idPosition, int refPosition, int altPosition) throws IOException {
		reader = new StringFileReader(file);
		iterator = reader.iterator();
		this.refPosition = refPosition;
//		this.altPosition = altPosition;
//		maxPosition = Math.max(Math.max(idPosition, refPosition), altPosition);
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public ChrPosition next() {
		String vcf = iterator.next();
		if (null != vcf) {
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
//			alt = null;
			
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
//				} else if (loops == altPosition) {
//					alt = vcf.substring(lastTabPosition + 1, tabPosition);
				}
				
				if (loops >= refPosition) {
					break;
				}
				
				loops++;
				lastTabPosition = tabPosition;
				tabPosition = vcf.indexOf("\t", lastTabPosition + 1);
			}
			
			if (null != chr && position > -1 && null != ref) {
				return new ChrPositionName(chr, position, position, id + "\t" + ref);
//				return new ChrPositionName(chr, position, position, id + "\t" + ref + "\t" + alt);
			}
		}
		return null;
	}

	@Override
	public List<String> order() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws IOException {
		if (null != reader) {
			reader.close();
		}
	}

	@Override
	public void sort(List<String> contigOrder) {
		System.err.println("Not yet possible to sort stream (unless we are dealing with block zipped vcfs)");
	}

}
