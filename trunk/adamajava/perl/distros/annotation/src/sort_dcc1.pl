#!/usr/bin/perl

use strict;
use Getopt::Long;
use Data::Dumper;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4004 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: sort_dcc1.pl 4667 2014-07-24 10:09:43Z j.pearson $' =~ /\$Id:\s+(.*)\s+/;

my $dcc1;
my $dccout;
my $logfile;

&GetOptions( "i=s" => \$dcc1, "o=s" => \$dccout, "log=s" => \$logfile );

if(! $dcc1 || ! $dccout) {
	qlogprint("ERROR, input and output files required: $0 -i file.dcc1 -o sortedfile.dcc1\n");
	exit(1);
}

my $cmdline	= join(' ',@ARGV);
qlogfile($logfile) if($logfile);
qlogbegin();
qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n" );
qlogprint( "Run parameters:\n" );
qlogprint( "-i   : $dcc1\n" );
qlogprint( "-o   : $dccout\n" );
qlogprint( "-log : $logfile\n" );

# create a uniquely name temp file to write records to for sorting; can't echo
# to command line because there are too many
my $tmpfile	= ".sortdcc1_".time.rand;
qlogprint( "Temp file: $tmpfile\n" );

my $header;
my $records_numerchr;
my $records_X;
my $records_Y;
my $records_M;
my $records_contig;
my $records_other;

qlogprint( "Reading $dcc1...\n" );
open(FH, $dcc1) || die "Cannot read $dcc1: $!\n";
while(<FH>) {
	if(/^#/ || /^analysis/) {
		$header 		.= $_;
	}
	elsif(/chr\d+/) {
		$records_numerchr	.= $_;
	}
	elsif(/chrX/) {
		$records_X		.= $_;
	}
	elsif(/chrY/) {
		$records_Y		.= $_;
	}
	elsif(/chrM/) {
		$records_M		.= $_;
	}
	elsif(/\tGL00/) {
		$records_contig		.= $_;
	}
	else {
		$records_other		.= $_;
	}
}
close(FH);

# set up sorting routines for each set of chromsome names
my $sed_numerchr	= q{sed 's/chr/chr\t/g' }.$tmpfile.q{ | sort -k6n -k7n | sed 's/chr\t/chr/g'};
my $sed_X		= q{sort -k7n }.$tmpfile;
my $sed_Y		= q{sort -k7n }.$tmpfile;
my $sed_M		= q{sort -k7n }.$tmpfile;
my $sed_contig		= q{sed 's/GL\([0-9]*\)\(\.[0-9]*\)/GL\t\1\t\2/g' }.$tmpfile.q{ | sort -k6n -k8n | sed 's/GL\t\([0-9]*\)\t\(\.[0-9]*\)/GL\1\2/g'};

my @labels		= ("numerical chromosome", "chrX", "chrY", "chrM/chrMT", "Contigs");
my @rec_types		= ($records_numerchr, $records_X, $records_Y, $records_M, $records_contig);
my @sort_types		= (    $sed_numerchr,     $sed_X,     $sed_Y,     $sed_M,     $sed_contig);


qlogprint("Writing sorted records to $dccout\n");
open(FH, ">".$dccout) || die "Cannot write to $dccout: $!\n";

my $numlines = ($header =~ tr/\n//);
qlogprint("Number of header lines: $numlines\n");
print FH $header;

for my $i (0..$#rec_types) {
	my $a_numlines = ($rec_types[$i] =~ tr/\n//);
	qlogprint("Number of $labels[$i] records before sorting: $a_numlines\n");

	open(TMP, ">".$tmpfile) || die "Cannot open $tmpfile: $!\n";
	print TMP $rec_types[$i];
	close(TMP);

	my $cmd		= $sort_types[$i];
	qlogprint("Sort command: $cmd\n");
	my $sorted	= `$cmd`;
	my $b_numlines	= ($sorted =~ tr/\n//);	# will be one extra due to newline from $cmd
	qlogprint("Number of $labels[$i] records after sorting: $b_numlines\n");

	exit(1) if($a_numlines != $b_numlines);

	print FH $sorted;
}

my $numlines = ($records_other =~ tr/\n//);
qlogprint("Number of other records: $numlines\n");
if($numlines > 0) {
	qlogprint("Writing any other records (not sorted)\n");
	print FH $records_other;
}
close(FH);

qlogprint("Removing temp file\n");
unlink($tmpfile);

qlogprint("Done\n");
exit(0);

__END__

=head1 NAME

sort_dcc1.pl - sort DCC1 file by genomic coordinates

=head1 DESCRIPTION

This script reads a DCC1 file and sorts it by genomic coordinate such that the
final order of chromosomes is:

 chrN, ..., chrNN, chrX, chrY, chrM*, GL000xxx.x, ..., GL000yyy.y, anything else

This assumes that the chromosome name is in column 5, the start position is
 in column 6, the columns are tab-delimited, header columns start with #, and 
 the column names header line starts with "analysis".

=head2 Commandline parameters

 -i        Input file in DCC1 format
 -o        Output file in DCC2 format
 -log      Logfile

=over

=item B<-i>

Mandatory. The input file is in DCC1 format (see QCMG wiki)

=item B<-o>

Mandatory. The output file (sorted DCC1 file)

=back

=item B<-log>

Optional. The log file 

=back


=head1 AUTHORS

=over 2

=item Lynn Fink, L<mailto:l.fink@imb.uq.edu.au>

=back


=head1 VERSION

$Id: sort_dcc1.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
