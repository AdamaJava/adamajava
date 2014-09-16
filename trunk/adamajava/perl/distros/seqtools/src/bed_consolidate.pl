#!/usr/bin/perl -w

##############################################################################
#
#  Program:  bam_consolidate.pl
#  Author:   John V Pearson
#  Created:  2010-10-03
#
#  Quick-n-dirty script for shuffling together multiple BED files.
#
#  $Id: bed_consolidate.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );
use POSIX qw( floor );

use QCMG::IO::BedReader;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: bed_consolidate.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my @infiles        = ();
    my $outfile        = '';
       $VERBOSE        = 0;
       $VERSION        = 0;
    my $help           = 0;
    my $man            = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \@infiles,       # -i
           'o|outfile=s'          => \$outfile,       # -o
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    # Allow for ,-separated lists of infiles
    @infiles = map { split /\,/,$_ } @infiles;

    print( "\nbed_consolidate.pl  v$REVISION  [" . localtime() . "]\n",
           '   infile(s)     ', join("\n".' 'x17, @infiles), "\n",
           "   outfile       $outfile\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $beds = Consolidator->new( filenames => \@infiles,
                                  outfile   => $outfile,
                                  verbose   => $VERBOSE );

    $beds->process;
}





##############################################################################
#
#  Module:   Consolidator
#  Author:   John V Pearson
#  Created:  2010-10-03
#
#  Handle multiple BED files and shuffle them together.
#
##############################################################################

package Consolidator;

use strict;
use warnings;
use Data::Dumper;
use IO::File;
use Carp qw( carp croak );

sub new {
    my $class = shift;
    my %params = @_;

    croak "Consolidator:new() requires the filenames parameter"
        unless (exists $params{filenames} and defined $params{filenames});

    my $self = { filenames        => $params{filenames},
                 outfile          => $params{outfile},
                 record_ctr       => 0,
                 _running_readers => {},
                 _waiting_readers => {},
                 _outfh           => '',
                 verbose          => ($params{verbose} ?
                                      $params{verbose} : 0),
               };
    bless $self, $class;

    $self->_initialise;

    return $self;
}


sub filenames {
    my $self = shift;
    return @{ $self->{filenames} };
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub _initialise {
    my $self = shift;

    # Open the inputs
    my $ctr = 1;
    foreach my $filename ($self->filenames) {
        my $bed = QCMG::IO::BedReader->new( filename => $filename,
                                            verbose  => $self->verbose );
        $bed->name( $ctr++ );
        $self->{_running_readers}->{$filename} = $bed;
    }

    # Open the output
    my $outfh =IO::File->new( $self->{outfile}, 'w' )
       or croak 'Can\'t open BED file [', $self->{outfile}, "] for writing: $!";
    $self->{_outfh} = $outfh;
}


sub _running_readers {
    my $self = shift;
    my @readers = values %{ $self->{_running_readers} };
    return @readers;
}

sub _running_reader_count {
    my $self = shift;
    my @readers = $self->_running_readers;
    return scalar(@readers);
}

sub _waiting_readers {
    my $self = shift;
    my @readers = values %{ $self->{_waiting_readers} };
    return @readers;
}

sub _waiting_reader_count {
    my $self = shift;
    my @readers = $self->_waiting_readers;
    return scalar(@readers);
}


# Determine lexicographically smallest (lowest) sequence
sub _lowest_sequence {
    my $self        = shift;
    my $current_seq = shift;

    foreach my $reader ($self->_running_readers) {
        my $this_seq = $reader->current_record_seq;
        if (! defined $this_seq) { print Dumper $self, $reader };
        if ($this_seq lt $current_seq) {
            $current_seq = $this_seq;
        }
    }
    return $current_seq;
}

# Determine smallest (lowest) base position
sub _lowest_position {
    my $self        = shift;
    my $current_pos = shift;

    foreach my $reader ($self->_running_readers) {
        my $this_pos = $reader->current_record_start;
        if ($this_pos < $current_pos) {
            $current_pos = $this_pos;
        }
    }
    return $current_pos;
}



sub process {
    my $self = shift;

    my $current_pos = 0;
    my $ctr         = 0;
    my $rh_report   = {};

    if ($self->verbose) {
        print "Running BED file readers:\n";
        foreach my $reader ($self->_running_readers) {
            print $reader->name, ' - ', $reader->filename,"\n";
        }
        print "\n";
    }

    # We are going to have to manage the BedReader objects carefully.  We
    # will actually need two hashes - "running readers" and "waiting
    # readers " because as each reader is exhausted of records for the 
    # current sequence, if it still has records for other sequences then
    # we will move it from the running to waiting new hash.  Once all 
    # readers are exhausted for the current sequence, all the still-active
    # readers are copied from the waiting hash back to the running hash,
    # the smallest sequence is chosen and we start processing again.

    # Find smallest sequence - N.B. string '~~~' is very large in ASCII
    my $current_seq = $self->_lowest_sequence( '~~~' );

    # We will use an infinite loop here and we will explicitly exit
    RECORD: while (1) {
        $ctr++;
        
        my @running_readers = $self->_running_readers;
        my @waiting_readers = $self->_waiting_readers;

        # Find smallest sequence - N.B. string '~~~' is very large in ASCII
        $current_seq = $self->_lowest_sequence( '~~~' );

        # Any reader which is not sitting on a record for the current
        # sequence needs to be moved to _waiting_readers.
        foreach my $reader ($self->_running_readers) {
            my $this_seq = $reader->current_record_seq;
            if ($this_seq ne $current_seq) {
                $self->_move_reader_to_waiting( $reader->filename );
            }
        }

        # Determine smallest remaining record
        my $current_pos = $self->_lowest_position( 1e25 );

        # Now read records from any reader with a record equal to the lowest
        # remaining read and do the tallying.
        my @outputs = ( $current_seq, $current_pos, $current_pos+1, '', 0 );

        foreach my $reader ($self->_running_readers) {
            my $this_pos = $reader->current_record_start;
            if ($this_pos == $current_pos) {
                my $ra_fields = $reader->current_record;
                my @fields = @{ $ra_fields };
                $outputs[3] =  $fields[3];   # set strand
                $outputs[4] += $fields[4];   # tally reads
                $rh_report->{$reader->filename}->{$fields[0]} += $fields[4];
                $rh_report->{'total'}->{$fields[0]} += $fields[4];

                # Read next record and if it returns undef then this
                # reader is completely exhausted so undef the reader
                # otherwise check if we're still on the same sequence
                # and delete if not
                my $rec = $reader->next_record;
                if (!defined $rec) {
                    $self->_close_exhausted_reader( $reader->filename );
                }
                elsif (defined $reader->current_record and 
                       $reader->current_record_seq ne $current_seq) {
                    $self->_move_reader_to_waiting( $reader->filename );
                }
            }
        }

        # If this was the last reader then we are done
        last if $self->_all_readers_exhausted;
        # Reactivate waiting readers if no more running readers
        $self->_bring_back_waiting_readers
            if ($self->_running_reader_count == 0);
        
        #Print out our hard-won data!
        $self->{_outfh}->print( join("\t",@outputs), "\n" );
    }

    $self->{_report} = $rh_report;
    print Dumper $rh_report;
}


sub _all_readers_exhausted {
    my $self = shift;

    # Tally up the remaining readers and return true (1, no readers left)
    # or false (0, readers left)
    return (($self->_running_reader_count + $self->_waiting_reader_count) == 0)
            ? 1 : 0;
}

sub _move_reader_to_waiting {
    my $self = shift;
    my $key  = shift;

    my $reader = $self->{_running_readers}->{$key};
    print( '[' . localtime() . ']  Moving reader ', $reader->name,
           ' to waiting, the next record [', $reader->record_ctr,
           '] is for sequence ' ,
           $reader->current_record_seq, "\n") if $self->verbose;
    $self->{_waiting_readers}->{$key} = $self->{_running_readers}->{$key};
    delete $self->{_running_readers}->{$key};
}

sub _close_exhausted_reader {
    my $self = shift;
    my $key  = shift;

    my $reader = $self->{_running_readers}->{$key};
    print('[' . localtime() . ']  Closing reader ', $reader->name,
          " because it is empty\n") if $self->verbose;
    $reader->filehandle->close;
    delete $self->{_running_readers}->{ $key };
}

sub _bring_back_waiting_readers {
    my $self = shift;
#    print('[' . localtime() . "]  Bringing back all waiting readers\n")
#        if $self->verbose;
    foreach my $key (keys %{ $self->{_waiting_readers} }) {
        my $reader = $self->{_waiting_readers}->{$key};
        $self->{_running_readers}->{$key} = $reader;
        print('[' . localtime() . ']  Bringing back waiting reader ',
              $reader->name, ' at record ', $reader->record_ctr, "\n")
              if $self->verbose;
    }
    $self->{_waiting_readers} = {};
}


1;

__END__

=head1 NAME

bed_consolidate.pl - Perl script for outputting BED from BAM


=head1 SYNOPSIS

 bam2bed.pl [options]


=head1 ABSTRACT

This is a very quick-n-dirty script for concatenating multiple BED files.


=head1 OPTIONS

 -i | --infile        name of BAM file
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<bed_consolidate.pl> is designed to follow bam2bed.pl and to consolidate
multiple BED files into a single "super" BED.
This script is very basic and makes the following assumptions:

 1. The BED is sorted by sequence and position within each sequence.
 2. The first record in all the BED files will have the same sequence.

=head2 Commandline Options

=over

=item B<-i | --infile>

Full pathname to BED file(s) to be processed.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT unless the B<-l> option is used to
specify a logfile.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: bed_consolidate.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010

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
