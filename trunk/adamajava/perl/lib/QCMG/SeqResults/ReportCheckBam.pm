package QCMG::SeqResults::ReportCheckBam;

###########################################################################
#
#  Module:   QCMG::SeqResults::ReportCheckBam.pm
#  Creator:  John V Pearson
#  Created:  2011-05-16
#
#  Logic for command checkbam that looks for BAMs with malformed names.
#
#  $Id: ReportCheckBam.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use File::Find;
use Getopt::Long;
use Pod::Usage;
use vars qw( $SVNID $REVISION @ISA );

use QCMG::SeqResults::Report;
use QCMG::SeqResults::Util qw( qmail bams_in_directory 
                               check_validity_of_bam_names );
use QCMG::Util::QLog;

@ISA = qw( QCMG::SeqResults::Report );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: ReportCheckBam.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    # Set defaults for commandline options from programmer-supplied values
    my %opts       = ();
    $opts{dir}     = $params{dir}     ||  '/mnt/seq_results';
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

    # Print help if help requested or no options supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND checkbam' ) if $opts{help};

    # Get $self from superclass
    my $self = $class->SUPER::new( %opts );

    # Add class-specific values
    $self->{ bams_with_valid_names } = [];
    $self->{ bams_with_invalid_names } = [];

    return $self;
}


sub valid_bam_count {
    my $self = shift;
    return scalar @{ $self->{bams_with_valid_names} };
}


sub valid_bams {
    my $self = shift;
    return @{ $self->{bams_with_valid_names} };
}


sub invalid_bam_count {
    my $self = shift;
    return scalar @{ $self->{bams_with_invalid_names} };
}


sub invalid_bams {
    my $self = shift;
    return @{ $self->{bams_with_invalid_names} };
}


sub execute {
    my $self = shift;

    my @bams = bams_in_directory( $self->dir );

    # Work out who's naughty and who's nice ...
    my ( $rh_valid_names, $rh_invalid_names ) =
        check_validity_of_bam_names( @bams );

    # Save pathnames of BAMs
    push @{$self->{bams_with_valid_names}},   sort values %{$rh_valid_names};
    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{bams_with_valid_names}} ) .
              " BAMs with valid names\n";
    push @{$self->{bams_with_invalid_names}}, sort values %{$rh_invalid_names};
    qlogprint {l=>'TOOL'}, "found " .
              scalar( @{$self->{bams_with_invalid_names}} ) .
              " BAMs with invalid names\n";
}


sub report_text {
    my $self = shift;

    my $text = 
        "\ntimelord.pl checkbam  v$REVISION  [" . localtime() . "]\n" .
        '   dir           '. $self->dir . "\n" .
        '   outfile       '. $self->outfile . "\n" .
        '   email(s)      '. join("\n".' 'x17, $self->emails). "\n" .
        '   verbose       '. $self->verbose ."\n\n";
        
    # Primary report:
    $text .= $self->report_bams_with_invalid_names;

    # In verbose mode, append a list of the files with valid names
    $text .= "\n" . $self->report_bams_with_valid_names if $self->verbose;

    return $text;
}


sub report_bams_with_invalid_names {
    my $self = shift;
    return "BAM files that do not follow the QCMG naming conventions:\n\n" .
           join("\n", $self->invalid_bams) . "\n";
}


sub report_bams_with_valid_names {
    my $self = shift;
    return "BAM files that follow the QCMG naming conventions:\n\n" .
           join("\n", $self->valid_bams) . "\n";
}


sub report_xml_string {
    my $self = shift;
    my $rcb = $self->report_xml_object;
    return $rcb->toString();
}


sub report_xml_object {
    my $self = shift;

    my $rcb = XML::LibXML::Element->new( 'ReportCheckBam' );
    $rcb->setAttribute( 'svn_revision', $REVISION );
    $rcb->setAttribute( 'start_time', localtime().'' );
    my $rps = XML::LibXML::Element->new( 'CliParameters' );
    $rcb->appendChild( $rps );
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

    my $vbs = XML::LibXML::Element->new( 'ValidlyNamedBams' );
    foreach my $bam ($self->valid_bams) {
        my $vb = XML::LibXML::Element->new( 'ValidlyNamedBam' );
        $vb->appendText( $bam  );
        $vbs->appendChild( $vb );
    }
    $rcb->appendChild( $vbs );
    my $ibs = XML::LibXML::Element->new( 'InvalidlyNamedBams' );
    foreach my $bam ($self->invalid_bams) {
        my $ib = XML::LibXML::Element->new( 'InvalidlyNamedBam' );
        $ib->appendText( $bam  );
        $ibs->appendChild( $ib );
    }
    $rcb->appendChild( $ibs );

    return $rcb;
}


1;

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011,2012

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
