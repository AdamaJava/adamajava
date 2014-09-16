package QCMG::Annotate::QsnpCompoundMutations;

###########################################################################
#
#  Module:   QCMG::Annotate::QsnpCompoundMutations.pm
#  Creator:  Lynn Fink
#  Created:  2013-01-16
#
# Logic for annotating a DCC1 qSNP file to label mutations that occur in a
# single codon
#
#  $Id: QsnpCompoundMutations.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
###########################################################################

use strict;

no warnings 'all';

use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Pod::Find qw(pod_where);
use Clone qw(clone);
use QCMG::IO::GffReader;
use QCMG::IO::DccSnpReader;
use QCMG::IO::DccSnpWriter;
use QCMG::IO::INIFile;
use QCMG::Util::QLog;
use QCMG::Util::Util qw( qexec_header );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 3242 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: QsnpCompoundMutations.pm 4660 2014-07-23 12:18:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    	my $class    = shift;
    	my %defaults = @_;

    	# Print usage message if no arguments supplied
    	#pod2usage(	-input	  => pod_where({-inc => 1, verbose => 1}, __PACKAGE__),
 	#		-exitval  => 0,
        #       		-verbose  => 99,
        #       		-sections => 'DESCRIPTION/Commandline parameters' )
        #unless (scalar @ARGV > 0);

	my $options = {};
        for(my $i=0; $i<=$#_; $i+=2) {
                $options->{lc($_[$i])} = $_[($i + 1)];
        }

    # Setup defaults for CLI params
    my %params = ( inifile  => '',
                   logfile  => '',
		   mode	    => '',
                   ensver   => $defaults{ensver},
                   organism => $defaults{organism},
                   release  => $defaults{release},
                   verbose  => $defaults{verbose}
		);

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'i|inifile=s'        => \$params{inifile},           # -i
	   'm|mode=s'           => \$params{mode},              # -m
           'l|logfile=s'        => \$params{logfile},           # -l
           'e|ensembl=s'        => \$params{ensver},            # -e
           'g|organism=s'       => \$params{organism},          # -g
           'r|release=s'        => \$params{release},           # -r
           'v|verbose+'         => \$params{verbose}           	# -v
           );

    die "-i|--inifile and -m|--mode are required\n"
       unless ($params{inifile} and $params{mode});


	# specify germline OR somatic (default)
	if($params{'mode'} =~ /g/i) {
		$params{'mode'}	= 'germline';
	}
	else {
		$params{'mode'}	= 'somatic';
	}

#	$params{config}		= $options->{'ini'};

	# Load the config file
	my $ini = QCMG::IO::INIFile->new(
					file	=> $params{inifile},
					verbose => $params{verbose}
				);

	# BAMs are needed for filtering indels via pileups
	$params{tumourbam}	= $ini->param('inputfiles', 'tumourbam');
	$params{normalbam}	= $ini->param('inputFiles', 'normalBam');
	$params{genome}		= $ini->param('inputFiles', 'ref');
	$params{dcc1file}	= $ini->param('outputFiles','dcc'.$params{'mode'});
	$params{dcc1out}	= $params{dcc1file}.".compoundsnps";
	$params{logfile}	= $params{dcc1file}.".compoundsnps.log";

	unless($params{genome}	=~ /GRCh37_ICGC_standard_v2/) {
		die "QsnpCompoundMutations not implemented for organisms other than human\n";
	}
	# default to human gene model
	$params{genemodel}	= "/panfs/share/gene_models/GRCh37/Ensembl/Homo_sapiens.GRCh37.".$params{'ensver'}.".fixed.gtf";
	
	if($params{verbose}) {
		qlogprint( "Parsed contents of INI file ",$params{inifile},":\n" );
		qlogprint( "  $_\n" ) foreach split( /\n/, $ini->to_text );
	}

	#print Dumper %params;

	die "infile is required\n"	unless ($params{dcc1file});
	die "outfile is required\n"	unless ($params{dcc1out});

	# Set up logging
	qlogfile($params{logfile}) if $params{logfile};
	qlogbegin();
	#qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n" );
	qlogprint( "Run parameters:\n" );
	foreach my $key (sort keys %params) {
		qlogprint( "  $key : ", $params{$key}, "\n" );
	}

	my $self	= { %params };
	bless $self, $class;
}

#######################################
# READ GENE MODEL
#
# BAD - fix hardcoded gene model file
# read all exons from the appropriate gene model GTF into a hash ref; 
# key = transcript ID; values = attributes
sub read_gene_model {
	my $self	= shift @_;

	qlogprint("Reading ".$self->{'genemodel'}."\n");

	my $features	= QCMG::IO::GffReader->new(filename => $self->{'genemodel'});
	my $count	= 0;
	my $exon;
	my $exonbychr;
	my %uniqueexons	= ();
	while (my $rec = $features->next_record) {

		#next unless($rec->{'feature'} eq 'exon');

		my $chr		= $rec->{'seqname'};
		my $start	= $rec->{'start'};
		my $end		= $rec->{'end'};
		my $strand	= $rec->{'strand'};
		my $txid	= $rec->{'attribs'}->{'transcript_id'};
		$txid		=~ s/\"//g;
		my $exonnum	= $rec->{'attribs'}->{'exon_number'};
	
		$chr		=~ s/chr//;	# NEED TO REPLACE WITH PROPER CONVERSION!
	
		$exon->{$txid}->{$exonnum}->{'seqname'}	= $chr;
		$exon->{$txid}->{$exonnum}->{'start'}	= $start;
		$exon->{$txid}->{$exonnum}->{'end'}	= $end;
		$exon->{$txid}->{$exonnum}->{'strand'}	= $strand;
	
		# identify all features uniquely with the transcript ID, exon
		# number, chromosome, start and end positions, and strand
		#push @{$exonbychr->{$chr}}, join ":", $txid, $exonnum, $chr, $start, $end, $strand;
		my $exonstring	= join ":", $chr, $start, $end, $strand;

		# skip this exon if we have another one with the same coordinates
		if($uniqueexons{$exonstring} == 1) {
			print STDERR "Skipping this exon, seen it before: $exonstring\n";
			next;
		}

		push @{$exonbychr->{$chr}}, join ":", $txid, $exonnum, $exonstring;

		$uniqueexons{$exonstring}	= 1;
		
		$count++;
	}

	qlogprint("Read $count unique exons\n");
	print STDERR "Read $count unique exons\n";

	$self->{'exonbychr'}	= clone($exonbychr);

	return($exonbychr);
}
#######################################

sub execute {
	my $self	= shift @_;

	qlogprint("QsnpCompoundMutations: reading gene model\n");

	### read gene model and get all features
	$self->read_gene_model();

	### read DCC1 file
	my $infh	= QCMG::IO::DccSnpReader->new(filename => $self->{'dcc1file'}, verbose => $self->{'verbose'});
	my $count	= 0;
	# get entire DCC1 header
	my $meta	= $infh->qcmgmeta_string();
	# get all records and put them into an array for sequential processing
	my @dcc		= ();
	while (my $rec = $infh->next_record) {
		push @dcc, $rec;
	}

	# create output file in same DCC1 format
	my $dcc1out	= QCMG::IO::DccSnpWriter->new(
	                   	filename	=> $self->{dcc1out},
				version		=> $infh->version,
				meta     	=> qexec_header().$meta,
				annotations 	=> { 
							Organism	=> $self->{organism}, 
							QAnnotateMode	=> "qsnpcompoundmutations"
						   },
	                   	verbose		=> $self->{verbose}
				);
	
	
	## ASSUME THAT ALL SNPS ARE 1 BASE IN LENGTH
	## ASSUME THAT A GENOMIC COORDINATE IS ONLY REPORTED ONCE IN QSNP DCC1 FILE
	## DO NOT ASSUME THAT TRANSCRIPT EXONS ALL HAVE THE SAME FRAME

	# keep list of mutation IDs that are in compound mutations
	my %compound_mutations		= ();
	# keep track of which bases in a codon have been mutated
	my %checked_codons		= ();

	#@days[3,4,5]
	# start with first DCC1 record so we can get them in groups
	my $i				= 0;

	while($i <= $#dcc) {
		if($i % 100000 == 0 && $i != 0) {
			#print STDERR "$i DCC records parsed for compound mutations\n";
			qlogprint("$i DCC records parsed for compound mutations\n");
		}


		# get three records (max that can be in a single codon)
		my @recs	= @dcc[$i,$i+1, $i+2];

		my $ichr		= $recs[0]->{'chromosome'};
		my $jchr		= $recs[1]->{'chromosome'};
		my $kchr		= $recs[2]->{'chromosome'};

		# if record 1 and record 2 are not on the same chromosome, skip
		# the first records and get the next 3 
		unless($ichr eq $jchr) {
			#print STDERR "$ichr ne $jchr\n";
			$i++;
			next;
		}

		# check that these are single-base SNPs; if not, exit
		my $istart		= $recs[0]->{'chromosome_start'};
		my $iend		= $recs[0]->{'chromosome_end'};
		unless($istart == $iend) {
			qlogprint("Found SNP with >1 bases, exiting\n");
			exit(3);
		}
		my $jstart		= $recs[1]->{'chromosome_start'};
		my $jend		= $recs[1]->{'chromosome_end'};
		unless($jstart == $jend) {
			qlogprint("Found SNP with >1 bases, exiting\n");
			exit(3);
		}
		my $kstart		= $recs[2]->{'chromosome_start'};
		my $kend		= $recs[2]->{'chromosome_end'};
		unless($kstart == $kend) {
			qlogprint("Found SNP with >1 bases, exiting\n");
			exit(3);
		}

		# check that records are within 1-2 bases of each other
		unless($jstart - $istart < 3) {
			#print STDERR "$jstart - $istart not < 3\n";
			$i++;
			next;
		}

		# check if the strand for the first two records are the same; 
		# set strand to records with known strand if one strand is
		# unknown; set strand to . if both are unknown and x if they 
		# are both known and are both not the same
		my $ij_common_strand		= $self->get_strand($recs[0]->{'refsnp_strand'}, $recs[1]->{'refsnp_strand'});
		# skip to next pair if these are explicitly on different strands (-1/1)
		unless($ij_common_strand ne 'x') {
			#print STDERR "Diff strands: $ij_common_strand\n";
			#print STDERR $recs[0]->{'refsnp_strand'}." ".$recs[1]->{'refsnp_strand'}."\n";
			$i++;
			next;
		}

		# check that flags are favourable
		my $iflag		= $recs[0]->{'QCMGflag'};
		my $jflag		= $recs[1]->{'QCMGflag'};
		my $badflag		= $self->check_flags($iflag, $jflag);

		if($badflag eq 'y') {
			#print STDERR "BAD FLAGS(i,j): $iflag $jflag\n";
			$i++;
			next;
		}


		my ($imutation, $imutationid);
		if($self->{'mode'} eq 'somatic') {
			$imutation		= $dcc[$i]->{'mutation'};
			$imutationid		= $dcc[$i]->{'mutation_id'};
		}
		else {
			$imutation		= $dcc[$i]->{'Mutation'};
			$imutationid		= $dcc[$i]->{'variant_id'};
		}
		my ($jmutation, $jmutationid);
		if($self->{'mode'} eq 'somatic') {
			$jmutation		= $dcc[$i+1]->{'mutation'};
			$jmutationid		= $dcc[$i+1]->{'mutation_id'};
		}
		else {
			$jmutation		= $dcc[$i+1]->{'Mutation'};
			$jmutationid		= $dcc[$i+1]->{'variant_id'};
		}

		# check M/mutation status - if 'null' (from GATK), also skip
		if($imutation eq 'null' || $jmutation eq 'null') {
			$i++;
			next;
		}




		## NOW we have a pair of SNPs to investigate!

		# if we get here, then we have found a SNP right next to the current one
		print STDERR "Found valid nearby pair: $ichr:$istart-$iend:$ij_common_strand\n" if($self->{'verbose'});
		print STDERR "                         $jchr:$jstart-$jend:$ij_common_strand\n" if($self->{'verbose'});
	
		# find the affected transcripts and check their codons to see where the
		# SNP falls
		my $txid	= $self->transcript_affected($ichr, $istart, $iend, $ij_common_strand);
		#print Dumper $txid;

		unless($txid->[0] =~ /\w/) {
			print STDERR "  No affected transcripts overlap these SNPs\n";
			$i++;
			next;
		}




		## now check for a third overlapping SNP in potential codons
		my $use_third_snp	= 'n';
		# check distance
		my $snp_dist		= $kstart - $istart;	
		# check strand
		my $ijk_common_strand	= $self->get_strand($ij_common_strand, $recs[2]->{'refsnp_strand'});
		# check flag
		$badflag		= $self->check_flags($recs[2]->{'QCMGflag'});
		if($ichr eq $kchr && $snp_dist < 3 && $ijk_common_strand ne 'x' && $badflag ne 'y') {
			# POSSIBLE THIRD SNP
			print STDERR "Using third SNP  $jchr:$jstart-$jend:$ij_common_strand\n" if($self->{'verbose'});
			$use_third_snp	= 'y';
		}
	
		# WARNING - there is a possible issue with same-codons SNPs in
		# an exone and a frame-shifted version of that exon causing the
		# oringal SNP record (then flagged as COMPOUNDMUTATION to appear
		# twice in the DCC1 output); this is a hard one to test unless a
		# real situation is found

		my %affected_codon	= ();

		foreach my $t (@{$txid}) {
			print STDERR "\tChecking transcript: $t\n" if($self->{'verbose'});
	
			# this only returns the codon that overlaps the SNP (if there is
			# one); this is a reference to an array of the codon start and end
			# coordinates
			my $num_snps;
			my $pattern;

			my @snp_starts	= ();
			push @snp_starts, $istart;
			push @snp_starts, $jstart;
			push @snp_starts, $kstart if($use_third_snp eq 'y');

			($num_snps, $pattern)	= $self->check_codon($t, \@snp_starts);
			if($num_snps	== 0) {
				print STDERR "\t\tSNPs are not in same codon\n";
				next;
			}

			# keep affected 
			#print STDERR "$num_snps\n";
			#print Dumper $pattern;
			#print Dumper @snp_starts;
			my $snp_pos	= join ",", @snp_starts;
			$affected_codon{$snp_pos}	= $pattern;
		}

		#print Dumper %affected_codon;

		# if no compound SNPs were found with this set of 3 SNP, increment i by 1 and try next set
		if(scalar keys %affected_codon < 1) {
			$i += 1;
			next;
		}



		# but first, report new DCC1 records and edit old ones
		foreach (keys %affected_codon) {
			# create DCC1 record for each possible affected codon (there should be only one
			# UNLESS there is a frame-shifted exon)

			# edit "old" records; just add QCMGflag
			# 0-1-2,  -0-1, 0- -1
			my $pattern		= $affected_codon{$_};	 # ref to array
			my $patternstring	= join "-", @{$pattern}; # string representing pattern
			my @indexes		= ();			 # indexes used in pattern
			foreach (@{$pattern}) {
				push @indexes, $_ if(/\d/);
			}
			my @editedrecs	= ();
			foreach (@indexes) {
				$editedrecs[$_]			= clone($recs[$_]);
				# edit actual record, not copy of it (unless it has already been set)
				$editedrecs[$_]->{'QCMGflag'}	.= ";COMPOUNDMUTATION" unless($recs[$_]->{'QCMGflag'} =~ /COMPOUND/);
			}


			# write new DCC record; model it on first SNP examined in this set
			my $newrec				= clone($recs[0]);
			$newrec->{'QCMGflag'}			= "PASS";


			# define new start and end coordinates
			$newrec->{'chromosome_start'}		= $recs[ $indexes[0] ]->{'chromosome_start'};
			$newrec->{'chromosome_end'}		= $recs[ $#indexes   ]->{'chromosome_end'};

			# compound the reference alleles and mutant alleles to cover the whole range
			my @new_ref_snp_allele	= ();		# if compound mutation, get all new refsnp_alleles
			my @new_mutation	= ();		# if compound mutation, get all new mutations
			my @new_tumour_gen	= ();		# if compound mutation, get all tumour_genotypes
			my @new_control_gen	= ();		# if compound mutation, get all normal_genotypes
			if($patternstring =~ /0\- \-1/) {
				# get ref base between SNPs
				my $pos		= $recs[0]->{'chromosome_start'} + 1;
				my $revcomp	= 'n';	# default (+/. strand)
				$revcomp	= 'y' if($ij_common_strand ne '+');
				my $refbase	= QCMG::Util::Util::fetch_subsequence(
									GENOME => $self->{'genome'}, 
									COORD	=> "chr".$ichr.":".$pos."-".$pos,
									REVCOMP	=> $revcomp
								);

				my $refbase_snp_allele		= join "/", $refbase, $refbase;
				my $refbase_mutation		= join ">", $refbase, $refbase;


				$newrec->{'reference_genome_allele'}		= join "", 	$recs[0]->{'reference_genome_allele'},
										$refbase,
										$recs[1]->{'reference_genome_allele'};

				push @new_ref_snp_allele, 	  		$recs[0]->{'refsnp_allele'},
										$refbase_snp_allele,
										$recs[1]->{'refsnp_allele'};

				if($self->{'mode'} eq 'somatic') {
					push @new_mutation, 	  	  	$recs[0]->{'mutation'},              
										$refbase_mutation,  
										$recs[1]->{'mutation'};
				}
				else {
					push @new_mutation, 	  	  	$recs[0]->{'Mutation'},               
										$refbase_mutation,  
										$recs[1]->{'Mutation'};
				}

				push @new_tumour_gen,		  		$recs[0]->{'tumour_genotype'},       
										$refbase_snp_allele, 
										$recs[1]->{'tumour_genotype'};

				push @new_control_gen, 	  	  		$recs[0]->{'control_genotype'}, 	
										$refbase_snp_allele, 
										$recs[1]->{'control_genotype'};

			}
			else {
				$newrec->{'reference_genome_allele'}		= '';
				foreach (@indexes) {
					$newrec->{'reference_genome_allele'}	.= $recs[$_]->{'reference_genome_allele'};

					push @new_ref_snp_allele, 	  	$recs[$_]->{'refsnp_allele'};

					if($self->{'mode'} eq 'somatic') {
						push @new_mutation, 	    	$recs[$_]->{'mutation'};              
					}
					else {
						push @new_mutation, 	    	$recs[$_]->{'Mutation'};               
					}

					push @new_tumour_gen,		  	$recs[$_]->{'tumour_genotype'};       

					push @new_control_gen, 	  	  	$recs[$_]->{'control_genotype'}; 	
				}
			}
			$newrec->{'refsnp_allele'}		= $self->format_compound_mutation("/", @new_ref_snp_allele);
			$newrec->{'tumour_genotype'}		= $self->format_compound_mutation("/", @new_tumour_gen);
			$newrec->{'control_genotype'}		= $self->format_compound_mutation("/", @new_control_gen);

			if($self->{'mode'} eq 'somatic') {
				$newrec->{'mutation'}		= $self->format_compound_mutation(">", @new_mutation);
				$newrec->{'mutation_id'}	= $recs[0]->{'mutation_id'}.".".$i;
				$newrec->{'mutation_type'}	= 4;
			}
			else {
				$newrec->{'Mutation'}		= $self->format_compound_mutation(">", @new_mutation);
				$newrec->{'variant_id'}		= $recs[0]->{'variant_id'}.".".$i;
				$newrec->{'variant_type'}	= 4;
			}

			# perform pileups
			my $mut_id		= "";
			if($self->{'mode'} eq 'somatic') {
				$mut_id	= $recs[0]->{'mutation_id'};
			}
			else {
				$mut_id	= $recs[0]->{'variant_id'};
			}
			my $snpfile	= "/scratch/$mut_id.dcc1";
			# create output file in same DCC1 format for this
			# individual SNP
			my $dccsnpout	= QCMG::IO::DccSnpWriter->new(
		                  	filename	=> $snpfile,
					version		=> $infh->version,
		                  	verbose		=> $self->{verbose}
				);
			$dccsnpout->write_record($newrec);
			$dccsnpout->close;
			my ($nd, $td)	= $self->run_qbasepileup($snpfile);
			$newrec->{'ND'}				= $nd;
			$newrec->{'TD'}				= $td;


			# add new records to array after relevant SNPs, replacing unedited records
			push @editedrecs, $newrec;
			splice @dcc, $i, $#indexes+1, @editedrecs;
			$i 	= $#indexes + $i + 1;	# account for new record; $# is automatically adjusted
		}
		###

		# if compound SNP(s) found, increment i by 2 and try that set
		$i += 2;
	}

	# write all records to new file
	my $count	= 0;
	foreach my $rec (@dcc) {
		#print STDERR $rec->{'mutation_id'}."\n" if($self->{'verbose'});
		$dcc1out->write_record($rec);
		$count++;
	}
	$dcc1out->close;

	qlogprint("\nWrote $count edited records\n");

	# copy original DCC1 file so it is not overwritten
	my $cmd		= qq{cp }.$self->{'dcc1file'}.qq{ }.$self->{'dcc1file'}.".noncompoundsnps";
	qlogprint("$cmd\n");
	my $rv		= system($cmd);
	unless($rv == 0) {
		qlogprint("FAILED ($rv)\n");
		exit(11);
	}

	$cmd		= qq{cp }.$self->{'dcc1out'}.qq{ }.$self->{'dcc1file'};
	qlogprint("$cmd\n");
	$rv		= system($cmd);
	unless($rv == 0) {
		qlogprint("FAILED ($rv)\n");
		exit(11);
	}

	return();
}


###############################################################################
=cut
##qbasepileup version 1.0																
0	1	2	3	4		5		6		7	8		9		10		11			12			13		14		15			16	
ID	Donor	Bam	SnpId	Chromosome	Start		End		RefBase	ExpectedAltBase	TotalPlus	TotalRefPlus	TotalExpectedAltPlus	TotalOtherAltPlus	TotalMinus	TotalRefMinus	TotalExpectedAltMinus	TotalOtherAltPlus
TD.bam	41519	2		89102920	89102921	TC	AT		39		28		4			7			39		32		5			2

ALL:T+T-;REF:TR+;TR-;ALT:TEAP+TOAP+;TEAM-TOAM-

ALL:30+,39-;TC:28+;32-;AT:4+7+;5-2-
=cut
sub run_qbasepileup {
	my $self	= shift @_;
	my $snpfile	= shift @_;

	my ($nd, $td);

	# PILEUP ON NORMAL BAM
	my $pileuplog	= $snpfile.".qbasepileup_ND.log";
	my $pileupout	= $snpfile.".qbasepileup_ND.txt";
	my $cmd		= qq{qbasepileup -i }.$self->{'normalbam'}.qq{ -s $snpfile -f dcc1 -m compoundsnp --loglevel OFF --log $pileuplog -o $pileupout -r }.$self->{'genome'};
	qlogprint("Running qbasepileup on normal BAM: $cmd\n");
	my $rv		= system($cmd);
	unless($rv == 0) {
		qlogprint("FAILED ($rv): $cmd\n");
		exit(10);
	}
	# parse qbasepileup output, edit ND field
	open(FH, $pileupout) || die "Cannot open $pileupout: $!\n";
	while(<FH>) {
		chomp;
		next if(/^#/);
		next if(/^ID/);
		my @data	= split /\t/;
		$nd		=  'ALL:'.$data[9] .'+'.$data[13].'-;';
		$nd		.= 'REF:'.$data[10].'+'.$data[14].'-;';
		$nd		.= 'ALT:'.$data[11].'+'.$data[12].'+'.$data[15].'-'.$data[16].'-;';
	}
	close(FH);
	# remove tmp files
	unlink($pileuplog);
	unlink($pileupout);

	# PILEUP ON TUMOUR BAM
	$pileuplog	= $snpfile.".qbasepileup_TD.log";
	$pileupout	= $snpfile.".qbasepileup_TD.txt";
	$cmd		= qq{qbasepileup -i }.$self->{'tumourbam'}.qq{ -s $snpfile -f dcc1 -m compoundsnp --loglevel OFF --log $pileuplog -o $pileupout -r }.$self->{'genome'};
	qlogprint("Running qbasepileup on tumour BAM: $cmd\n");
	$rv		= system($cmd);
	unless($rv == 0) {
		qlogprint("FAILED ($rv): $cmd\n");
		exit(10);
	}
	# parse qbasepileup output, edit TD field
	open(FH, $pileupout) || die "Cannot open $pileupout: $!\n";
	while(<FH>) {
		chomp;
		next if(/^#/);
		next if(/^ID/);
		my @data	= split /\t/;
		$td		=  'ALL:'.$data[9] .'+'.$data[13].'-;';
		$td		.= 'REF:'.$data[10].'+'.$data[14].'-;';
		$td		.= 'ALT:'.$data[11].'+'.$data[12].'+'.$data[15].'-'.$data[16].'-;';
	}
	close(FH);

	# remove tmp files
	unlink($pileuplog);
	unlink($pileupout);
	unlink($snpfile);

	return($nd, $td);
}

###############################################################################
# check that there are no bad flags assigned to records of interest
sub check_flags {
	my $self	= shift @_;
	my $badflag	= 'n';

	foreach my $flag (@_) {
		if(	$flag =~ /COV/   || 
			$flag =~ /GERM/  || 
			$flag =~ /SBIAS/ || 
			$flag =~ /NNS/   || 
			#$flag =~ /MER/   || 
			#$flag =~ /MR/    || 
			$flag =~ /SAN3/  || 
			$flag =~ /SAT3/  || 
			#$flag =~ /5BP/   || 
			$flag =~ /COMPOUND/) {

			$badflag	= 'y';
			return($badflag);
		}
	}

	return($badflag);
}

###############################################################################
# translate DCC strand into standard characters
sub get_strand {
	my $self	= shift @_;
	my $strand1	= shift @_;
	my $strand2	= shift @_;

	# normalize vocabulary
	if($strand1 eq '+') {
		$strand1	= 1;
	}
	elsif($strand1 eq '-') {
		$strand1	= -1;
	}
	else {
		$strand1	= '-888';
	}
	if($strand2 eq '+') {
		$strand2	= 1;
	}
	elsif($strand2 eq '-') {
		$strand2	= -1;
	}
	else {
		$strand2	= '-888';
	}

	my $strand;

	# both on + strand
	if($strand1 == 1 && $strand2 == 1) {
		$strand	= '+';
	}
	# both on - strand
	elsif($strand1 == -1 && $strand2 == -1) {
		$strand	= '-';
	}
	# first on - strand, second unknown
	elsif($strand1 == -1 && $strand2 == -888) {
		$strand	= '-';
	}
	# first on + strand, second unknown
	elsif($strand1 == 1 && $strand2 == -888) {
		$strand	= '+';
	}
	# first unknown, second on + strand
	elsif($strand1 == -888 && $strand2 == 1) {
		$strand	= '+';
	}
	# first unknown, second on - strand
	elsif($strand1 == -888 && $strand2 == -1) {
		$strand	= '-';
	}
	# both unknown
	elsif($strand1 == -888 && $strand2 == -888) {
		$strand	= '.';
	}
	# both on different strands
	elsif($strand1 != $strand2) {
		$strand	= 'x';
	}

	return($strand);
}

###############################################################################
# combine mutation annotations into a compound mutation
# C/A C/G -> CC/AG
# C>A C>G -> CC>AG
# mutation_snp  ???
# THIS IS ONLY MEANT TO RUN ON SINGLE BASE MUTATIONS WITH A MAX OF 3 IN
# SUCCESSION
sub format_compound_mutation {
	my $self	= shift @_;

	my $delimiter	= shift @_;

	my $mut1	= shift @_;
	my $mut2	= shift @_;
	my $mut3	= shift @_;

	$mut1		=~ /(\w+)(\W)(\w+)/;
	my $ref1	= $1;
	#my $delimiter1	= $2;
	my $mutant1	= $3;
	$mut2		=~ /(\w+)(\W)(\w+)/;
	my $ref2	= $1;
	#my $delimiter2	= $2;
	my $mutant2	= $3;

	my($ref3, $delimiter3, $mutant3);
	if($mut3) {
		$mut3		=~ /(\w+)(\W)(\w+)/;
		$ref3		= $1;
		#$delimiter3	= $2;
		$mutant3	= $3;
	}

	#if($delimiter1 ne $delimiter2) {
        #	die print STDERR "$mut1 and $mut2 are illegal - should have matching mutation delimiters\n";
	#}
	#if($delimiter3 && $delimiter1 ne $delimiter3) {
        #	die print STDERR "$mut1 and $mut3 are illegal - should have matching mutation delimiters\n";
	#}

	my $output	= $ref1.$ref2;
	$output		.= $ref3 if($ref3);
	#$output		.= $delimiter1;
	$output		.= $delimiter;
	#$output		.= ">";
	$output		.= $mutant1.$mutant2;
	$output		.= $mutant3 if($mutant3);

	$output 	= '--' if($output eq '>');

	return($output);
}

sub transcript_affected {
	my $self	= shift @_;

	my $chr		= shift @_;
	my $start	= shift @_;
	my $end		= shift @_;
	my $strand	= shift @_;	# +,-,.

	my $exons	= $self->{'exonbychr'}->{$chr};

	my @txtaffected;
	foreach (@{$exons}) {
		my ($txid, $exonnum, $chr, $estart, $eend, $estrand)	= split /\:/, $_;

		if($strand ne $estrand && $strand ne '.') {
			#print STDERR "Skipping exon because of strand mismatch : $strand <=> $estrand\n";
			next;
		}

		if($start >= $estart && $end <= $eend) {
			push @txtaffected, $_;
			#print STDERR "Keeping $_\n";
		}

		#last if($start > $eend);	# assumes GTF is sorted and it may not be
	}

	return(\@txtaffected);
}

sub check_codon {
	my $self	= shift @_;

	my $tx		= shift @_;
	my $starts	= shift @_;

	my $pos1	= shift @{$starts}; # coord of SNP1
	my $pos2	= shift @{$starts};	# coord of SNP2
	my $pos3	= shift @{$starts};	# coord of SNP3 (optional)

	print STDERR "\t\tChecking codon for $pos1, $pos2, $pos3\n";

	my ($txid, $exonnum, $chr, $start, $end, $strand)	= split /\:/, $tx;
	#print STDERR "\t\t$txid, $exonnum, $chr, $start, $end, $strand\n";

	# --- ---
	# ijk		same, 3 SNPs
	# i j		same, 2 SNPs (not adjacent)
	#  ij k		same, 2 SNPs

	my $codon_coords	= '';
	my $num_snps		= 0;
	my %used_codons		= ();
	for (my $i=$start;$i<=$end;$i+=3) {
		my $j	= $i+2;

		my $tag		= join "-", $i, $j;

		last if($used_codons{$tag} == 1);

		#print STDERR "\t\tCODON: $i - $j\n";
		if($pos1 < $j && $pos1 >= $i && $pos2 <= $j && $pos2 > $i) {
			print STDERR "\t\t\tCODON: $i - $j\n";
			print STDERR "\t\t\t\tSNP at $pos1 in this codon\n";
			print STDERR "\t\t\t\tSNP at $pos2 in this codon\n";

			$num_snps	= 2;
			$codon_coords	= "$i-$j";

			if($pos3 > 0) {
				if($pos3 <= $j && $pos3 > $i) {
					print STDERR "\t\t\tSNP at $pos3 in this codon\n";
					$num_snps	= 3;
				}
			}

			$used_codons{$tag}	= 1;

			my @pattern 	= (' ', ' ', ' ');
			if($pos1 == $i) {
				$pattern[0]	= 0;
			}
			elsif($pos1 == $i+1) {
				$pattern[1]	= 0;
			}
			if($pos2 == $i+1) {
				$pattern[1]	= 1;
			}
			elsif($pos2 == $i+2) {
				$pattern[2]	= 1;
			}
			if($pos3 == $i+2) {
				$pattern[2]	= 2;
			}
			my $snp_pattern	= join "-", @pattern;
			print STDERR "\t\t\tPATTERN: $snp_pattern\n";

			return($num_snps, \@pattern);
		}
	}

	return($num_snps);
}

=cut
sub check_codon {
	my $self	= shift @_;

	my $tx		= shift @_;
	my $pos1	= shift @_;
	my $pos2	= shift @_;

	my ($txid, $exonnum, $chr, $start, $end, $strand)	= split /\:/, $tx;
	#print STDERR "$txid, $exonnum, $chr, $start, $end, $strand\n";
	#print STDERR "check_codon(): Checking SNPs at $pos1 and $pos2\n";

	# adjust coordinate system
	#$pos		+= 1;	# do not adjust SNP; already in correct coordinate system
	#$start		+= 1;	# do not adjust start pos
	$end		+= 1;

	my $revcomp	= 'n';	# default (+ strand)
	$revcomp	= 'y' if($strand eq '-');

	my $subseq	= QCMG::Util::Util::fetch_subsequence(	GENOME	=> "/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa", 
								COORD	=> $chr.":".$start."-".$end,
								REVCOMP	=> $revcomp
							);

	#print STDERR "SUBSEQ: $subseq\n";
	my @bases	= ($subseq =~ /([ACGTN]{1})/g);
	my @coords	= $start..$end;

	if($strand eq '-') {
		@coords	= reverse(@coords);
	}

	my @codon_coords	= ();
	for (my $i=0;$i<=$#coords;$i+=3) {
		#print STDERR "$coords[$i]\n";
		#print STDERR $coords[$i+1]."\n";
		#print STDERR $coords[$i+2]." "."\n\n";

		# - strand:
		# |---|---|
		#  321
		#   **
		if(
			($strand eq '-' && $coords[$i] >= $pos1 && $coords[$i+2] <= $pos1 && $coords[$i] >= $pos2 && $coords[$i+2] <= $pos2) ||
			($strand eq '+' && $coords[$i] <= $pos1 && $coords[$i+2] >= $pos1 && $coords[$i] <= $pos2 && $coords[$i+2] >= $pos2)
		) {
			print STDERR "\t\tSame codon ($pos1 $pos2)\n" if($self->{'verbose'});
			#$is_same_codon	= 'y';
			push @codon_coords, $coords[$i]; 
			push @codon_coords, $coords[$i+2];
			last;
		}
		#else {
			#print STDERR "Not on same codon ($pos1 $pos2)\n";
		#}
	}

	# only return codon if it overlaps SNP
	# 
	return(\@codon_coords);
}
=cut

=cut
sub get_codon {
	my $chr		= shift @_;
	my $start	= shift @_;
	my $end		= shift @_;
	my $strand	= shift @_;

	# adjust coordinate system
	#$pos		+= 1;	# do not adjust SNP; already in correct coordinate system
	#$start		+= 1;	# do not adjust start pos

	#??$end		+= 1;

	# also, need to complement the $tdseqa1 because the DCC/MAF will have
	# the + strand base

	# + strand only; must revcomp to get codon from - strand
	my %DNA_code = (
	'GCT' => 'A', 'GCC' => 'A', 'GCA' => 'A', 'GCG' => 'A', 
	'TGT' => 'C', 'TGC' => 'C', 
	'GAT' => 'D', 'GAC' => 'D', 
	'GAA' => 'E', 'GAG' => 'E', 
	'TTT' => 'F', 'TTC' => 'F',
	'GGT' => 'G', 'GGC' => 'G', 'GGA' => 'G', 'GGG' => 'G', 
	'CAT' => 'H', 'CAC' => 'H', 
	'ATT' => 'I', 'ATC' => 'I', 'ATA' => 'I', 
	'AAA' => 'K', 'AAG' => 'K', 
	'TTA' => 'L', 'TTG' => 'L', 'CTT' => 'L', 'CTC' => 'L', 'CTA' => 'L', 'CTG' => 'L',
	'ATG' => 'M', 
	'AAT' => 'N', 'AAC' => 'N',
	'CCT' => 'P', 'CCC' => 'P', 'CCA' => 'P', 'CCG' => 'P', 
	'CAA' => 'Q', 'CAG' => 'Q', 'TCT' => 'S', 'TCC' => 'S',
	'CGT' => 'R', 'CGC' => 'R', 'CGA' => 'R', 'CGG' => 'R', 'AGA' => 'R', 'AGG' => 'R', 
	'TCA' => 'S', 'TCG' => 'S', 'AGT' => 'S', 'AGC' => 'S', 
	'ACT' => 'T', 'ACC' => 'T', 'ACA' => 'T', 'ACG' => 'T',
	'GTT' => 'V', 'GTC' => 'V', 'GTA' => 'V', 'GTG' => 'V',
	'TGG' => 'W',
	'TAT' => 'Y', 'TAC' => 'Y' ,
	'TAG' => 'STOP', 'TAA' => 'STOP', 'TGA' => 'STOP'
	);

	my $revcomp	= 'n';	# default (+ strand)
	$revcomp	= 'y' if($strand eq '-');

	my $subseq	= QCMG::Util::Util::fetch_subsequence(	GENOME	=> "/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa", 
								COORD	=> $chr.":".$start."-".$end,
								REVCOMP	=> $revcomp
							);

	#print "SUBSEQ: $subseq\n";
	my @bases	= ($subseq =~ /([ACGTN]{1})/g);
	my @coords	= $start..$end;

	if($strand eq '-') {
		@coords	= reverse(@coords);

		# complement mutant allele
		foreach (keys %{$pos}) {
			$pos->{$_}	=~ tr/ACGT/TGCA/;
		}
	}

	my %revisedbases	= ();
	for my $i (0..$#bases) {
		# get reference bases
		$revisedbases{$coords[$i]}	= $bases[$i];

		# alter reference with any mutant bases
		if($pos->{$coords[$i]}) {
			$revisedbases{$coords[$i]}	= $pos->{$coords[$i]};
		}
	}

	for (my $i=0;$i<=$#coords;$i+=3) {
		#print STDERR "$coords[$i] $revisedbases{$coords[$i]}\n";
		#print STDERR $coords[$i+1]." ".$revisedbases{$coords[$i+1]}."\n";
		#print STDERR $coords[$i+2]." ".$revisedbases{$coords[$i+2]}."\n";

		my $origcodon	= join "", @bases[$i..$i+2];
		my $codon	= join "", $revisedbases{$coords[$i]}, $revisedbases{$coords[$i+1]}, $revisedbases{$coords[$i+2]};

		my $origaa	= $DNA_code{$origcodon};
		my $aa		= $DNA_code{$codon};

		print "$coords[$i]: $origcodon -> $origaa, $codon -> $aa";
		if($origaa ne $aa) {
			print " **";
		}
		print "\n";
	}
}
=cut


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
