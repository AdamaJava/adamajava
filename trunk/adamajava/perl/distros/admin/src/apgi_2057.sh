#!/bin/bash

###########################################################################
#
#  Script:  apgi_2057.sh
#  Creator: John Pearson
#  Created: 2012-08-18
#
#  This is a bash script for running qBamMaker.pl to create exome and
#  genome level BAMs for APGI_2057
#
#  $Id: apgi_2057.sh 2971 2012-09-10 09:30:31Z j.pearson $
#
###########################################################################

#PBS -N qbm_apgi_2057.pbs
#PBS -S /bin/bash
#PBS -q batch
#PBS -r n
#PBS -l walltime=72:00:00,nodes=1:ppn=4,mem=41gb
#PBS -m ae
#PBS -M j.pearson@uq.edu.au

# Resource usage recipes:
# For qcoverage -n 7 : -l walltime=24:00:00,nodes=1:ppn=4,mem=41gb

MY_EXOME_DIR=$QCMG_HOME/ExomePEBC
PICARD=/share/software/picard-tools-1.29


function qbm1 {
    module load adama/nightly
    local DONOR=$1
    local LIB=$2
    local CRIT=$3
    local DIR=$4
    local NAME=$5
    local QBM=$QCMG_SVN/QCMGPerl/distros/admin/src/qBamMaker.pl
    
    local CMD1="$QBM -q 0 \
                     -d $DONOR \
                     -b $LIB \
                     $CRIT \
                     --pbs ${DONOR}.${LIB}.${NAME}.pbs \
                     --outdir $DIR \
                     --name $NAME"
    echo Executing: $CMD1
    eval $CMD1
}


# 2012-08-09
# This routine makes the seq_final BAMs for the QCMG HiSeq sequencing of
# the APGI_2057 mixtures experiment.  It includes whole genome and
# TargetSeq exome.

function make_APGI_2057_hiseq_BAMs {
    # Use "Amplified Library" from "Cellularity Experiment" wikipage in
    # qBamMaker.pl invocations
    local BAM_OUT=/mnt/seq_results/smgres_special/mixture_model_hiseq/seq_final
 
    #qbm1 APGI_2057 Library_20120605_A genome $BAM_OUT/APGI_2057.hiseq_genome_100CD_0ND.jvp.bam
    #qbm1 APGI_2057 Library_20120605_B genome $BAM_OUT/APGI_2057.hiseq_genome_80CD_20ND.jvp.bam
    #qbm1 APGI_2057 Library_20120605_C genome $BAM_OUT/APGI_2057.hiseq_genome_60CD_40ND.jvp.bam
    #qbm1 APGI_2057 Library_20120615_D genome $BAM_OUT/APGI_2057.hiseq_genome_40CD_60ND.jvp.bam
    #qbm1 APGI_2057 Library_20120615_E genome $BAM_OUT/APGI_2057.hiseq_genome_20CD_80ND.jvp.bam
    #qbm1 APGI_2057 Library_20120615_F genome $BAM_OUT/APGI_2057.hiseq_genome_0CD_100ND.jvp.bam

    qbm1 APGI_2057 Library_20120605_A '-c TargetSEQ' $BAM_OUT hiseq_exome_100CD_0ND
    qbm1 APGI_2057 Library_20120605_B '-c TargetSEQ' $BAM_OUT hiseq_exome_80CD_20ND
    qbm1 APGI_2057 Library_20120605_C '-c TargetSEQ' $BAM_OUT hiseq_exome_60CD_40ND
    qbm1 APGI_2057 Library_20120615_D '-c TargetSEQ' $BAM_OUT hiseq_exome_40CD_60ND
    qbm1 APGI_2057 Library_20120615_E '-c TargetSEQ' $BAM_OUT hiseq_exome_20CD_80ND
    qbm1 APGI_2057 Library_20120615_F '-c TargetSEQ' $BAM_OUT hiseq_exome_0CD_100ND
}



##########################################################################
### Main #################################################################
##########################################################################

START_DATE=`/bin/date`
START_SECOND=`/bin/date +%s`

# 2012-08-09  APGI_2057 mixtures experiment HiSeq gemome/exome seq_final BAMs
make_APGI_2057_hiseq_BAMs

END_SECOND=`/bin/date +%s`
ELAPSED=$(($END_SECOND-$START_SECOND))
echo $PBS_JOBID \| $PBS_JOBNAME \| $PBS_QUEUE \| $START_DATE \| $ELAPSED >> $QCMG_HOME/pbs_jobs.log
