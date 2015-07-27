package QCMG::QBamMaker::SeqFinalDirectory;


##############################################################################
#
#  Module:   QCMG::QBamMaker::SeqFinalDirectory
#  Author:   John V Pearson
#  Created:  2013-05-21
#
#  This module collects information about the BAMs in a seq_final directory.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Digest::CRC;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::DB::Metadata;
use QCMG::FileDir::Finder;
use QCMG::IO::SamReader;
use QCMG::QBamMaker::Mapset;
use QCMG::QBamMaker::SeqFinalBam;
use QCMG::QBamMaker::SeqFinalBamCollection;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;



###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { dirs    => [],
                 verbose => ($params{verbose} ?
                             $params{verbose} : 0),
               };

    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub directories {
    my $self = shift;
    return @{ $self->{dirs} };
}


sub dir_ctr {
    my $self = shift;
    return scalar(@{$self->{dirs}});
}


sub process_directory {
    my $self = shift;
    my $dir  = shift;

    qlogprint "processing directory [$dir]\n";

    my $finder = QCMG::FileDir::Finder->new( verbose => $self->verbose );
    my @files = $finder->find_file( $dir, '\.bam$' );

    # 2014-07-09 Added code to "hide" PreRehead BAM files from this
    # module because they are buggering up qbasepileup/qverify.
    my @finals = grep { /\/seq_final\// }
                 grep { ! /\.PreRehead\.bam$/ } @files;

    my $coll = QCMG::QBamMaker::SeqFinalBamCollection->new(
                   verbose => $self->verbose );

    foreach my $infile (sort @finals) {
#qlogprint "about to make SeqFinalBam for: $infile\n";
        my $bam = QCMG::QBamMaker::SeqFinalBam->new(
                      filename => $infile,
                      verbose  => $self->verbose );
#qlogprint "adding parsed bam to collection: $infile\n";
        $coll->add_record( $bam );
    }

    qlogprint "completed directory [$dir]\n";
    push @{$self->{dirs}}, $dir;
    return $coll;
}


1;

__END__

=head1 NAME

QCMG::QBamMaker::SeqFinalDirectory - processes seq_final directories


=head1 SYNOPSIS

 use QCMG::QBamMaker::SeqFinalDirectory;


=head1 DESCRIPTION

This module is a factory for collecting information about the BAMs in 
a seq_final directory.


=head1 PUBLIC METHODS

=over 2

=item B<new()>

 my $dir = QCMG::QBamMaker::SeqFinalDirectory->new( verbose => 1 );

The B<new()> method takes a single optional parameter (verbose).

=item B<process_directory()>

This routines takes a single parameter which is the root directory to be
searched for BAM files.  It must be an absolute path starting from root.
This is mandatory so that the full BAM pathname can be used elsewhere
for processing.  This directory need not be a QCMG donor directory although
it can be.  All BAM files below the specified directory are examined but
they only make the final list if they sit within a seq_final directory.
You can specify the root of a study and the list will contain all seq_final
BAMs for the study across all donors.  The method returns a
SeqFinalBamCollection object.

=item B<directories()>

A string array of the directories processed by this instance.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013,2014

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
