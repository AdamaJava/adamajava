oDir=/Users/christix/Documents/Eclipse/sourceForge/trunk/adamajava
nDir=/Users/christix/Documents/Eclipse/gitHub/AdamaJava/adamajava
projects="qannotate
qbamannotate
qbamfilter
qbamfix
qbammerge
qbasepileup
qcnv
qcommon
qcoverage
qio
qmaftools
qmito
qmotif
qmule
qpicard
qpileup
qprofiler
qprofiler2
qsignature
qsnp
qsplit
qsv
qtesting
qvisualise
qvisualise2"

for i in $projects; do 
rm $i/build.gradle
cp $oDir/$i/build.gradle  $i/
done
