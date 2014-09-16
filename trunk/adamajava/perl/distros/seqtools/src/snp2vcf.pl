#!/usr/bin/perl

##############################################################################
#
#  Program:  snp2vcf.pl
#  Author:   John V Pearson
#  Created:  2012-03-08
#
#  Read text files from Genome Studio for Illumina Omni 1M SNP chips and
#  creates VCF files suitable for use with qsignature.
#
#  $Id: snp2vcf.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
#use warnings;

##use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX;

use QCMG::IO::GenomeStudioReader;
use QCMG::IO::VcfWriter;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE %PARAMS );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: snp2vcf.pl 4669 2014-07-24 10:48:22Z j.pearson $'
	=~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

	# Setup defaults for important variables.

	$PARAMS{infile}  = '';
	$PARAMS{outfile} = '';
	$PARAMS{logfile} = '';
	$PARAMS{patient} = '';
	$PARAMS{type}	= '';
	$PARAMS{library} = '';
	$VERBOSE = 0;
	$VERSION = 0;
	my $help = 0;
	my $man  = 0;

	# Print usage message if no arguments supplied
	pod2usage(1) unless (scalar @ARGV > 0);

	# Use GetOptions module to parse commandline options

	my $cmdline = join(' ',@ARGV);
	my $results = GetOptions (
		   'i|infile=s'		   => \$PARAMS{infile},  # -i
		   'o|outfile=s'		  => \$PARAMS{outfile}, # -o
		   'l|logfile=s'		  => \$PARAMS{logfile}, # -l
		   'p|patient=s'		  => \$PARAMS{patient}, # -p
		   't|type=s'			 => \$PARAMS{type},	# -t
		   'b|library=s'		  => \$PARAMS{library}, # -b
		   'v|verbose+'		   => \$VERBOSE,		 # -v
		   'version!'			 => \$VERSION,		 # --version
		   'h|help|?'			 => \$help,			# -?
		   'man|m'				=> \$man			  # -m
		   );
	
	pod2usage(1) if $help;
	pod2usage(-exitstatus => 0, -verbose => 2) if $man;

	if ($VERSION) {
		print "$SVNID\n";
		exit;
	}

	qlogfile($PARAMS{logfile}) if $PARAMS{logfile};
	qlogbegin;
	qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");

	# Read the file, work out what sort of XML we are dealing with and
	# if we have an appropriate parser, call it.

	die "input file not specified\n" unless $PARAMS{infile};
	die "output file not specified\n" unless $PARAMS{outfile};

	#create and return formatted VCF lines
	my $text	= &process_snp_file();

	## sort VCF records
	#my $sortedtext	= &sort_vcf($text);
	## write sorted record to VCF file
	#&write_vcf($sortedtext);

	&write_vcf($text);
}

=cut
sub sort_vcf {
	my $text	= shift @_;

	my @lines	= split /\n/, $text;

	# can't use vcf-sort (from vcf-tools) because our version of Unix "sort"
	# is not current enough

	# chr1    47851   cnvi0146654     C               null    .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
	# chr1    50251   cnvi0146656     T               null    .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0

	# DO STUFF HERE....

	return($sortedtext);
}
=cut

# write headers and pre-formatted VCF lines to a file
sub write_vcf {
	my $text	= shift @_;

	my $vcf = QCMG::IO::VcfWriter->new(
						filename	=> $PARAMS{outfile},
						verbose		=> $VERBOSE 
					);

	$vcf->header( 'patient_id',		$PARAMS{patient} );  
	$vcf->header( 'input_type',		$PARAMS{type} );  
	$vcf->header( 'library',		$PARAMS{library} );  

	$vcf->header( 'filter_q_score',		10 );
	$vcf->header( 'filter_match_qual', 	10 );
	$vcf->header( 'snp_file',		'/panfs/share/qsignature/qsignature_positions.txt' );
	$vcf->header( 'genome',   		'/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa' );

	$vcf->filter( 'LowQual', 		'Description="REQUIRED: QUAL < 50.0"' );
	$vcf->info( 'FULLCOV', 			'Number=.,Type=String,Description="all bases at position"' );
	$vcf->info( 'NOVELCOV', 		'Number=.,Type=String,Description="bases at position from reads with novel starts"' );

	$vcf->write_headers;

	$vcf->write_text( "$text\n" );

	$vcf->close;

	return();
}

# read GenomeStudio file and translate it into VCF lines for each qualifying SNP
sub process_snp_file {
	my $text;

	# find out which SNPs have to be complemented and get reference base at
	# that genomic coordinate
	my ($comp_snp, $refbase)	= &read_complement_rules();

	# read GenomeStudio file
	my $infh = QCMG::IO::GenomeStudioReader->new( filename => $PARAMS{infile} );
	while (my $rec = $infh->next_record) {
		# Skip any records with missing data
		next unless ( defined $rec->{'B Allele Freq'} and
					  defined $rec->{'Log R Ratio'} and
					  defined $rec->{'SNP'} and
					  defined $rec->{'SNP Name'} and
					  defined $rec->{'Chr'} and
					  defined $rec->{'Allele1 - AB'} and
					  defined $rec->{'Allele2 - AB'} and
					  defined $rec->{'Position'} and
					  defined $rec->{'SNP Name'} );

		# Skip any records with weird data
		next if ( $rec->{'B Allele Freq'}	eq 'NaN' or
			  $rec->{'Log R Ratio'}		eq 'NaN' or
			  $rec->{Chr}			eq '0' );

		# I'm not even going to try to cope with indels or trinary SNPs
		# etc so if it's not a vanilla bi-allelic SNP, we ditch
		next unless $rec->{'SNP'} =~ /\[[ACGT]{1}\/[ACGT]{1}\]/;

		# skip SNPs with low confidence Genotyping Call Score;
		# 0.7 is the threshold that most people use in publication; increase
		# this threshold to get more confident calls
		next if($rec->{'GC Score'} < 0.70);
	
		# skip non-dbSNP positions
		next if($rec->{'SNP Name'} !~ /^rs/);
	
		# get bases that are probed for at this genomic coordinate
		$rec->{'SNP'}	=~ /\[(\w)\/(\w)\]/;
		my $snpvar1	= $1;
		my $snpvar2	= $2;
	
		# should the array genotype be complemented because, according to dbSNP,
		# the SNP is on the - strand; 0 = no complement; 1 = complement;
		# undefined = not determined so a decision cannot be made
		my $comp	= $comp_snp->{$rec->{'SNP Name'}};
		next if(! $comp);
	
		## determine genotype according to array
		# Unambiguous SNPs:
		# A/C -> SNP=TOP, A=ALLELE A and C=ALLELE B
		# A/G -> SNP=TOP, A=ALLELE A and G=ALLELE B
		# T/C -> SNP=BOT, T=ALLELE A and C=ALLELE B
		# T/G -> SNP=BOT, T=ALLELE A and G=ALLELE B
		my $array_genotype;
		if($snpvar1 =~ /[AT]/ && $snpvar2 =~ /[CG]/) {
			if($rec->{'Allele1 - AB'} eq 'A') {
				$array_genotype		= $snpvar1;
	
				if($rec->{'Allele2 - AB'} eq 'A') {
					# AA
					$array_genotype	.= $snpvar1;
					#print STDERR "Unambiguous: AA $array_genotype ($comp)\n";
				}
				elsif($rec->{'Allele2 - AB'} eq 'B') {
					# AB
					$array_genotype	.= $snpvar2;
					#print STDERR "Unambiguous: AB $array_genotype ($comp)\n";
				}
			}
			elsif($rec->{'Allele1 - AB'} eq 'B') {
				# BB
				$array_genotype		= $snpvar2.$snpvar2;
				#print STDERR "Unambiguous: BB $array_genotype ($comp)\n";
			}
		}
		# Ambiguous SNPs:
		# if SNP is A/T and SNP=TOP, A=ALLELE A and T=ALLELE B
		# if SNP is A/T and SNP=BOT, T=ALLELE A and A=ALLELE B
		# if SNP is C/G and SNP=TOP, C=ALLELE A and G=ALLELE B
		# if SNP is C/G and SNP=BOT, G=ALLELE A and C=ALLELE B
		elsif(($snpvar1 eq 'A' && $snpvar2 eq 'T') || ($snpvar1 eq 'C' && $snpvar2 eq 'G') || ($snpvar1 eq 'T' && $snpvar2 eq 'A') || ($snpvar1 eq 'G' && $snpvar2 eq 'C')) {
			if($rec->{'Allele1 - AB'} eq 'A' && $rec->{'ILMN Strand'} eq 'TOP') {
				$array_genotype		= $snpvar1;
	
				if($rec->{'Allele2 - AB'} eq 'A') {
					# AA
					$array_genotype	.= $snpvar1;
					#print STDERR "Ambiguous: AA $array_genotype ($comp)\n";
				}
				elsif($rec->{'Allele2 - AB'} eq 'B') {
					# AB
					$array_genotype	.= $snpvar2; # ** -> CG
					#print STDERR "Ambiguous: AB $array_genotype ($comp)\n";
				}
			}
			elsif($rec->{'Allele1 - AB'} eq 'A' && $rec->{'ILMN Strand'} eq 'BOT') {
				$array_genotype		= $snpvar2;
	
				if($rec->{'Allele2 - AB'} eq 'A') {
					# AA
					$array_genotype	.= $snpvar1;
					#print STDERR "Ambiguous: AA $array_genotype ($comp)\n";
				}
				elsif($rec->{'Allele2 - AB'} eq 'B') {
					# AB
					$array_genotype	.= $snpvar1;
					#print STDERR "Ambiguous: AB $array_genotype ($comp)\n";
				}
			}
			elsif($rec->{'Allele1 - AB'} eq 'B') {
				# BB
				$array_genotype		= $snpvar2.$snpvar2;
					#print STDERR "Ambiguous: BB $array_genotype ($comp)\n";
			}
		}
		else {
			# shouldn't happen, but here as a warning just in case
			$array_genotype			= "unhandled case";
		}
		# complement genotype if dbSNP says SNP is on - minus strand
		if($comp == 1) {
			$array_genotype			=~ tr/[ACGT]/[TGCA]/;
		}
	
		$array_genotype	=~ /(\w)(\w)/;
		my $base1	= $1;
		my $base2	= $2;
	
		# We'll arbitrarily set nominal coverage as 20 but we'll scale
		# it up or down according to logR (assumes it's a natural log)
		my $arbcov	= 20;
		my $totcov	= floor( $arbcov * exp( $rec->{'Log R Ratio'} ) );
		my $alt_count	= floor( $rec->{'B Allele Freq'} * $totcov );
		my $ref_count	= $totcov - $alt_count;
		my %cov		= ( A => 0, C => 0, G => 0, T => 0, N => 0 );
		$cov{$base1}	= $ref_count;
		$cov{$base2}	= $alt_count;
		my $covtext	= join(
					"",
						 'A:',$cov{A},
						',C:',$cov{C},
						',G:',$cov{G},
						',T:',$cov{T},
						',N:',$cov{N},
						',TOTAL:',$totcov
					);
	
		$text		.= join(
					"\t", 
						'chr'.$rec->{'Chr'} || '',
						 $rec->{'Position'} || '',
						 $rec->{'SNP Name'} || '',
						 $base1,
						 $base2,
						 '.',
						 '.',
						 "FULLCOV=$covtext;NOVELCOV=$covtext"
					);
		$text		.= "\n";
	}

	return($text);
}

# return a ref to a hash of SNP IDs that need to complemented for SNP array
# calls
sub read_complement_rules {
	my $cfile	= "/panfs/share/qsignature/Illumina_arrays_design.txt";

=cut
#dbSNP Id	   Reference Genome		dbSNP alleles   Chr	 Position(hg19)  dbSNP Strand	IlluminaDesign  ComplementArrayCalls?
rs1000000	   G	   C/T	 chr12   126890980	   -	   [T/C]   yes
rs10000004	  A	   A/G	 chr4	75406448		+	   [T/C]   yes
rs10000006	  T	   C/T	 chr4	108826383	   +	   [A/G]   yes
rs1000002	   C	   A/G	 chr3	183635768	   -	   [A/G]   yes
=cut
	my %comp_snp	= ();
	my %refbase	= ();
	open(FH, $cfile) || die "Cannot open $cfile: $!\n";
	while(<FH>) {
		chomp;

		# skip header lines
		next if(/^#/);

		my @data		= split /\t/;
		$comp_snp{$data[0]}	= 1 if($data[7] eq 'yes');
		$comp_snp{$data[0]}	= 0 if($data[7] eq 'no');

		$refbase{$data[0]}	= $data[1];
	}
	close(FH);

	return(\%comp_snp, \%refbase);
}


__END__

=head1 NAME

snp2vcf.pl - Create VCF from Genome Studio text file for Illumina SNP microarray


=head1 SYNOPSIS

 snp2vcf.pl -i infile -o outfile [options]


=head1 ABSTRACT

This script will take a Genome Studio text file from an Illumina SNP
microarray chip and will create a VCF in the format required by
qsignature.


=head1 OPTIONS

 -i | --infile		Genome Studio text file
 -o | --outfile	   VCF file
 -l | --logfile	   Log file; optional
 -p | --patient	   Patient ID
 -t | --type		  Type (TD, ND, TR etc)
 -b | --library	   Library name
 -v | --verbose	   print progress and diagnostic messages
	  --version	   print version number
 -? | --help		  display help
 -m | --man		   display man page


=head1 DESCRIPTION

I have worked out how to convert Illumina A/B style allele calls from a
GenomeStudio file into the forward-strand base calls used by regular
folk.  You need only 3 things: the reference allele, the A/B call from
Illumina and the SNP definition from Illumina.  The best way to walk
through it is to work some examples.  The hg19+ column in the list below
shows how we would represent the alleles in UCSC on the forward strand.

  Example  SNP	Ref   AB   UCSC+  
  1		[T/C]  G	 BB   GG
  2		[A/G]  A	 BB   GG
  3		[A/C]  C	 BB   CC
  4		[A/G]  T	 AA   TT
  5		[A/G]  G	 AB   AG
  6		[A/G]  C	 AB   TC

Example 1 is a T/C SNP with a reference of G so we must be on
the opposite strand.  BB alleles would make it a CC SNP in
Illumina-speak bu we apply the strand flip to make it GG.

Example 2 is a A/G SNP with a reference of A so no strand flip needed
and therefore Illumina BB (GG) becomes UCSC+ GG.

Example 3 is a A/C SNP with a reference of C so no strand flip.
Illumina BB (CC) becomes UCSC+ CC.

Example 4 is A/G with reference of T so strand flip makes Illumina AA
(AA) become TT. 

Example 5 is A/G with reference of G so no strand flip makes Illumina
AB (AG)  become UCSC+ AG.

Example 6 is A/G with reference of C so strand flip.  Illumina AB (AG)
would become TC.  We do NOT use the small-letter-first convention to
make this CT because we need to maintain the A-allele B-allele
relationship so we interpret the B allele frequency correctly.

The examples show that the only real trick is working out whether or not
to flip the Illumina calls.



=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: snp2vcf.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012,2013

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
