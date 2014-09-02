package au.edu.qimr.qmito;

import java.util.Hashtable;
import java.util.Map;

import au.edu.qimr.qmito.BaseStatRecord.Base;


public class BaseStatRecord {
	public enum Base{BaseA, BaseC,BaseG,BaseT; }

	String ref;
	int position;
	char ref_base;
	
	private double chiValue;	
	private Map<Base, Integer> forward = new Hashtable<Base, Integer>();
	private Map<Base, Integer> reverse = new Hashtable<Base, Integer>();
	
	public void setForward(Base key, Integer value){ forward.put(key, value); }
	public long[] getForwardArray(){ return getBaseCounts(forward); }
	
	public void setReverse(Base key, Integer value){ reverse.put(key, value); }
	public long[] getReverseArray(){ return getBaseCounts(reverse); }
		
	public void setChiValue(double value){ this.chiValue = value;}
	public double getChiValue(){ return chiValue;}
	
	public String getForwardString(){
		return forward.get(Base.BaseA) + "\t" + forward.get(Base.BaseC) + "\t" 
	+ forward.get(Base.BaseG) + "\t" +forward.get(Base.BaseT) ;
	}
	
	public String getReverseString(){
		return reverse.get(Base.BaseA) + "\t" + reverse.get(Base.BaseC) + "\t" 
				+ reverse.get(Base.BaseG) + "\t" +reverse.get(Base.BaseT) ;
	}
	
	//all base increase one to avoid ZeroException for chiSqare
	private long[] getBaseCounts(Map<Base, Integer> baseCounts){
		long[] myarray = { (long) baseCounts.get(Base.BaseA) + 1,
				(long) baseCounts.get(Base.BaseC) + 1,
				(long) baseCounts.get(Base.BaseG) + 1,
				(long) baseCounts.get(Base.BaseT) + 1 };
		
		return myarray;
	}

}
