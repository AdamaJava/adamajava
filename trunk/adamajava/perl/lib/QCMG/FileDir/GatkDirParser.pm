package QCMG::FileDir::GatkDirParser;

##############################################################################
#
#  Module:   QCMG::FileDir::GatkDirParser.pm
#  Creator:  John V Pearson
#  Created:  2010-10-12
#
#  This module pulls apart a variants/GATK directory to determine key aspects
#  of the variant calling.  For much of its functionality it relies on
#  files associated with running qSNP in HATK mode so a raw GATK
#  directory will not be parsed well (maybe not at all).
#
#  $Id: GatkDirParser.pm 4662 2014-07-23 12:39:59Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use Carp qw( carp croak confess );
use vars qw( $SVNID $REVISION %CLASS_GLOBAL );

use QCMG::FileDir::Finder;
use QCMG::FileDir::GatkDirRecord;
use QCMG::Util::QLog;

( $REVISION ) = '$Revision: 4662 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: GatkDirParser.pm 4662 2014-07-23 12:39:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { finder    => undef,
                 qsnp_objs => [],
                 verbose   => ($params{verbose} ? $params{verbose} : 0) };
    bless $self, $class;

    my $finder = QCMG::FileDir::Finder->new( verbose => $self->verbose );
    $self->{finder} = $finder;

    return $self;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub _finder {
    my $self = shift;
    return $self->{finder};
}


sub parse  {
    my $self = shift;
    my $dir  = shift;

    my @gatk_objs = ();

    # Find all variants/GATK directories
    my @gatkdirs = $self->_finder->find_directory( $dir, 'GATK' );
    foreach my $gatkdir (sort @gatkdirs) {

        # Find all qSNP runs (hopefully in UUID format)
        my @rundirs = $self->_finder->find_directory( $gatkdir, '.*' );
        foreach my $rundir (sort @rundirs) {

            my $obj = QCMG::FileDir::GatkDirRecord->new( dir     => $rundir,
                                                         verbose => $self->verbose );
            push @gatk_objs, $obj;
        }
    }

    $self->{gatk_objs} = \@gatk_objs;
    return \@gatk_objs;
}


sub completion_report  {
    my $self = shift;

    # Put in field names as header
    my @fields = QCMG::FileDir::GatkDirRecord->variant_run_fields;
    my $text = join("\t", @fields). "\n";

    # Write a line for each qSNP variant call directory
    foreach my $gatk (@{ $self->{gatk_objs} }) {
        $text .= $gatk->completion_report_text;
    }

    return $text;
}


sub duplicate_report  {
    my $self = shift;

    # Put in field names as header
    my @fields = QCMG::FileDir::GatkDirRecord->variant_run_fields;
    my $text = join("\t", @fields). "\n\n";

    # Place all of the variant calls into a hash keyed on the names of
    # the normal and tumour BAM files being compared.  
    my %seen_before = ();
    foreach my $gatk (@{ $self->{gatk_objs} }) {
        my $rh_data = $gatk->completion_report;
        # If we don't have the BAM names then skip this record
        next unless ($rh_data->{qsnplog_normal_bam} and
                     $rh_data->{qsnplog_tumour_bam});
        # Push this record onto our hash of arrays
        push @{ $seen_before{ $rh_data->{qsnplog_normal_bam} }->{ $rh_data->{qsnplog_tumour_bam} } }, $rh_data;
    }

    # Output any cases where more than 1 call is seen with the same
    # tumour and normal BAM files
    foreach my $normal (sort keys %seen_before) {
        my $rh_tumours = $seen_before{ $normal };
        foreach my $tumour (sort keys %{ $rh_tumours }) {
            my @calls = @{ $seen_before{ $normal }->{ $tumour } };
            if (scalar(@calls) > 1) {
                foreach my $rh_call (@calls) {
                    # Output values in same order as header
                    my @vals = map { $rh_call->{$_} } @fields;
                    $text .= join("\t",@vals) . "\n";
                }
                $text .= "\n";
            }
        }
    }

    return $text;
}

1;

__END__


=head1 NAME

QCMG::FileDir::GatkDirParser - Perl module for parsing GATK variant directories


=head1 SYNOPSIS

 use QCMG::FileDir::GatkDirParser;


=head1 DESCRIPTION

This module pulls apart a variants/GATK directory to determine key aspects
of the variant calling.  For much of its functionality it relies on
files associated with running qSNP in GATK mode so a raw GATK
directory will not be parsed well (maybe not at all).


=head1 PUBLIC METHODS

=over

=item B<new()>

 $raw = QCMG::FileDir::GatkDirParser->new( verbose => 0 );

There is currently only one valid parameter that can be supplied to the
constructor - verbose.

=item B<parse()>

 $raw->parse( '/panfs/seq_results/proj/sample/variants/GATK/my_calls' );

This method triggers the parsing of all variants/GATK directories under
the supplied root directory.  It returns an arrayref of 
QCMG::FileDir::GatkDirRecord objects.  This is a factory method and can
be called multiple times.  Reports are generated based on the GATK run
directories found by the most recent invocation of parse().

=item B<completion_report()>

This method calls B<completion_report()> on all of the GatkDirRecord
objects from the most recent invocation of B<parse()> and puts them
together into a single text string along with a matching header line.
This report could be used to populate a database table.

=item B<verbose()>

 $raw->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: GatkDirParser.pm 4662 2014-07-23 12:39:59Z j.pearson $


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
