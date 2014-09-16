#!/usr/bin/perl

##############################################################################
#
#  Program:  bioscope2ma.pl
#  Author:   John V Pearson
#  Created:  2010-03-09
#
#  Convert bioscope ma files to regular ma format.
#
#  $Id: bioscope2ma.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;

use QCMG::Bioscope::MatchFile;
use QCMG::Bioscope::Properties;

use vars qw( $CVSID $REVISION $VERBOSE $VERSION );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $CVSID ) = '$Id: bioscope2ma.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;



###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile          = '';
    my $properties      = '';
    my $outfile         = '';
       $VERBOSE         = 0;
       $VERSION         = 0;
    my $help            = 0;
    my $man             = 0;

    my $cmdline = $0 .' '. join(' ', @ARGV);
    print $cmdline;

    # If no params then print usage
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \$infile,         # -i
           'p|properties=s'       => \$properties,     # -p
           'o|outfile=s'          => \$outfile,        # -o
           'v|verbose+'           => \$VERBOSE,        # -v
           'version!'             => \$VERSION,        #
           'h|help|?'             => \$help,           # -?
           'man|m'                => \$man             # -m
           );

    # Handle calls for help, man, version
    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;
    if ($VERSION) { print "$CVSID\n"; exit }

    # Deal with compulsory inputs
    die "No input file specified" unless $infile;
    die "No properties file specified" unless $properties;
    die "No output file specified" unless $outfile;

    print "\nbioscope2ma.pl  v$REVISION  [" . localtime() . "]\n",
          "   infile        $infile\n",
          "   properties    $properties\n",
          "   outfile       $outfile\n",
          "   verbose       $VERBOSE\n\n" if $VERBOSE;

    my $bma = BioscopeMa->new( filename  => $infile,
                               verbose   => $VERBOSE );

    my $prop = BioscopeProperties->new( filename  => $properties,
                                        verbose   => $VERBOSE );

    my $outfh = IO::File->new( $outfile, 'w' );
    die 'Unable to open file ', $outfile, " for writing: $!"
        unless defined $outfh;

    process_file( $bma, $prop, $outfh );

    print '['.localtime(). "] bioscope2ma.pl - complete\n" if $VERBOSE;
}


sub process_file {
    my $bma    = shift;
    my $prop   = shift;
    my $outfh  = shift;

    print $outfh $_ foreach @{ $bma->headers };

    while (my $rec = $bma->next_record) {
        # Deconstruct defline
        my $defline = $rec->defline;
        chomp $defline;
        my ($id, @matches) = split /\,/, $defline; 
        # If we found matches then do the transform
        if (scalar(@matches)) {
            my @new_matches = ();
            foreach my $match (@matches) {
                if ($match =~ /^(\d+)_(-{0,1}\d+)\.(\d+):
                               \((\d+)\.(\d+)\.(\d+)\):q(\d+)$/x) {
                   # do the tranform             
                   push @new_matches, join('.', $prop->contig_name($1),$2,$3);
                }
                else {
                    die "match $match does no match our pattern";
                }
            }

            print $outfh join( "\t", $id, scalar(@matches), @new_matches),"\n",
                         $rec->sequence;
        }
        else {
            print $outfh join( "\t", $id, scalar(@matches)),"\n",
                         $rec->sequence;
        }
    }
}


###########################################################################
#
#  Module:   BioscopeProperties
#  Creator:  John V Pearson
#  Created:  2010-03-10
#
#  Reads properties files output by AB SOLiD Bioscope pipeline.
#
###########################################################################


package BioscopeProperties;

use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    die "BioscopeMa:new() requires a filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename           => $params{filename},
                 headers            => [],
                 contigs            => {},
                'reference.length'  => 0,
                'number.of.contigs' => 0,
                 version            => '',
                 verbose            => ($params{verbose} ?
                                       $params{verbose} : 0),
               };

    bless $self, $class;

    $self->_parse_file;

    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub contig_name {
    my $self   = shift;
    my $contig = shift;

    if (exists $self->{contigs}->{ $contig }) {
        return $self->{contigs}->{ $contig }->{name};
    }
    return undef;
}


sub contig_length {
    my $self   = shift;
    my $contig = shift;

    if (exists $self->{contigs}->{ $contig }) {
        return $self->{contigs}->{ $contig }->{length};
    }
    return undef;
}


sub _headers {
    my $self = shift;
    return $self->{headers};
}

sub _parse_file {
    my $self = shift;

    my $fh = IO::File->new( $self->filename, 'r' );
    die 'Unable to open properties file [', $self->filename,
        "] for reading: $!" unless defined $fh;

    print 'processing properties file [', $self->filename, "]\n"
        if $self->verbose;

    my %contigs = ();
    while (my $line = $fh->getline) {
        chomp $line;
        $line =~ s/\s+$//;        # trim trailing spaces
        next if ($line =~ /^#/);  # skip comments
        next unless $line;        # skip blank lines

        if ($line =~ /^#/) {
            push @{ $self->headers }, $line;
        }
        elsif ($line =~ /^reference\.length=(\d*)$/) {
            $self->{'reference.length'} = $1;
        }
        elsif ($line =~ /^number\.of\.contigs=(\d*)$/) {
            $self->{'number.of.contigs'} = $1;
        }
        elsif ($line =~ /^version=(.*)$/) {
            $self->{version} = $1;
        }
        elsif ($line =~ /^c\.(\d+)\.H=(.*)$/) {
            $self->{contigs}->{$1}->{name} = $2;
        }
        elsif ($line =~ /^c\.(\d+)\.L=(.*)$/) {
            $self->{contigs}->{$1}->{length} = $2;
        }
        else {
            die "Can't parse this line: $line\n";
        }
    }

}


###########################################################################
#
#  Module:   BioscopeMa
#  Creator:  John V Pearson
#  Created:  2010-03-10
#
#  Reads ma alignment files output by AB SOLiD Bioscope pipeline.
#
###########################################################################


package BioscopeMa;

use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    die "BioscopeMa:new() requires a filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename        => $params{filename},
                 headers         => [],
                 record_ctr      => 0,
                 _defline        => '',
                 _sequence       => '',
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    # If there a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.

    if ( $params{zipname} ) {
        my $fh = IO::Zlib->new( $params{zipname}, 'r' );
        die 'Unable to open ', $params{zipname}, "for reading: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
        $self->filehandle( $fh );
    }
    elsif ( $params{filename} ) {
        my $fh = IO::File->new( $params{filename}, 'r' );
        die 'Unable to open ', $params{filename}, "for reading: $!"
            unless defined $fh;
        $self->filename( $params{filename} );
        $self->filehandle( $fh );
    }

    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}

sub filehandle {
    my $self = shift;
    return $self->{filehandle} = shift if @_;
    return $self->{filehandle};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub headers {
    my $self = shift;
    return $self->{headers};
}

sub records {
    my $self = shift;
    return $self->{record_ctr};
}



sub _incr_record_ctr {
    my $self = shift;
    return $self->{record_ctr}++;
}


sub next_record {
    my $self = shift;

    # Read lines, checking for and processing any headers
    # and only return once we have a record

    while (my $line = $self->filehandle->getline()) {
        # Note that there is intentionlly NO chomping to simplify output!

        if ($line =~ /^#/) {
            push @{ $self->{headers} }, $line;
        }
        else {

            if ($line =~ /^>/) {
                if ($self->{_defline}) {
                    $self->_incr_record_ctr;
                    if ($self->verbose) {
                        # Print progress messages for every 1M records
                        print( '[' . localtime() . '] '. $self->{record_ctr},
                               " records processed\n" )
                            if $self->{record_ctr} % 1000000 == 0;
                    }
                    my $rec = BioscopeMaRecord->new(
                               defline  => $self->{_defline},
                               sequence => $self->{_sequence},
                               verbose  => $self->verbose );
                    $self->{_defline}  = $line;
                    $self->{_sequence} = '';
                    return $rec;
                }
                $self->{_defline}  = $line;
                $self->{_sequence} = '';
            }
            else {
                $self->{_sequence} .= $line
            }
        }
    }

    # If we got this far then we may be finished or one record to go.
    if ($self->{_defline}) {
        $self->_incr_record_ctr;
        my $rec = BioscopeMaRecord->new( defline  => $self->{_defline},
                                         sequence => $self->{_sequence},
                                         verbose  => $self->verbose );
        $self->{_defline}  = '';
        $self->{_sequence} = '';
        return $rec;
    }
}



###########################################################################
#
#  Module:   BioscopeMaRecord
#  Creator:  John V Pearson
#  Created:  2010-03-10
#
###########################################################################

package BioscopeMaRecord;

use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    my $self  = { defline      => $params{defline},
                  seq          => $params{sequence},
                  verbose      => ($params{verbose} ? $params{verbose} : 0),
                };

    bless $self, $class;
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub defline {
    my $self = shift;
    return $self->{defline};
}

sub sequence {
    my $self = shift;
    return $self->{seq};
}

1;
__END__


=head1 NAME

Bio::TGen::Util::FASTA - FASTA file IO


=head1 SYNOPSIS

 use Bio::TGen::Util::FASTA;


=head1 DESCRIPTION

This module provides an interface for reading and writing FASTA files.


=head1 AUTHORS

John Pearson L<mailto:bioinfresearch@tgen.org>


=head1 VERSION

$Id: bioscope2ma.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

This module is copyright 2008 by The Translational Genomics Research
Institute.  All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes.  Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection
with the production, marketing, sale or support of any commercial
product or service.  For commercial use, please contact
licensing@tgen.org.  By installing this Software you are agreeing to
the terms of the LICENSE file distributed with this software.

In any work or product derived from the use of this Software, proper
attribution of the authors as the source of the software or data must
be made.  The following URL should be cited:

L<http://bioinformatics.tgen.org/brunit/>

=cut


###########################################################################


__END__

=head1 NAME

collated2gff.pl - Generate ABI GFF from RNA-Mate collated alignment files


=head1 SYNOPSIS

 collated2gff.pl [options]


=head1 ABSTRACT

Take collated-format alignment output files from RNA-Mate and mate each
aligned read with the matching quality values from the original AB qual
file and write the record out in ABI GFF-format files.

=head1 OPTIONS

 -i | --infile        filename for collated input file
 -q | --qualfile      filename for quality file
 -o | --outfile       filename for GFF output
 -b | --buffer        buffer size (records)
 -v | --verbose       print progress and diagnostic messages
      --version       print version and exit immediately
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<collated2gff.pl> was designed to support the Australian ICGC
sequencing project.  It will take 1 or more alignment files in
collated-format as output by RNA-Mate, and match each read in the
collated file(s) with the corresponding read quality string from the
original AB qual file.  The output should be in GFF format.

This script makes a number of assumptions: (1) each collated file is
sorted in read-id order; (2) the qual file contains a superset of the
reads from all the ocllated files, i.e. the expectation is that the
collated files were generated by aligning the .csfasta file that
corrsponds to the .qual file so reads that did not map or mapped more
than once will not appear in the collated file but no read that was not
in the original .csfasta file can appear in a collated file.

=head2 Input format

A collated file looks very much like a .ma file from mapreada, i.e. a
modified FASTA format where the defline carries additional information.
In this case the defline is a tab-separated list of read ID, number of
matches, and one or more map locations where each location is in the
format chromosome.location.mismatch_count.

=head2 Output format

The GFF input format read by this script is a tab-separated text file
with unix-style line endings and the following fields of which the last
two are optional:

     Fieldname      Example value
 1.  seqname        1231_644_1328_F3
 2.  source         solid
 3.  feature        read
 4.  start          97
 5.  end            121
 6.  score          13.5
 7.  strand         -
 8.  frame          .
 9.  [attributes]   b=TAGGGTTAGGGTTGGGTTAGGGTTA;
                    c=AAA;
                    g=T320010320010100103000103;
                    i=1;
                    p=1.000;
                    q=23,28,27,20,17,12,24,16,20,8,13,26,28,2
                      4,13,13,27,14,19,4,23,16,19,9,14;
                    r=20_2;
                    s=a20;
                    u=0,1
 10. [comments]

=head2 Output format

The output format is SAM v0.1.2.


=head2 Commandline Options

=over

=item B<-i | --infile>

Name of AB GFF alignment input file.
Either -i or -z must be specified or the script will exit immediately.

=item B<-z | --zipinfile>

Name of gzipped AB GFF alignment input file.
Either -i or -z must be specified or the script will exit immediately.

=item B<-o | --outfile>

Name of SAM file to write to.  If no filename is specified, the program
will construct one based on the name of the GFF input file.  If the GFF
file ends in .txt or .sam (case insensitive) then the output file is the
same as the input but with the .sam extention in place of the original
extension.  In all other cases, .sam is simply appended to the input
filename.

=item B<-u | --unpaired>

Optional SAM-format file of records that are unmated.

=item B<-b | --buffer>

Number of lines to be kept in buffer while looking for mate-pairs.
Default value is 100000.  With GFF files containing hundreds of millions
of alignments, we can't afford to load it all into memory in order to
spot mate pairs so we'll keep a buffer and if the second read from a
mate-pair is within -b alignments of the first read, then both reads
will be modified prior to being written out.

=item B<-p | --matepair>

Should be specified if the GFF file is from a mate-pair library.  This
is hard to tell from the GFF itself so the user has to specify.  If the
flag is not set, the library is assumed to be fragment.

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

The following usage example will convert a gzipped GFF file to SAM.

  gff2sam.pl -v -z SUSAN_20080709_1_Baylor_2_Frag.gff.gz

B<N.B.> The spaces between the options (B<-z>, B<-o> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:jpearson@tgen.org>

=back


=head1 VERSION

$Id: bioscope2ma.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

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
