#!/usr/bin/perl

##############################################################################
#
#  Program:  sam_splitter.pl
#  Author:   John V Pearson
#  Created:  2010-02-02
#
#  Take one or more SAM files and split the records into sequence-specific
#  SAM files.  A typical use would be to take a SAM file containing a
#  whole-genome alignment and split it into multiple smaller
#  chromosome-specific SAM files.
#
#  $Id: sam_splitter.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;

use vars qw( $CVSID $REVISION $VERBOSE $VERSION );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $CVSID ) = '$Id: sam_splitter.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my @infiles         = ();
    my $outdir          = '';
    my $stem            = '';
       $VERBOSE         = 0;
       $VERSION         = 0;
    my $help            = 0;
    my $man             = 0;

    # Never know when the original commandline might come in handy
    my $cmdline = $0 .' '. join(' ', @ARGV);

    # If no params then print usage
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \@infiles,        # -i
           'o|outdir=s'           => \$outdir,         # -o
           's|stem=s'             => \$stem,           # -s
           'v|verbose+'           => \$VERBOSE,        # -v
           'version!'             => \$VERSION,        #
           'h|help|?'             => \$help,           # -?
           'man|m'                => \$man             # -m
           );

    # Handle calls for help, man, version
    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;
    if ($VERSION) { print "$CVSID\n"; exit }

    # Allow for ,-separated lists of infiles
    @infiles = map { split /\,/,$_ } @infiles;

    # Append '/' to outdir if not already present;
    $outdir .= '/' if ($outdir and $outdir !~ m!/$!);

    # Input file is compulsory
    die "No input file specified" unless $infiles[0];

    my $shuffler = SAMShuffler->new( infiles => \@infiles,
                                     outdir  => $outdir,
                                     stem    => $stem,
                                     verbose => $VERBOSE );

    print "\nsam_splitter.pl  v$REVISION  [" . localtime() . "]\n",
          '   infile(s)     ', join("\n".' 'x17, @infiles), "\n",
          "   outdir        $outdir\n",
          '   stem          ' . $shuffler->stem . "\n",
          "   verbose       $VERBOSE\n\n" if $VERBOSE;

    # Process the SAM file(s)
    $shuffler->shuffle;

    # Print record count report
    $shuffler->report;

    print '['. localtime() ."] sam_splitter.pl - complete\n" if $VERBOSE;
}



###########################################################################
#
#  Module:   SAMShuffler
#  Creator:  John V Pearson
#  Created:  2009-01-26
#
#  Takes a series of SAM records and for each one it checks whether
#  there is already an open file and if not it opens one.  Once it is
#  sure there is an appropriate file open, it writes out the SAM record.
#
###########################################################################


package SAMShuffler;

use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    die "SAMShuffler:new() requires a infiles parameter"
        unless (exists $params{infiles} and defined $params{infiles});

    my $self = { infiles         => ($params{infiles} ? $params{infiles} : []),
                 stem            => ($params{stem} ? $params{stem} : ''),
                 outdir          => $params{ outdir },
                 outfiles        => {},
                 verbose         => ($params{verbose} ? $params{verbose} : 0),
                 version         => '2',
               };

    bless $self, $class;

    # If no output filename stem supplied, build one based on first input SAM
    if (! $self->stem) {
        # Does the filename contain an extension (it should!)
        my $infile = $self->infiles->[0];  # first input (SAM) filename
        # Ditch any path stuff prepended to the filename
        $infile =~ s!.*\/!!g;
        if ($infile =~ m/^(.*)\.([[:alnum:]]+)$/) {
            my $name = $1;
            my $extension = $2;
            $self->stem( $name );
        }
    }

    return $self;
}


sub shuffle {
    my $self = shift;

    # Process each SAM file
    foreach my $file (@{ $self->infiles }) {
        my $sam = IO::File->new( $file, 'r' );
        die "Unable to open SAM file $file for reading: $!"
            unless defined $sam;

        print "[". localtime() ."] processing SAM file $file\n"
            if $self->verbose;

        while (my $line = $sam->getline) {
            chomp $line;
            next if ($line =~ /^@/);  # skip headers
            next unless $line;        # skip blank lines
            
            $self->write_record( $line );
        }
    }
}


sub write_record {
    my $self       = shift;
    my $sam_record = shift;

    my @fields = split /\t/, $sam_record;

    # If we don't already have a file open for this sequence then open one
    if (! defined $self->infiles->{ $fields[2] }) {
        my $filename = $self->outdir .
                       $self->stem .
                       '_' . $fields[2] . '.sam';

        my $fh = IO::File->new( $filename, 'w' );
        die "Unable to open $filename for writing: $!" unless defined $fh;

        print '['. localtime() ."] creating SAM file $filename\n"
            if $self->verbose; 

        # Save filehandle into our hash of open files
        $self->outfiles->{ $fields[2] } = { filename => $filename,
                                            handle   => $fh,
                                            count    => 0 };
    }

    # Finally, write out the record
    $self->outfiles->{ $fields[2] }->{ handle }->print( $sam_record, "\n" );
    $self->outfiles->{ $fields[2] }->{ count }++;
}


sub report {
    my $self = shift;

    my $total = 0;
    my @seqs = sort keys %{ $self->infiles() };
    print "\nRecord count for each output SAM:\n";
    foreach my $seq (@seqs) {
       printf "%11u", $self->infiles->{ $seq }->{ count };
       print  '  ', $self->infiles->{ $seq }->{ filename }, "\n";
       $total += $self->infiles->{ $seq }->{ count };  # tally total
    }
    print "-----------\n";
    printf "%11u", $total;
    print  "  Total\n";
}


sub infiles {
    my $self = shift;
    return $self->{infiles};
}

sub outfiles {
    my $self = shift;
    return $self->{outfiles};
}

sub outfile_names {
    my $self = shift;
    my @filenames = sort 
                    map { $self->outfiles->{ $_ }->{ filename } }
                    keys %{ $self->{outfiles} };
   return \@filenames;
}


sub stem {
    my $self = shift;
    return $self->{stem} = shift if @_;
    return $self->{stem};
}

sub outdir {
    my $self = shift;
    return $self->{outdir} = shift if @_;
    return $self->{outdir};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}


1;
__END__


=head1 NAME

sam_splitter.pl - Split a SAM file(s) into sequence-specific SAM files


=head1 SYNOPSIS

 sam_splitter.pl [options]


=head1 ABSTRACT

Take one or more SAM files and sort the records into sequence-specific
SAM files.  A typical use would be to take a SAM file containing a
whole-genome alignment and split it into multiple smaller
chromosome-specific SAM files.

=head1 OPTIONS

 -i | --infile        filename for SAM input file
 -o | --outdir        directory to place new SAM files in
 -s | --stem          output file name stem
 -v | --verbose       print progress and diagnostic messages
      --version       print version and exit immediately
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<sam_splitter.pl> was designed to support the Australian ICGC
sequencing project.  It will take 1 or more alignment files in
SAM format and sort all of the records in those files into separate
SAM files for each sequence.  There is no presumption that the SAM files
will be sorted in any order.  Also note that this program does not
resort records so if you input 2 SAM files then the output SAM files
will certainly not be sorted - all the records for the first SAM will be
copied into the output files followed by all the records for the second
SAM will be copied.

The output SAM filenames are constructed by taking the value of the stem
option (see commandline parameters below) and appending an underscore
plus the name of the sequence and a .sam extension.  For example, is a
single SAM file called B<my_sequence.sam> was submitted and it contained
alignments to chromosomes 1-22, X, Y and M then you would expect to see
output SAM files called B<my_sequence_chr1.sam>,
B<my_sequence_chr2.sam> etc.


=head2 Commandline Options

=over

=item B<-i | --infile>

Name of SAM files.
Multiple input files are allowed and may be specified 2 ways.  Firstly,
the B<-i> option may be specified multiple times, and secondly each
B<-i> may be followed by a comma-separated list (no spaces!) of
filenames.  You may also mix the 2 modes.
This option must be specified or the script will exit immediately.

=item B<-o | --outdir>

Directory where the output SAM files should be created.  Default is the
current working directory.

=item B<-s | --stem>

Stem to be used to construct output SAM file names.  If no stem is
specified then the name of the first input SAM file will be used as a
stem.  This is not always a desirable behaviour but it is useful in the
case where you have a single SAM file to be spliot in which case the
output filenames will default to being based on the input SAM file name.
bName of SAM file to write to. 

=item B<--version>

Print the program version number and exit immediately.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will split a single SAM into multiple SAMs
with the output file names being based on the input filename (default)
and with the output files written to the 
/panfs/imb/j.pearson/ directory:

  sam_splitter.pl -v -i my_sequence.sam -o /panfs/imb/j.pearson/

B<N.B.> The spaces between the options (B<-i>, B<-o> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: sam_splitter.pl 4669 2014-07-24 10:48:22Z j.pearson $


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
