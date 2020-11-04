/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;


public class RunGatk {
	
//	public static  String PATH="/panfs/home/oholmes/devel/QCMGScripts/o.holmes/gatk/pbs4java/";
//	public static final String PARAMS=" -l walltime=124:00:00 -v patient=";
//	public static int jobCounter = 1;
//	
//	// inputs
//	public static String patientId;
//	public static String mixture;
//	public static String normalBamFile;
//	public static String tumourBamFile;
//	public static String outputDir;
//	
//	public static String patientParams;
//	public static String nodeName;
//	public static String startPoint;
//
//	public static void main(String[] args) throws IOException, InterruptedException, Exception {
//		
//		if (args.length < 5) throw new IllegalArgumentException("USAGE: RunGatk <patient_id> <mixture> <normal_bam_file> <tumour_bam_file> <output_dir> [<path_to_scripts>]");
//		
//		patientId = args[0];
//		mixture = args[1];
//		normalBamFile = args[2]; 
//		tumourBamFile = args[3]; 
//		outputDir = args[4];
//		if (args.length == 6) {
//			PATH = args[5];
//		}
//		if (args.length == 7) {
//			PATH = args[6];
//		}
//		
//		patientParams = PARAMS + patientId + ",mixture=" + mixture;
//		
//		String mergeParams = patientParams + ",normalBam=" + normalBamFile + ",tumourBam=" + tumourBamFile;
//		
//		
//		String jobName = jobCounter++ + "RG_" + mixture;
//		System.out.println("About to submit merge job");
//		
//		Job merge = new Job(jobName, PATH + "run_gatk_merge_1.sh" + mergeParams);
////		merge.setQueue(queue);
//		merge.queue();
//		String status = merge.getStatus();
//		System.out.println("1st job status: " + status);
//		while ("N/A".equals(status)) {
//			Thread.sleep(1500);
//			String [] jobs = Job.SearchJobsByName(jobName, true);
//			System.out.println("Sleeping till job status changes..." + status + ", id: " + merge.getId() + " no of jobs: " + jobs.length);
//			
//			for (int i = 0 ; i < jobs.length ; i++) {
//				System.out.println("jobs[" + i + "] : " + jobs[i]);
//				merge = Job.getJobById(jobs[i]);
//				status = merge.getStatus();
//				System.out.println("job.getJobStatus: " + Job.getJobStatus(jobs[i]));
//				
//			}
//		}
//		nodeName = merge.getExecuteNode().substring(0, merge.getExecuteNode().indexOf('/'));
//		
//		
//		
//		System.out.println("About to submit clean 1 job");
//		// clean 1
//		String script = PATH + "run_gatk_clean_1.sh" + patientParams;
//		Job clean1 = submitDependantJob(merge, "1", script, true);
//
//		
//		System.out.println("About to submit clean 2 job");
//		// clean 2
//		script = PATH + "run_gatk_clean_2.sh" + patientParams;
//		Job clean2 = submitDependantJob(clean1, "1", script, true);
//		
//		// clean 3
//		script = PATH + "run_gatk_clean_3.sh" + patientParams;
//		Job clean3 = submitDependantJob(clean2, "6", script, true);
//		
////		String scriptToRun = PATH + "run_gatk_clean_4.sh" + patientParams;
//		
//		System.out.println("About to submit clean 4 job");
//		script = PATH + "run_gatk_clean_4.sh" + patientParams;
//		Job clean4 = submitDependantJob(clean3, "1", script, true);
//		
//		// split
//		System.out.println("About to submit split job");
//		script = PATH + "run_gatk_split.sh" + patientParams;
//		Job split = submitDependantJob(clean4, "1", script, true);
//		
//		runMergeDelUG(split, "ND");
//		runMergeDelUG(split, "TD");
//	}
//
//	private static void runMergeDelUG(Job splitJob, String type) throws IOException, InterruptedException, Exception {
//		String script = PATH + "run_gatk_merge_2.sh" + patientParams + ",type=" + type;
//		Job mergeJob = submitDependantJob(splitJob, "1", script, true);
//		
//		// delete
//		script = PATH + "run_gatk_del_split_files.sh" + patientParams + ",type=" + type;
//		Job deleteJob = submitDependantJob(mergeJob, "1", script, true);
//		
//		
//		// UG
//		script = PATH + "run_gatk_UG.sh" + patientParams + ",type=" + type;
//		Job unifiedGenotyperJob = submitDependantJob(mergeJob, "4", script, false);
//		
//	}
//
//	private static Job submitDependantJob(Job depJob, String ppn, String script, boolean onNode) throws IOException, InterruptedException, Exception {
//		
//		String jobName;
//		ArrayList<String> dependantJobs;
//		String[] jobs;
//		jobName = jobCounter++ + "RG_" + mixture;
//		Job newJob = new Job(jobName, script);
////		Job newJob = new Job(jobName, PATH + script + patientParams + ",type=" + type);
////		newJob.setQueue(queue);
//		if (onNode) {
//			newJob.setExecuteNode(nodeName);
//			newJob.setNodes(nodeName);
//		}
//		newJob.setPpn(ppn);
//		dependantJobs = new ArrayList<String>();
//		dependantJobs.add(depJob.getId() + " ");
//		newJob.setAfterOK(dependantJobs);
//		newJob.queue();
//		// sleep to allow job to make it to the queue
//		Thread.sleep(1000);
//		
//		jobs = Job.SearchJobsByName(jobName, true);
//		newJob = Job.getJobById(jobs[0]);
//		return newJob;
//	}
	
}
