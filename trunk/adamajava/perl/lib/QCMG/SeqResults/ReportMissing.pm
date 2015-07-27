package QCMG::SeqResults::ReportMissing;

###########################################################################
#
#  Module:   QCMG::SeqResults::ReportMissing.pm
#  Creator:  John V Pearson
#  Created:  2011-05-16
#
#  Logic for command checkbam that looks for BAMs with malformed names.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use File::Find;
use Getopt::Long;
use Pod::Usage;
use Data::Dumper;

use QCMG::DB::QcmgReader;
#use QCMG::DB::GeneusReader;
#use QCMG::DB::TrackLite;
use QCMG::SeqResults::Util qw( qmail seqmapped_bams
                               check_validity_of_bam_names );
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    # Defaults (if any) for commandline options
    my %opts       = ();
    $opts{dir}     = $params{dir}     || '/mnt/seq_results';
    $opts{emails}  = $params{emails}  || [ ];
    $opts{outfile} = $params{outfile} || '';
    $opts{verbose} = $params{verbose} || 0;
    $opts{help}    = 0;

    # Use GetOptions module to parse commandline options
    my $results = GetOptions (
           'd|dir=s'         => \$opts{dir},           # -d
           'e|email=s'       =>  $opts{emails},        # -e
           'o|outfile=s'     => \$opts{outfile},       # -o
           'v|verbose+'      => \$opts{verbose},       # -v
           'h|help|?'        => \$opts{help},          # -h
           );

    # If no email recipient supplied, add invoker as default
    push @{$opts{emails}}, $ENV{QCMG_EMAIL} unless scalar @{$opts{emails}};

    # Print help if help requested or no options supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND missing' ) if $opts{help};

    # Create the object
    my $self = { bams_with_valid_names   => {},
                 bams_with_invalid_names => {},
                 mapsets_in_lims         => {},
                 lims_and_disk           => [],
                 lims_only               => [],
                 disk_only               => [],
                 %opts };
    bless $self, $class;
}


sub dir {
    my $self = shift;
    return $self->{dir};
}


sub email {
    my $self = shift;
    return $self->{emails};
}


sub emails {
    my $self = shift;
    return @{ $self->{emails} };
}


sub outfile {
    my $self = shift;
    return $self->{outfile};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub mapsets_in_lims {
    my $self = shift;
    return $self->{mapsets_in_lims};
}


sub mapsets_fail_qc {
    my $self = shift;
    return $self->{mapsets_fail_qc};
}


sub lims_and_disk {
    my $self = shift;
    return $self->{lims_and_disk};
}


sub lims_only {
    my $self = shift;
    return @{ $self->{lims_only} };
}


sub lims_only_count {
    my $self = shift;
    return scalar( @{ $self->{lims_only} } );
}


sub disk_only {
    my $self = shift;
    return @{ $self->{disk_only} };
}


sub disk_only_count {
    my $self = shift;
    return scalar( @{ $self->{disk_only} } );
}


sub execute {
    my $self = shift;

    # For the purposes of this module, we only care about BAMs in
    # seq_mapped/, not seq_lib/ and seq_final/.
    my %bams = seqmapped_bams( $self->dir );
    my @bams = sort values %bams;  # values gives full pathname

    my ( $rh_valid_names, $rh_invalid_names ) =
        check_validity_of_bam_names( @bams );

    # Save BAM details including hash of filename => pathname
    $self->{seqmapped_bams}          = \%bams;
    $self->{bams_with_valid_names}   = $rh_valid_names;
    $self->{bams_with_invalid_names} = $rh_invalid_names;

    # Query the LIMS for all mapped mapsets.
    $self->query_lims_for_mapped_mapsets();
    
    # Compare lists of mapped mapsets and BAMs found
    $self->cross_check_mapsets_and_bams();
}


sub output_report {
    my $self = shift;

    # Report is emailed unless -o is specified to direct output to a file
    if ($self->outfile) {
        qlogprint( "writing report to ". $self->outfile ."\n" );
        open OUT, '>'.$self->outfile ||
            die 'Unable to open ',$self->outfile," for writing $!";
        print OUT $self->report_xml_string();
        #print OUT $self->report_text();
        close OUT;
    }
    else {
        qlogprint( "emailing report to ". join(', ', @{$self->email} ) ."\n" );
        qmail( To      => $self->email,
               From    => $ENV{QCMG_EMAIL},
               Subject => $self->lims_only_count .
                          ' BAMs in LIMS and not in '. $self->dir ."\n\n",
               Message => $self->report_text() );
    }
}


sub query_lims_for_mapped_mapsets {
    my $self = shift;

    #my $lims = QCMG::DB::TrackLite->new();
    #$lims->connect();
    #my $ra_rows = $lims->mapped_mapsets;

    my $lims = QCMG::DB::QcmgReader->new();
    if ($lims->all_resources_metadata()) {
        my $ra_recs = $lims->fetch_metadata("mapset");
        my %recs    = ();
        my %fail_qc = ();
        my $ctr = 0;
        foreach my $rec (@{ $ra_recs }) {
            my $mapset = defined $rec->{mapset} ? $rec->{mapset} : 'Unknown'.$ctr++;
            #print Dumper $rec if (! defined $rec->{Mapset});
            # Separate out any mapsets flagged as failing QC
            if ($rec->{failed_qc}) {
                $fail_qc{ $mapset } = $rec;
            }
            else {
                $recs{ $mapset } = $rec;
            }
        }
        $self->{ mapsets_in_lims } = \%recs;
        $self->{ mapsets_fail_qc } = \%fail_qc;
    }
    else {
        qlogprint( "Unable to fetch mapset metadata from LIMS" );
        die;
    }
}


sub cross_check_mapsets_and_bams {
    my $self = shift;

    # Sort mapsets by donor
    my @mapsets = map  { $_->[0] }
                  sort { $a->[1] cmp $b->[1] }
                  map  { [ $_, $self->mapsets_in_lims->{ $_ }->{project} ] }
                  keys %{ $self->mapsets_in_lims };
    my @bams    = sort keys %{$self->{bams_with_valid_names}};

    my @lims_and_disk         = ();
    my @lims_only             = ();
    my @disk_only             = ();
    my %rg_val_probs          = ();
    my @on_disk_but_failed_qc = ();

    # bams_with_valid_names - BAM filename => BAM pathname
    # bams - BAM filenames
    # mapsets - mapset names (BAM filename but without the .bam extension)
    # mapsets in LIMS - mapset => \@lims_row

    # Do the cross checks
    foreach my $mapset (@mapsets) {
        my $bam = $mapset.'.bam';  # BAM corresponding to this mapset
        if (exists $self->{bams_with_valid_names}->{ $bam }) {
            push @lims_and_disk, $self->{bams_with_valid_names}->{ $bam };
            # At this point we will check whether the BAM has an @RG
            # line and if so, whether the SM and LB fields in the BAM
            # match the data in the LIMS

            my $bfile = QCMG::IO::SamReader->new( filename =>
                            $self->{bams_with_valid_names}->{$bam} );
            my $header = $bfile->headers_text;

            # If the BAM has an @RG line then do the extra tests
            if ($header =~ /^\@RG\s+(.*)$/m) {
                my $text = $1;
                my $donor   = $self->mapsets_in_lims->{$mapset}->{project};
                my $library = $self->mapsets_in_lims->{$mapset}->{primary_library} || '';
                my @fields = split /\t/, $text;
                my %field_vals = ();
                foreach my $field (@fields) {
                    my ($key,$val) = split /:/, $field;
                    $field_vals{$key} = $val;
                }
                if (exists $field_vals{SM} and
                    ($field_vals{SM} ne $donor)) {
                    $rg_val_probs{$self->{bams_with_valid_names}->{$bam}} .=
                       'Donor[is:'.$field_vals{SM}.
                       ",should_be:$donor] ";
                }
                if (exists $field_vals{LB} and
                    ($field_vals{LB} ne $library)) {
                    $rg_val_probs{$self->{bams_with_valid_names}->{$bam}} .=
                       'Library[is:'.$field_vals{LB}.
                       ",should_be:$library] ";
                }
            }

            #die;
        }
        else {
            push @lims_only, $self->mapsets_in_lims->{ $mapset };
        }
    }

    foreach my $bam (@bams) {
        my $mapset = $bam;
        $mapset =~ s/\.bam$//;  # mapset corresponding to this BAM
        if (! exists $self->mapsets_in_lims->{ $mapset }) {
            push @disk_only, $self->{bams_with_valid_names}->{ $bam };
        }
        if (exists $self->mapsets_fail_qc->{ $mapset }) {
            push @on_disk_but_failed_qc, $self->{bams_with_valid_names}->{ $bam };
        }
    }

    # Save away all of the results
    $self->{lims_and_disk}         = \@lims_and_disk;
    $self->{lims_only}             = \@lims_only;
    $self->{disk_only}             = \@disk_only;
    $self->{rg_val_probs}          = \%rg_val_probs;
    $self->{on_disk_but_failed_qc} = \@on_disk_but_failed_qc;

    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{disk_only}} ) .
              " BAMs on disk but not in LIMS\n";
    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{lims_only}} ) .
              " BAMs in LIMS but not on disk\n";
    qlogprint {l=>'TOOL'}, "found " .
              scalar( keys( %{$self->{rg_val_probs}} ) ) .
              " BAMs with bad data on \@RG line\n";
    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{on_disk_but_failed_qc}} ) .
              " BAMs on disk but marked in LIMS as failing QC\n";
}


sub report_text {
    my $self = shift;

    my $text = 
        "\ntimelord.pl missing  v$REVISION  [" . localtime() . "]\n" .
        '   dir           '. $self->dir ."\n".
        '   outfile       '. $self->outfile ."\n".
        '   email(s)      '. join("\n".' 'x17, $self->emails) ."\n".
        '   verbose       '. $self->verbose ."\n\n";

    # If there are any disk-only mapsets then report them
    if ($self->disk_only_count) {
        $text .= $self->disk_only_text;
    }

    # Always report on lims-only mapsets
    $text .= "\n" . $self->lims_only_text;

    $text .= "\n" . $self->bams_with_rg_value_problems_text;

    # In verbose mode, report mapsets that are in LIMS and on disk
    if ($self->verbose) {
        $text .= $self->bams_in_lims_and_on_disk_text;
    }

    return $text;
}


sub disk_only_text {
    my $self = shift;
    my @bams = map{ $self->{seqmapped_bams}->{$_} } $self->disk_only;
    return "BAMS found on disk but without a matching LIMS mapset:\n\n" .
           join("\n", @bams) . "\n";
}


sub lims_only_text {
    my $self = shift;

    # Always report on lims-only mapsets
    my $text = "Mapsets found in LIMS but no matching BAM found on disk:\n\n";
    $text .= "Project,Donor,Mapset,Source Code,Material,Failed QC\n";
    { 
        no warnings;  # hide 'uninitialised value' warnings
        foreach my $lims_entry ($self->lims_only) {
            $text .= join( ',', $lims_entry->{parent_project},
                                $lims_entry->{project},
                                $lims_entry->{mapset},
                                $lims_entry->{sample_code},
                                $lims_entry->{material},
                                $lims_entry->{failed_qc} ) . "\n";
        }
    }
    return $text;
}


sub bams_in_lims_and_on_disk_text {
    my $self = shift;
    my $text = "Mapsets found in LIMS and on disk:\n\n";
    $text .= "$_\n" foreach @{ $self->lims_and_disk };
    return $text;
}


sub bams_with_rg_value_problems_text {
    my $self = shift;
    my $text = "Mapsets with \@RG line SM/LB values that don't agree with LIMS:\n\n";
    foreach my $bam (keys %{ $self->{rg_val_probs} }) {
        $text .= "$bam : ". $self->{rg_val_probs}->{$bam} . "\n";
    };
    return $text;
}


sub report_xml_string {
    my $self = shift;
    my $rcb = $self->report_xml_object;
    return $rcb->toString();
}


sub report_xml_object {
    my $self = shift;

    my $rib = XML::LibXML::Element->new( 'ReportMissingBam' );
    $rib->setAttribute( 'svn_revision', $REVISION );
    $rib->setAttribute( 'start_time', localtime().'' );
    my $rps = XML::LibXML::Element->new( 'CliParameters' );
    $rib->appendChild( $rps );
    if ($self->dir) {
        my $rp = XML::LibXML::Element->new( 'CliParameter' );
        $rp->setAttribute( 'dir', $self->dir );
        $rps->appendChild( $rp );
    }
    if ($self->outfile) {
        my $rp = XML::LibXML::Element->new( 'CliParameter' );
        $rp->setAttribute( 'outfile', $self->outfile );
        $rps->appendChild( $rp );
    }
    if ($self->emails) {
        my $rp = XML::LibXML::Element->new( 'CliParameter' );
        $rp->setAttribute( 'emails', join(',',$self->emails) );
        $rps->appendChild( $rp );
    }
    if ($self->verbose) {
        my $rp = XML::LibXML::Element->new( 'CliParameter' );
        $rp->setAttribute( 'verbose', $self->verbose );
        $rps->appendChild( $rp );
    }

    my $bs1 = XML::LibXML::Element->new( 'BamsOnlyOnDisk' );
    foreach my $bam (@{$self->{disk_only}}) {
        my $bx = XML::LibXML::Element->new( 'BamOnlyOnDisk' );
        $bx->appendText( $bam );
        $bs1->appendChild( $bx );
    }
    $rib->appendChild( $bs1 );
           
    my $bs2 = XML::LibXML::Element->new( 'BamsOnlyInLims' );
    foreach my $bam (@{$self->{lims_only}}) {
        my $bx = XML::LibXML::Element->new( 'BamOnlyInLims' );
        $bx->setAttribute( 'project', $bam->{parent_project} );
        $bx->setAttribute( 'donor',   $bam->{project} );
        $bx->setAttribute( 'mapset',  $bam->{mapset} );
        $bs2->appendChild( $bx );
    }
    $rib->appendChild( $bs2 );

    my $bs3 = XML::LibXML::Element->new( 'BamsWithRgMismatches' );
    foreach my $bam (keys %{$self->{rg_val_probs}}) {
        my $bx = XML::LibXML::Element->new( 'BamWithRgMismatches' );
        $bx->setAttribute( 'mapset',   $bam );
        $bx->setAttribute( 'problems', $self->{rg_val_probs}->{$bam} );
        $bs3->appendChild( $bx );
    }
    $rib->appendChild( $bs3 );

    my $bs4 = XML::LibXML::Element->new( 'MapsetsMarkedInLimsAsFailedQc' );
    foreach my $bam (@{$self->{on_disk_but_failed_qc}}) {
        my $bx = XML::LibXML::Element->new( 'MapsetMarkedInLimsAsFailedQc' );
        $bx->appendText( $bam );
        $bs4->appendChild( $bx );
    }
    $rib->appendChild( $bs4 );

    return $rib;
}   


1;

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2013

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
