/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbamfilter.filter;

import net.sf.picard.filter.SamRecordFilter;
import net.sf.samtools.SAMRecord;

public class FlagFilter  implements SamRecordFilter{
    
    private final SAMFlag flag;
    private final boolean FlagValue;
    private final Comparator op;

    public enum SAMFlag{
        ReadPaired,
        ProperPair,
        ReadUnmapped,
        Mateunmapped,
        ReadNegativeStrand,
        MateNegativeStrand,
        FirstOfpair,
        SecondOfpair,
        NotprimaryAlignment,
        ReadFailsVendorQuality,
        DuplicateRead;
        
        /**
     * Here we assume all read with valid Flag. Any query regard to Mate read information, 
      * we check whether this read is paired or not. If not paired, it return false. So that 
      * you must check whether this SAMRead is valid or not.
      * eg. flag = 0x0000;
      * query: Flag_ProperPaire == true; // it return false even it isn't a Paired read, rather exception
      * query: Flag_ProperPaire != true; // it return true even it isn't a Paired read, rather exception
      * eg. flag = 0x0002;
      * query: Flag_properPaire == true; // it return false even it is an invalid flag
     */
        public boolean checkFlag(final SAMRecord record){
            boolean ReadPairedFlag = record.getReadPairedFlag();
            switch(this){
                case ReadPaired: return ReadPairedFlag;
                case ProperPair: return ReadPairedFlag ? record.getProperPairFlag() : false;
                case ReadUnmapped: return ReadPairedFlag ? record.getReadUnmappedFlag() : false;
                case Mateunmapped: return ReadPairedFlag ? record.getMateUnmappedFlag() : false;
                case ReadNegativeStrand: return record.getReadNegativeStrandFlag();
                case MateNegativeStrand: return ReadPairedFlag ? record.getMateNegativeStrandFlag() : false;
                case FirstOfpair: return ReadPairedFlag ? record.getFirstOfPairFlag():false;
                case SecondOfpair: return ReadPairedFlag ? record.getSecondOfPairFlag() : false;
                case NotprimaryAlignment: return record.getNotPrimaryAlignmentFlag();
                case ReadFailsVendorQuality: return record.getReadFailsVendorQualityCheckFlag();
                case DuplicateRead: return record.getDuplicateReadFlag();
            }
            throw new AssertionError("Unknow flag:" + this);
        }

        /**
         * @param flagName: valid String must belong below 11 type of string ignoring letter case:
         * [ "ReadPaired", "ProperPair","ReadUnmaped","Mateunmapped", "ReadNegativeStrand",
         * "MateNegativeStrand","FirstOfpair","SecondOfpair", "NotprimaryAlignment",
         * "ReadFailsVendorQuality","DuplicateRead"];
         * @return related SAMFlag based on paramter flagName String;
         * @throws Exception if paramter flagName is invalid
         */
        final static SAMFlag getFlag(String flagName) throws Exception{
            if(flagName.equalsIgnoreCase("ReadPaired")){
                return  ReadPaired;
            }
            else if(flagName.equalsIgnoreCase("ProperPair")){
                return ProperPair;
            }
            else if(flagName.equalsIgnoreCase("ReadUnmapped")){
                return ReadUnmapped;
            }
            else if(flagName.equalsIgnoreCase("Mateunmapped")){
                return Mateunmapped;
            }
            else if(flagName.equalsIgnoreCase("ReadNegativeStrand")){
                return ReadNegativeStrand;
            }
            else if(flagName.equalsIgnoreCase("MateNegativeStrand")){
                return MateNegativeStrand;
            }
            else if(flagName.equalsIgnoreCase("FirstOfpair")){
                return FirstOfpair;
            }
            else if(flagName.equalsIgnoreCase("SecondOfpair")){
                return SecondOfpair;
            }
            else if(flagName.equalsIgnoreCase("NotprimaryAlignment")){
                return NotprimaryAlignment;
            }
            else if(flagName.equalsIgnoreCase("ReadFailsVendorQuality")){
                return ReadFailsVendorQuality;
            }
            else if(flagName.equalsIgnoreCase("DuplicateRead")){
                return DuplicateRead;
            }
            else{
                throw new Exception("invaid flag name: " + flagName  +
                        "in query condition Flag_" + flagName );
            }
        }
    }
    
    /**
     * initilize flag name, comparator and flag value
     * @parm flagName : At moment the valid name are [M,I,D,N,S,H,P].
     * @param comp: only valid on Comparator.Equal and Comparator.NotEqual.
     * See details on org.qcmg.qbamfilter.filter::Comparator.
     * @param value:  only valid at ["true","false", "1", "0" ]
     * @throws Exception if the invalid parameter are exists;
     * See usage on method filterout.
     */
    public FlagFilter( String flagName,  Comparator comp,  String value )throws Exception{
        //only boolean type value are varable
        if(value.equalsIgnoreCase("true") || value.equals("1")){
            FlagValue = true;
        }
        else if(value.equalsIgnoreCase("false") || value.equals("0")){
            FlagValue = false;
        }
        else{
            throw new Exception("can't accept non-boolean key value for query key name: "
                    + flagName + ". please try 'true', 'false', '1' or '0'");
        }
        
        //get comparator type
        if((comp == Comparator.Equal) || (comp == Comparator.NotEqual)){
            op = comp;
        }
        else{
            throw new Exception("invalid flag comparator: " + flagName +
                    ". Please try '==' or '!='");
        }

        try{
            flag = SAMFlag.getFlag(flagName);
        }catch(Exception e){throw e;}
    }

    /**
     * check the record Flag value. 
     * @param record: a SAMRecord
     * @return true if the Flag is satified by the condition
     * Usage example: if you want filter out all reads with matched base greater equal than 35mers. 
     * SAMRecordFilter myfilter = new FlagFilter("PorperPair", Comparator.Equal, "true" );
     * if(myfilter.filterout(record)){ System.out.println(record.toString);}
     */
    @Override
	public boolean filterOut(final SAMRecord record){
        boolean result = flag.checkFlag(record);

        return op.eval(FlagValue, result);
    }
    
    /**
     * It is an inherited method and return false only. 
     */
	@Override @Deprecated
	public boolean filterOut(SAMRecord arg0, SAMRecord arg1) {
		// TODO Auto-generated method stub
		return false;
	}
}
