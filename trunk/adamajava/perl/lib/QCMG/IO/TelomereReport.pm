package QCMG::IO::TelomereReport;

###########################################################################
#
#  Module:   QCMG::IO::TelomereReport
#  Creator:  John V Pearson
#  Created:  2014-01-08
#
#  Parse telomere count data and provide reporting functions.
#
#  $Id: TelomereReport.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use vars qw( $SVNID $REVISION );

use QCMG::Util::QLog;

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: TelomereReport.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    croak "QCMG::IO::TelomereReport:new() requires the lines parameter" 
        unless (exists $params{lines} and defined $params{lines});
    
    my $self = { totalreads    => undef,
                 windowsize    => undef,
                 cutoff        => undef,
                 unique_motifs => undef,
                 lines         => [ @{ $params{lines} } ],
                 windows       => [ ],
                 verbose       => ($params{verbose} ?
                                   $params{verbose} : 0),
               };

    bless $self, $class;

    $self->_parse;

    return $self;
}


sub totalreads {
    my $self = shift;
    return $self->{totalreads};
}

sub windowsize {
    my $self = shift;
    return $self->{windowsize};
}

sub cutoff {
    my $self = shift;
    return $self->{cutoff};
}

sub unique_motifs {
    my $self = shift;
    return $self->{unique_motifs};
}


sub windows {
    my $self = shift;
    return @{ $self->{windows} };
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub _parse {
    my $self = shift;

    # TO_DO:
    # We should parse the SUMMARY line to make sure that the same
    # windowsize and cutoff was used axcross all files.  We shoud
    # also require the same includes and excludes but this will be a
    # bit harder to do unless we require the config file contents to
    # be included in the logfile or output.

    # This matches the log file motif line
    my $pattern1 = '([^\s]+) \[(.*?)\] : TotalCov: (\d+), '.
                   'stage 1 coverage: (\d+), '.
                   'stage 2 coverage: (\d+), '.
                   '# FS motifs: (\d+)\((\d+)\), '.
                   '# RS motifs: (\d+)\((\d+)\)\s*';

    # location=chr1:0-9999 r_motifs_total=1 total_reads=0
    # f_motifs_unique=0 f_motifs_total=0 stage1_reads=1
    # stage2_reads=1 r_motifs_unique=1 category=GENOMIC

    my $pattern2 = 'location=([^\s]+) '.
                   'r_motifs_total=(\d+) '.
                   'total_reads=(\d+) '.
                   'f_motifs_unique=(\d+) '.
                   'f_motifs_total=(\d+) '.
                   'stage1_reads=(\d+) '.
                   'stage2_reads=(\d+) '.
                   'r_motifs_unique=(\d+) '.
                   'category=([^\s]+)';

    my $line_count = scalar @{ $self->{lines} };
    my $ctr = 0;

    while ($ctr <= $line_count) {
        my $line = $self->{lines}->[ $ctr++ ];

        if (! defined $line->{message}) {
            warn "no message parsed from line $ctr\n";
        }
        elsif ($line->{message} =~ /total number from input is (\d+)/) {
            $self->{totalreads} = $1;
        }
        elsif ($line->{message} =~ /SUMMARY: \(window size: (\d+), cutoff: (\d+)\)/) {
            #SUMMARY: (window size: 10000, cutoff: 5)
            $self->{windowsize} = $1;
            $self->{cutoff}     = $2;
        }
        elsif ($line->{message} =~ /$pattern1/) {
            # parse record
            push @{ $self->{windows} },
                 { location        => $1,
                   category        => $2,
                   total_reads     => $3,
                   stage1_reads    => $4,
                   stage2_reads    => $5,
                   f_motifs_unique => $6,
                   f_motifs_total  => $7,
                   r_motifs_unique => $8,
                   r_motifs_total  => $9, };
        }
        elsif ($line->{message} =~ /$pattern2/) {
            # parse record
            push @{ $self->{windows} },
                 { location        => $1,
                   r_motifs_total  => $2,
                   total_reads     => $3,
                   f_motifs_unique => $4,
                   f_motifs_total  => $5,
                   stage1_reads    => $6,
                   stage2_reads    => $7,
                   r_motifs_unique => $8,
                   category        => $9, };
        }
        elsif ($line->{message} =~ /No of unique motifs: (\d+)/) {
            $self->{unique_motifs} = $1;
        }
        else {
            # do nothing
        }
    }
}


sub report {
    my $self = shift;

    my %results = ();

    foreach my $rh_window (@{ $self->{windows} }) {
        if ($rh_window->{category} =~ /UNMAPPED/) {
            $results{raw_unmapped} += $rh_window->{stage1_reads};
        }
        elsif ($rh_window->{category} =~ /INCLUDES/i) {
            $results{raw_includes} += $rh_window->{stage1_reads};
        }
        elsif ($rh_window->{category} =~ /GENOMIC/i) {
            $results{raw_genomic} += $rh_window->{stage1_reads};
        }
        else {
            my @attribs = map { $_ .'='. $rh_window->{$_} } 
                          keys %{ $rh_window };
            warn "could not categorise telomere region: ",
                 join( ' ', @attribs ), "\n";
        }
    }

    $results{raw_total} = $results{raw_genomic} + $results{raw_unmapped} +
                          $results{raw_includes};
    if (defined $self->totalreads and $self->totalreads) {
        $results{normalised_unmapped} = sprintf( "%.0f", $results{raw_unmapped} /
                                                         $self->totalreads *
                                                         1000000000 );
        $results{normalised_genomic}  = sprintf( "%.0f", $results{raw_genomic} /
                                                         $self->totalreads *
                                                         1000000000 );
        $results{normalised_includes} = sprintf( "%.0f", $results{raw_includes} /
                                                         $self->totalreads *
                                                         1000000000 );
        $results{normalised_total}    = sprintf( "%.0f", $results{raw_total} /
                                                         $self->totalreads *
                                                         1000000000 );
    }
    else {
        $results{normalised_unmapped} = 'undef';
        $results{normalised_genomic}  = 'undef';
        $results{normalised_includes} = 'undef';
        $results{normalised_total}    = 'undef';
    }

    qlogprint join("\t", $self->windowsize,
                         $self->cutoff,
                         $self->totalreads,
                         $self->unique_motifs,
                         $results{raw_unmapped},
                         $results{raw_includes},
                         $results{raw_genomic},
                         $results{raw_total},
                         $results{normalised_unmapped},
                         $results{normalised_includes},
                         $results{normalised_genomic},
                         $results{normalised_total} ), "\n"
        if $self->verbose;

    return { windowsize          => $self->windowsize,
             cutoff              => $self->cutoff,
             totalreads          => $self->totalreads,
             unique_motifs       => $self->unique_motifs,
             raw_unmapped        => $results{raw_unmapped},
             raw_genomic         => $results{raw_genomic},
             raw_includes        => $results{raw_includes},
             raw_total           => $results{raw_total},
             normalised_unmapped => $results{normalised_unmapped},
             normalised_genomic  => $results{normalised_genomic},
             normalised_includes => $results{normalised_includes},
             normalised_total    => $results{normalised_total} };
}

1;


__END__


=head1 NAME

QCMG::IO::TelomereReport - Telomere Report


=head1 SYNOPSIS

 use QCMG::IO::TelomereReport;


=head1 DESCRIPTION

This module provides a data container for a telomere report as read from
qMotif output.


=head1 METHODS

=over 2

=item B<totalreads()>

=item B<windowsize()>

=item B<cutoff()>

=item B<windows()>

Returns an array of hashrefs where each hash represents a window that
was reported as having motif matching reads.  The hashes look like:

 { location        => 'chr1:0-10500',
   category        => 'INCLUDES',
   total_reads     => 0,
   stage1_reads    => 1564,
   stage2_reads    => 1564,
   f_motifs_unique => 728,
   f_motifs_total  => 1683,
   r_motifs_unique => 587,
   r_motifs_total  => 1215 };

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: TelomereReport.pm 4663 2014-07-24 06:39:00Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2014
Copyright (c) John Pearson 2014

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

=cut
