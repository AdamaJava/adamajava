package QCMG::Annotate::SmallIndelTool;

###########################################################################
#
#  Module:   QCMG::Annotate::SmallIndelTool.pm
#  Creator:  John V Pearson
#  Created:  2012-11-28
#
#  Logic for Ensembl API annotation of variants called by the Bioscope
#  small indel tool.
#
#  $Id: SmallIndelTool.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::Annotate::EnsemblConsequences;
use QCMG::Annotate::Util qw( load_ensembl_API_modules
                             initialise_dcc2_file
                             transcript_to_domain_lookup 
                             transcript_to_geneid_and_symbol_lookups
                             dcc2rec_from_dcc1rec
                             dccqrec_from_dcc1rec 
                             annotation_defaults
                             read_and_filter_dcc1_records
			     mutation_indel );

use QCMG::IO::DccSnpReader;
use QCMG::IO::DccSnpWriter;
use QCMG::IO::EnsemblDomainsReader;
use QCMG::IO::EnsemblTranscriptMapReader;
use QCMG::Util::QLog;
use QCMG::Util::Util qw( qexec_header );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4660 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: SmallIndelTool.pm 4660 2014-07-23 12:18:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class    = shift;
    my %defaults = @_;

    # This routine has to take over the functionality of script:
    # QCMGScripts/k.kassahn/share/indel_SOLiD_v2/INDELannotation_v61.pl

    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'DESCRIPTION/Commandline parameters' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile   => '',
                   dcc2out  => '',
                   dccqout  => '',
                   dcc1out  => '',
                   logfile  => '',
                   ensver   => $defaults{ensver},
                   organism => $defaults{organism},
                   release  => $defaults{release},
                   verbose  => $defaults{verbose},
                   domains  => '',
                   symbols  => '',
                   type     => '' );

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'i|infile=s'         => \$params{infile},            # -i
           'o|dcc2out=s'        => \$params{dcc2out},           # -o
           'q|dccqout=s'        => \$params{dccqout},           # -q
             'dcc1out=s'        => \$params{dcc1out},           # -p
           'l|logfile=s'        => \$params{logfile},           # -l
           'e|ensembl=s'        => \$params{ensver},            # -e
           'g|organism=s'       => \$params{organism},          # -g
           'r|release=s'        => \$params{release},           # -r
           'd|domains=s'        => \$params{domains},           # -d
           'y|symbols=s'        => \$params{symbols},           # -y
           't|type=s'           => \$params{type},              # -t
           'v|verbose+'         => \$params{verbose},           # -v
           );

    die "--infile, --dcc2out and --dccqout are required\n"
       unless ($params{infile} and $params{dcc2out} and $params{dccqout});

    die "you must specify --type with a value of somatic or germline\n"
       unless ($params{type} and ($params{type} =~ /somatic/i or
                                  $params{type} =~ /germline/i ));

    # Pick default Ensembl files if not specified by user
    $params{domains} = annotation_defaults( 'ensembl_domains', \%params )
        unless ($params{domains});
    $params{symbols} = annotation_defaults( 'ensembl_symbols', \%params )
        unless ($params{symbols});


    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n" );
    qlogprint( "Run parameters:\n" );
    foreach my $key (sort keys %params) {
        qlogprint( "  $key : ", $params{$key}, "\n" );
    }

    # Get those pesky ensembl API modules into the search path
    load_ensembl_API_modules( $params{ensver} );

    # Create the object
    my $self = { %params };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub execute {
    my $self = shift;

    # We are expecting a very specific type of DCC1 file that is peculiar
    # to the small indel tool pipeline developed by Karin (and Nic) so
    # I'm going to force the filetype here.

    my $dcc1 = QCMG::IO::DccSnpReader->new(
                   filename => $self->{infile},
                   #version  => 'dcc1_smlindeltool',
                   version  => 'dcc1_smallindeltool',
                   verbose  => $self->verbose );
    
    # Read in the SNP positions from the DCC1 and return lists of
    # those to be annotated and those to be skipped

    my ($ra_targets, $ra_skips) = read_and_filter_dcc1_records( $dcc1 );

    # Create corresponding DCC2 file type from DCC1 file type
    my $dcc2_version = ($self->{type} =~ /somatic/i) ?  'dcc2_dbssm_somatic_r11' :
                                                        'dcc2_dbsgm_germline_r11';

    # Construct metadata header
    my $new_meta .= qexec_header() . $dcc1->qcmgmeta_string;

    # Open DCC2 and copy out "skipped" records
    my $dcc2 = initialise_dcc2_file( $self->{dcc2out},
                                     $dcc2_version,
                                     $self->verbose,
                                     $self->{ensver},
                                     $ra_skips,
                                     $new_meta );

    # Create corresponding DCCQ file type from DCC1 file type
    my $dccq_version = $dcc2_version;
    $dccq_version =~ s/dcc2/dccq/;

    # Construct metadata header
    $new_meta = qexec_header() . $dcc1->qcmgmeta_string;

    my $dccq = QCMG::IO::DccSnpWriter->new(
                   filename => $self->{dccqout},
                   version  => $dccq_version,
                   meta     => $new_meta,
                   verbose  => $self->verbose );

    # Load Ensembl lookups for domains, geneids and symbols
    my $rh_transcript_to_domains =
        transcript_to_domain_lookup( $self->{domains},
                                     $self->verbose );
    my ( $rh_transcript_to_geneid,
         $rh_transcript_to_symbol ) =
        transcript_to_geneid_and_symbol_lookups( $self->{symbols},
                                                 $self->verbose );

    # Bio::EnsEMBL::Registry::load_registry_from_db() expects the latin
    # species name so we'll set it here.
    my %species = ();
    if ($self->{organism} =~ /human/i) {
        $species{latin} = 'homo sapiens';
        $species{adaptor} = 'human';
    }
    elsif ($self->{organism} =~ /mouse/i) {
        $species{latin} = 'mus musculus';
        $species{adaptor} = 'mouse';
    }
    else {
        die 'Organism [', $self->{organism}, "] is not valid\n";
    }

    my %reg_params = ( -host       => '10.160.72.25',
                       -user       => 'ensembl-user',
                       -verbose    => $self->verbose,
                       -species    => $species{latin},
                       -db_version => $self->{ensver} );

    qlogprint( "Initialising Bio::EnsEMBL::Registry from database using params:\n" );
    foreach my $key (sort keys %reg_params) {
        qlogprint( "  $key : ", $reg_params{$key}, "\n" );
    }

    # Ensembl API magic:
    # 1. Load a registry via a class method from Bio::EnsEMBL::Registry
    # 2. Get a Bio::EnsEMBL::Variation::DBSQL::VariationFeatureAdaptor
    # 3. Get a Bio::EnsEMBL::DBSQL::SliceAdaptor

    my $reg = 'Bio::EnsEMBL::Registry';
    $reg->load_registry_from_db( %reg_params );
    my $vfa = $reg->get_adaptor($species{adaptor}, 'variation', 'variationfeature');
    my $sa = $reg->get_adaptor($species{adaptor}, 'core', 'slice');

    # And now do the annotation and output
 
    my $chr=''; #dummy chromosome name to trigger slice generation
    my $count = 0;
    my %counts = ();
    my %consequences = ();
    my $slice = '';

    foreach my $rec (@{$ra_targets}) {

        # fetch_by_region() can't seem to cope with chromosome names
        # that include 'chr' so we will strip them out (particularly
        # required for mouse where our reference includes the 'chr').
        my $chrom = $rec->chromosome;
        $chrom =~ s/^chr//;

        $count++;
        $counts{$chrom}++;
        qlogprint( "Annotating record $count\n") if ($count % 1000 == 0);

        # Every chromosome needs a "slice" so watch for a change in chromosome
        if ($chrom ne $chr) {
            #qlogprint( 'Annotated ',$counts{$chr}," SNPs on chromosome $chr\n" ) if $chr;
            $chr = $chrom;
        	# Get a slice for the new feature to be attached to
            qlogprint( "Fetching new EnsEMBL 'slice' for chromosome $chr\n" );
        	$slice = $sa->fetch_by_region('chromosome',$chr);
        }
 
        my $mutation = mutation_indel( $self->{type},
                                       $rec->control_genotype,
                                       $rec->mutation,
                                       $rec->reference_genome_allele,
                                       $rec->mutation_id );

        # Thanks to the different column names in DCC somatic and germline files ...
        my $variation_name = '';
        if ($self->{type} eq 'somatic') {
            $variation_name = $rec->mutation_id;
        }
        elsif ($self->{type} eq 'germline') {
            #$variation_name = $rec->variation_id;
            $variation_name = $rec->mutation_id;
        }

        my %var_feat_params = (
           	-verbose        => $self->verbose,
    		-start          => $rec->chromosome_start,
    		-end            => $rec->chromosome_end,
    		-slice          => $slice,     # the variation must be attached to a slice
    		-allele_string  => $mutation,  # the first allele should be the reference
    		-strand         => 1,
    		-map_weight     => 1,
    		-adaptor        => $vfa,
    		-variation_name => $rec->mutation_id );

        my $new_vf = Bio::EnsEMBL::Variation::VariationFeature->new( %var_feat_params );
           
        # We need to eval this block because it has a tendency to barf under
        # all sorts of ill-defined conditions and we would much rather just
        # note that the SNP in question is "not annotatable" and soldier on.

        my $recq = dccqrec_from_dcc1rec( $rec, $dccq_version );
        $recq->gene_build_version( $self->{ensver} );

 ###
 #
 #  2012-11-27 : conversion from qsnp to smlindeltool has reached here
 #
 ###

        eval {
        	my $cons = $new_vf->get_all_TranscriptVariations();

            # We are going to need a swathe of DDCQ temp arrays
            # that we can append to as we look at the various DCC2
            # records.  At the end we will push the strings into $recq
            # using the appropriate accessors.

            my @recq_consequence_type        = ();
            my @recq_aa_mutation             = ();
            my @recq_cds_mutation            = ();
            my @recq_protein_domain_affected = ();
            my @recq_gene_affected           = ();
            my @recq_transcript_affected     = ();
            my @recq_gene_symbol             = ();
            my @recq_All_domains             = ();
            my @recq_All_domains_type        = ();
            my @recq_All_domains_description = ();

        	while (my $con = shift@$cons) {
    		    my $ra_strings= $con->consequence_type;
            	while (my $string = shift @{$ra_strings}) {

                    $consequences{ $chr }++;

                    # Create new record by cloning DCC1 and flipping version
                    my $rec2 = dcc2rec_from_dcc1rec( $rec, $dcc2_version );
                    $rec2->gene_build_version( $self->{ensver} );
                    $rec2->consequence_type( $string );

		#print Dumper $rec2;
		#exit(0);

        			#get amino acid change in DCC format
    	    		my $aa_ch = $con->pep_allele_string;
                    my $aa_pos = '';
    			    my $aa_change = '';
        			if (defined $aa_ch and $aa_ch=~/\//) {
    	    			$aa_pos = $con->translation_start;
    		    		my @bits = split(/\//,$aa_ch);
    			    	$aa_change=$bits[0].$aa_pos.$bits[1];
          			}
    	    		elsif (defined $aa_ch and $aa_ch=~/[A-Z]/) {
    		    		$aa_pos = $con->translation_start;
    			    	$aa_change=$aa_ch.$aa_pos.$aa_ch;
        			}
    	    		else {
    		    		$aa_pos    = '-888';
    			    	$aa_change = '-888';
    				    $aa_ch     = '-888';
        			}

        			#get cds change in DCC format
        			my $cds_pos = $con->cdna_start;
        			my $cds_change = '';
        			my $transcript = '';
        			if (defined $cds_pos and $cds_pos=~/\d/) {
        				$cds_change=$cds_pos.$mutation;
                        $cds_change =~ s/\//>/;  # change X/Y to X>Y
        				$transcript = $con->transcript->stable_id;
        			}
        			elsif ($string eq 'INTERGENIC') {
        				$cds_change = '-888';
        				$transcript = '-888';
        			}
        			else {
        				$cds_change = '-888';
        				$transcript = $con->transcript->stable_id;
        			}

                    	#$rec2->aa_mutation( $aa_change );	# somatic only
                    	#$rec2->aa_variation( $aa_change );	# germline only
        		if ($self->{type} eq 'somatic') {
				$rec2->aa_mutation( $aa_change );
                    		$rec2->cds_mutation( $cds_change );
        		}
        		elsif ($self->{type} eq 'germline') {
				$rec2->aa_variation( $aa_change );
                    		$rec2->cds_variation( $cds_change );
        		}

                    $rec2->transcript_affected( $transcript );

                    if (exists $rh_transcript_to_geneid->{ $transcript } ) {
                        $rec2->gene_affected( $rh_transcript_to_geneid->{ $transcript } );
                    }

                    # Now we need to crack on with storing away any info
                    # that we need to keep for our DCCQ record.

                    if ($aa_pos ne '-888') {
                        if (exists $rh_transcript_to_domains->{ $transcript } ) {
                            my $ra_domains = $rh_transcript_to_domains->{ $transcript };
                            foreach my $drec (@{ $ra_domains }) {
                                if ( $drec->protein_seq_start <= $aa_pos and
                                     $drec->protein_seq_end >= $aa_pos ) {
                                    # DCC currently only reports PFAM domains
                                    if ($drec->domain =~ /pfam/i) {
                                        push @recq_protein_domain_affected, $drec->protein_hit_name;
                                        $rec2->protein_domain_affected( $drec->protein_hit_name );
                                    }
                                    push @recq_All_domains, $drec->protein_hit_name;
                                    push @recq_All_domains_type, $drec->domain;
                                    push @recq_All_domains_description, $drec->interpro_label;
                                }
                            }
                        }
                    }

                    # Now we have the domain sorted we are ready to write DCC2
                    $dcc2->write_record( $rec2 );

                    ### JVP ###
                    #
                    # Here we need to start copying logic from lines
                    # 128-181 of the annotate_and_reformat_v61.py script.
                    # The simple pushes we have here do not capture the
                    # complexity of Karin's code - we will almost
                    # certainly need Karin to interpret the python to
                    # get this sorted out.

                    push @recq_consequence_type, $string;
                    push @recq_aa_mutation, $aa_change; 
                    push @recq_cds_mutation, $cds_change;
                    push @recq_transcript_affected, $transcript;

                    if (exists $rh_transcript_to_symbol->{ $transcript } ) {
                        push @recq_gene_symbol, $rh_transcript_to_symbol->{ $transcript };
                    }
                    else {
                        push @recq_gene_symbol, '-888';
                    }

                    if (exists $rh_transcript_to_geneid->{ $transcript } ) {
                        push @recq_gene_affected, $rh_transcript_to_geneid->{ $transcript };
                    }
                    else {
                        push @recq_gene_affected, '-888';
                    }

                }
        	}

            ### JVP ###

            # We will certainly have to set $recq values here - all of
            # the values that corresspond to DCC2 info will need to
            # be set once we have finished writing the DCC2 records.

            $recq->consequence_type(
                   join(',',@recq_consequence_type) );

    		if ($self->{type} eq 'somatic') {
            		$recq->aa_mutation(
                   		join(',',@recq_aa_mutation) );
            		$recq->cds_mutation(
                   		join(',',@recq_cds_mutation) );
        	}
        	elsif ($self->{type} eq 'germline') {
            		$recq->aa_variation(
                   		join(',',@recq_aa_mutation) );
            		$recq->cds_variation(
                   		join(',',@recq_cds_mutation) );
        	}

            #$recq->aa_mutation(
            #       join(',',@recq_aa_mutation) );
            #$recq->cds_mutation(
            #       join(',',@recq_cds_mutation) );
            $recq->protein_domain_affected(
                   join(',',@recq_protein_domain_affected) );
            $recq->gene_affected(
                   join(',',@recq_gene_affected) );
            $recq->transcript_affected(
                   join(',',@recq_transcript_affected) );
            $recq->gene_symbol(
                   join(',',@recq_gene_symbol) );
            $recq->All_domains(
                   join(',',@recq_All_domains) );
            $recq->All_domains_type(
                   join(',',@recq_All_domains_type) );
            $recq->All_domains_description(
                   join(',',@recq_All_domains_description) );

        };
        if ($@) {
            # If there was a problem, write out an "empty" DCC2 record
            # and just keep moving!
    	    warn 'Error annotating ', $rec->mutation_id, ": $@\n";
            my $rec2 = dcc2rec_from_dcc1rec( $rec, $dcc2_version );
            $rec2->gene_build_version( $self->{ensver} );
            $dcc2->write_record( $rec2 );
        }

        # Regardless of whether the annotation worked, write out the
        # DCCQ record.  This works OK because we populated it with
        # sensible "missing" values as we initialised it so if we didn't
        # manage to find better values, it's still a valid but
        # uninformative record.

        $dccq->write_record( $recq );
    }

    my @annot_chrs = sort keys %counts;
    qlogprint( "Annotated SNPs on chromosomes:\n" );
    foreach my $key (@annot_chrs) {
        qlogprint( "  $key : ", $counts{$key}, "\n" );
    }
    my @conseq_chrs = sort keys %consequences;
    qlogprint( "Found consequences on chromosomes:\n" );
    foreach my $key (@conseq_chrs) {
        qlogprint( "  $key : ", $consequences{$key}, "\n" );
    }

    ### JVP 2012-11-27 ###
    #
    # At the moment, I have made no attempt to replicate Karin's code in
    # script annotate_and_reformat_v61.py, lines approx 200-370 which
    # create the "consequence summary" file.  Not sure at this stage if
    # this is neede or not.  Guess we'll wait until someone whinges and
    # then make a decision.

    qlogend();
}


1;

__END__

=head1 NAME

QCMG::Annotate::SmallIndelTool - Annotate Bioscope small indel tool output files


=head1 SYNOPSIS

 use QCMG::Annotate::SmallIndelTool;

 my %defaults = ( ensver   => '70',
                  organism => 'human',
                  release  => '11',
                  verbose  => 0 );
 my $qsnp = QCMG::Annotate::SmallIndelTool->new( %defaults );
 $qsnp->execute;


=head1 DESCRIPTION

This module provides functionality for annotating Bioscope small
indeltool DCC1 files.  It includes commandline parameter processing.

This module is based on a series of SNP annotation scripts originally
created by Karin Kassahn and JP and elaborated by Karin to adapt to new
organisms (Mouse etc) and new versions for the EnsEMBL API.  This
version has been rewritten by JP for better diagnostics and to support
the annotation of SNPs from different organisms and EnsEMBL versions
within a single executable.

=head2 Commandline parameters

 -i | --infile        Input file in Karin Kassahn format
 -o | --dcc2out       Output file in DCC2 format
 -q | --dccqout       Output file in DCCQ format
 -p | --dcc1out       Output file in DCC1 format
 -l | --logfile       log file (optional)
 -g | --organism      Organism; default=human
 -e | --ensver        EnsEMBL version; default=70
 -r | --release       DCC release; default=11
 -d | --domains       Ensembl domains file
 -y | --symbols       Ensembl gene ids and symbols file
 -t | --type          Valid values: somatic, germline
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message


=over

=item B<-i | --infile>

Mandatory. The input file is in DCC1 format (see QCMG wiki) and is a 
direct output of the qSNP java program.

=item B<-o | --dcc2out>

Mandatory. The DCC2 format output file (see QCMG wiki).

=item B<-q | --dccqout>

Mandatory. The DCCQ format output file (see QCMG wiki).

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-g | --organism>

This must be one of 'human' or 'mouse'.  The default value is 'human'.

=item B<-e | --ensver>

The version of the (1) EnsEMBL annotation files, (2) local EnsEMBL
database, and (3) perl API.  All three of these items B<must> be from the
same version or weird shit will happen.  You have been warned.  The
default value is '70'.

=item B<-r | --release>

DCC release.  This paramter is important because unfortunately, the
column names and contents of the various DCC files changes from release
to release.  If you dont set this appropriately, it is likely that the
script will exit while attetping to load the DCC1 file due to mismatches
between the found and expected columns.  The default value is '11'.

=item B<-d | --domains>

Ensembl domains file.  If none is specified then the --organism and 
--ensver values will be used to pick a default file.  If a
default cannot be chosen then the module will die.

=item B<-d | --symbols>

Ensembl gene symbols file.  If none is specified then the --organism and 
--ensver values will be used to pick a default file.  If a
default cannot be chosen then the module will die.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 AUTHORS

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=item Karin Kassahn, L<mailto:k.kassahn.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: SmallIndelTool.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
