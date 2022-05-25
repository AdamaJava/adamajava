

#run qpileup, take input as qpileup project root, eg $(pwd)/adamajava
pileup=$1/qpileup/build/flat/qpileup.jar;
path=$1/qpileup/test/resources

############################
#run bootstrap first
##############################
hdf=$path/pileup.boostrap.h5
ini=$path/pileup.bootstrap.ini
cat <<- EOF > $ini
[general]
hdf=$hdf
log=$hdf.log
mode=bootstrap

[bootstrap]
reference=$path/test-reference.fa
low_read_count=10
nonref_percent=30
EOF
java -jar $pileup --ini $ini
#check h5 header
java  -jar $pileup  --view --hdf-header --hdf $hdf > ${hdf}.header;

#####################
#run add mode once 
##########################
cp $path/test.bam $path/pileup.A.bam
cp $path/test.bam.bai $path/pileup.A.bam.bai
hdf=$path/pileup.A.h5
cp $path/pileup.boostrap.h5 $hdf

ini=$path/pileup.addA.ini
cat <<- EOF > $ini
[general]
hdf=$hdf
log=$hdf.add.log
mode=add
thread_no=1

[add_remove]
name=$path/pileup.A.bam
name=$path/test.bam
EOF
#run and check h5 header
java -jar $pileup --ini $ini
java  -jar $pileup  --view --hdf-header --hdf $hdf > ${hdf}.header;

#####################
#run add mode again
##########################
cp $path/test.bam $path/pileup.B.bam
cp $path/test.bam.bai $path/pileup.B.bam.bai
hdf=$path/pileup.B.h5
cp $path/pileup.boostrap.h5 $hdf

ini=$path/pileup.addB.ini
cat <<- EOF > $ini
[general]
hdf=$hdf
log=$hdf.add.log
mode=add
thread_no=1

[add_remove]
name=$path/pileup.B.bam
EOF
#run and check h5 header
java -jar $pileup --ini $ini
java  -jar $pileup  --view --hdf-header --hdf $hdf > ${hdf}.header;

#####################
#run remove mode
##########################
hdf=$path/pileup.A.h5
ini=$path/pileup.removeA.ini
cat <<- EOF > $ini
[general]
hdf=$hdf
log=$hdf.remove.log
mode=remove
thread_no=1

[add_remove]
name=$path/pileup.A.bam
EOF
#run and check h5 header
java -jar $pileup --ini $ini
java  -jar $pileup  --view --hdf-header --hdf $hdf > ${hdf}.remove.header;



#####################
#run merge mode
##########################
hdf=$path/pileup.merge.h5
ini=$path/pileup.merge.ini
cat <<- EOF > $ini
[general]
hdf=$hdf
log=$hdf.log
mode=merge
thread_no=1

[bootstrap]
reference=$path/test-reference.fa
low_read_count=10
nonref_percent=30

[merge]
input_hdf=$path/pileup.A.h5
input_hdf=$path/pileup.B.h5
EOF
#run and check h5 header
java -jar $pileup --ini $ini
java  -jar $pileup  --view --hdf-header --hdf $hdf > ${hdf}.remove.header;

