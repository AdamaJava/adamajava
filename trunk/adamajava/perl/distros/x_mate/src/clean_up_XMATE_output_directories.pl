#! /usr/bin/perl -w

=head1 NAME

clean_up_XMATE_output_directories.pl

=head1 USAGE

 perl clean_up_XMATE_output_directories.pl
	-p full path of the directory to be cleaned

=head1 DESCRIPTION

This program cleans up completed X-MATE directories.

=head1 AUTHORS

=over 3

=item Nicole Cloonan (n.cloonan@imb.uq.edu.au)

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

use Getopt::Std;

getopt("p");
getopts("p:");

# if insufficient options, print usage

if (!($opt_p)) {
    print "\n\nUsage: $0\n\n";
    print "\tREQUIRED:\n";
    print "\t-p full path of the directory to be checked\n";
    die "\n\n";
}

# set paramaters

$path = $opt_p;

print STDOUT "\n\n\nCleaning up directory:\t$path\n";
print STDOUT "Removing csfasta files...\n";
unlink glob("$path/*.csfasta") or print "Unable to delete $path/*.csfasta because $!\n";
unlink glob("$path/*.fastq") or print "Unable to delete $path/*.fastq probably because there is none [$!]\n";
print STDOUT "Removing temporary wiggle files...\n";
unlink glob("$path/chr*") or print "Unable to delete $path/chr*.negative because $!\n";
unlink("$path/nohup.out") or print "Unable to delete $path/nohup.out probably because there is none\n";

print STDOUT "\nClean up of directory $path complete.\n\n\n";

exit(0);
