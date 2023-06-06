package au.edu.qimr.qannotate.nanno;

import java.util.List;

public class AnnotationInputs {
	
	private String outputFieldOrder;
	private String additionalEmptyFields;
	private boolean includeSearchTerm;
	private int annotationSourceThreadCount;
	private List<AnnotationInput> inputs;
	
	public List<AnnotationInput> getInputs() {
		return inputs;
	}
	
	public String getOutputFieldOrder() {
		return outputFieldOrder;
	}
	
	public String getAdditionalEmptyFields() {
		return additionalEmptyFields;
	}
	
	public boolean isIncludeSearchTerm() {
		return includeSearchTerm;
	}
	
	public int getAnnotationSourceThreadCount() {
		return annotationSourceThreadCount;
	}


	public static class AnnotationInput {
		private String file;
		private int chrIndex;
		private int positionIndex;
		private int refIndex;
		private int altIndex;
		private boolean snpEffVcf;
		private String fields;
		
		public String getFile() {
			return file;
		}
		
		public int getChrIndex() {
			return chrIndex;
		}
		
		public String getFields() {
			return fields;
		}
		
		public int getPositionIndex() {
			return positionIndex;
		}
		
		public int getRefIndex() {
			return refIndex;
		}
		
		public int getAltIndex() {
			return altIndex;
		}
		
		public boolean isSnpEffVcf() {
			return snpEffVcf;
		}
		
	}
}
