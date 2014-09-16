#! /usr/bin/perl -w

=head1 USAGE

 perl write_sam_conversion_config.pl
    -x xmateMappingOutputDirectory
    -o samOutputDirectory
    -c originalXmateConfigFile
    -q qualityFile
    -l tagLength
    -t tmpDirLocation
    [-p numThreads] (default 1)
    [-multiSAMFile] (default single SAM file)
    [-g gffToSamLocation] (default $PATH location of GffToSam)

=head1 DESCRIPTION

Write a configuration file for the utility SamConverter.jar.  
This script reads the contents of an X-MATE mapped directory, and 
the mapping runs original configuration file, then
writes the new configuration file (with addition of some command line
arguments) for the utility SamConverter.jar.  Using this utility (available
in the X-MATE distribution), you can create SAM (Sequence Alignment Format) files.  
SAM files can then be converted to BAM files, which can be rapidly interrogated using
third party software, loading into genome browsers and more.

=head1 AUTHORS

=over 3

=item David Wood (d.wood@imb.uq.edu.au)

=back

=head1 COPYRIGHT

This software is copyright 2010 by the Queensland Centre for Medical
Genomics. All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.  For commercial or governmental 
use, please contact licensing\@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/

=cut

use strict;
use warnings;
use Getopt::Std;
use QCMG::X_Mate::XConfig::mainConf;
use Data::Dumper;

use constant { TRUE => 'true', FALSE => 'false' };

my $usage = "usage: perl $0 \n".
    "\t-x xmateMappingOutputDirectory\n".
    "\t-o samOutputDirectory\n".
    "\t-c originalXmateConfigFile\n".
    "\t-q qualityFile\n".
    "\t-l tagLength\n".
    "\t-t tmpDirLocation\n".
    "\t[-p numThreads] (default 1)\n".
    "\t[-multiSAMFile] (default single SAM file)\n".
    "\t[-g gffToSamLocation] (default from 'which gffToSam)\n";

getopts("x:o:c:q:l:t:p:g:multiSAMFile");

our ($opt_x, $opt_o, $opt_c, $opt_q, $opt_l, $opt_t, $opt_p, $opt_multiSAMFile, $opt_g);

my $originalDir = $opt_x;
my $outputDir = $opt_o;
my $configFile = $opt_c;
my $qualFile = $opt_q;
my $tagLength = $opt_l;
my $tmpDir = $opt_t;
my $numThreads = $opt_p;
my $multiSam = $opt_multiSAMFile;
my $gffToSam = $opt_g;

# check the parameters, set defaults:
unless (defined($originalDir) && defined($configFile) && defined($qualFile) 
    && defined($tagLength) && defined($tmpDir) && defined($outputDir)) { die $usage; }
unless (-e $outputDir) { system "mkdir $outputDir"; }
unless (-d $outputDir) { die "Cannot locate outputDir [$outputDir]. Aborting\n"; }
if (defined($multiSam)) { $multiSam = TRUE; } else { $multiSam = FALSE; }
unless (defined($numThreads)) { $numThreads = 1; }
else { unless ($numThreads =~ /^\d+$/) { die "-p numThreads must be an integer\n"; } }
unless ($tagLength =~ /^\d+$/) { die "-l tagLength must be an integer\n"; }
unless (defined($gffToSam)) { $gffToSam = `which GffToSam`; chomp $gffToSam; }
unless (-e $gffToSam) { die "Cannot locate binary for GffToSam.  Aborting\n"; }
unless (-d $outputDir) { die "Cannot locate xmateMappingOutputDirectory [$outputDir]. Aborting\n"; }
unless (-e $configFile) { die "Cannot locate originalXmateConfigFile [$configFile]. Aborting\n"; }
unless (-e $qualFile) { die "Cannot location qualityFile [$qualFile]. Aborting\n"; }

# load the original configuration file:
my $config = QCMG::X_Mate::XConfig::mainConf->new('fname'=> $configFile);

# now get the other required information from the xmate output directory:
my @collatedFiles = glob("$originalDir/*.collated");

# now print the SAM configuration file:
my $samConf = $outputDir."/".$config->exp_name.".sam.conf";
open(CONF, ">$samConf") 
    or die "Failed to write to configuration file: [$samConf]: $!\n";

&printHeader();
&printInputs();
if ($config->map_junction eq TRUE) {
    &printJunctions();
}
&printSAMTools();

print CONF "\n\n# end of configuration file\n";

close CONF;

print "wrote $samConf\n";

########################################

sub printHeader { 
    print CONF "#################################################################\n";
    print CONF "# SAM Conversion configuration file for X-MATE                  #\n";
    print CONF "# automatically generated using write_sam_configuration_file.pl #\n";
    print CONF "#################################################################\n\n";
}


sub printInputs {
    if ($config->map_ISAS eq FALSE) {
        print CONF "[inputs]\n\tgenomes ";
        foreach my $chr (@{$config->genomes}) { print CONF "= $chr\n\t\t"; }
    }
    else {
        print CONF "[inputs]\n\tgenomes = PLEASE ADD GENOME FILES HERE\n";
        print "\nNOTE: Configuration file requires manual addition of genome files\n";
    }
    
    if ($config->map_junction eq TRUE) {
        foreach my $junc (@{$config->junc_lib}) { print CONF "= $junc\n\t\t"; }
    }
    print CONF "\n\n\tcollated_file ";
    foreach my $colFile (@collatedFiles) { print CONF "= $colFile\n\t\t"; }
    
    print CONF "\n\tQV_files = $qualFile\n";
    print CONF "\n\texp_name = ".$config->exp_name."\n";
    print CONF "\n\toutput_dir = $outputDir\n";
    print CONF "\n\tParallelNumber = $numThreads\n";
    print CONF "\n\tMaxTagLength = $tagLength\n";
    print CONF "\n\tMultiSAM = $multiSam\n\n";
}


sub printJunctions { 
    print CONF "[junctions]\n";
    print CONF "\tNameOfJunction ";
    foreach my $junctionPathName (@{$config->junc_index}) {
        my $file = `basename $junctionPathName`;
        chomp $file;
        if ($file =~ /(.*?)(\.fa)?\.index/) {
            print CONF "= $1\n\t\t";
        }
        else {
            die "Failed to parse junction name from file $file\n";
        }
    }
    print CONF "\n\tIndexOfJunction ";
    foreach my $junctionPathName (@{$config->junc_index}) {
        print CONF "= $junctionPathName\n\t\t";
    }
}


sub printSAMTools { 
    print CONF "\n[SAM tools]\n";
    print CONF "\n\tGffToSam = $gffToSam\n";
    print CONF "\n\tTempDir = $tmpDir\n";
}



