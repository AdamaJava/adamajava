package org.qcmg.base;


public abstract class Record {
	private final CharSequence discriminator;
	private String data; 
	
	Record() { this.discriminator = "\t"; }
	
	Record(String str) { this(); setData(str); }
	
	Record(CharSequence cs) { this.discriminator = cs; }	
	
	Record(String str, CharSequence cs ) { this(cs); setData(str); }
			
	public String getData() { return data; }	
	
	public void setData(String data) { this.data = data; }
	
	public String[] getDataArray() {
		return data.replace("\"", "").split(discriminator.toString());
	}
		 	
}
