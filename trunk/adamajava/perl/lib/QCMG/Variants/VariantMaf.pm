package QCMG::Variants::VariantMaf;

##############################################################################
#
#  Module:   QCMG::Variants::VariantMaf.pm
#  Creator:  John V Pearson
#  Created:  2013-08-09
#
#  This class pulls apart MAF files as written by the QCMG variant
#  calling pipeline.  In particular, it parses the headers to extract
#  useful information.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;

use QCMG::IO::MafReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %CLASS_GLOBALS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    die "new() you must supply a file parameter"
       unless (exists $params{file} and $params{file});

    my $self = { file         => $params{file},
                 verbose      => $params{verbose} || 0 };
    bless $self, $class;

    my $mafreader = QCMG::IO::MafReader->new( filename => $params{file},
                                              verbose  => $params{verbose} );
    $self->{mafreader} = $mafreader;

    return $self;
}



sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub info {
    my $self = shift;

    # Extract info from the TOOL lines
    my @comments = $self->{mafreader}->comments;

    my %attribs = ();
    foreach my $comment (@comments) {
        if ($comment =~ /#Q_DCCMETA\s+analysisId\s+(.*)$/i) {
            $attribs{analysisId} = $1;
        }
        elsif ($comment =~ /#Silent\/non-silent ratio : (.*)$/i) {
            $attribs{silent_nonsilent_ratio} = $1;
        }
        elsif ($comment =~ /#Ti\/Tv ratio : (.*)$/i) {
            $attribs{ti_tv_ratio} = $1;
        }
        elsif ($comment =~ /#dbSNP annotation percentage : (.*)$/i) {
            $attribs{dbsnp_annotation_pc} = $1;
        }
        elsif ($comment =~ /#Q_EXEC ToolName\s+(.*)$/i) { 
            $attribs{tool_name} = $1;
        }
        elsif ($comment =~ /#Q_EXEC ToolVersion\s+(.*)$/) {
            $attribs{tool_version} = $1;
        }
    }

    # Count records - using the MafReader next_record() method is too
    # slow because it parses every record into a perl object.  We'll
    # open the file ourself and count non-comment lines -1.
    my $filename = $self->{mafreader}->filename;
    my $fh = IO::File->new( $filename, 'r' );
    die "Unable to open $filename for reading: $!\n"
        unless defined $fh;

    my $ctr = 0;
    while (my $line = $fh->getline()) {
        next if $line =~ /^#/;
        $ctr++;
    }
    # Subtract 1 for the column header line
    $attribs{record_count} = $ctr -1;

    return \%attribs;
}


1;
__END__


=head1 NAME

QCMG::Variant::VariantMaf - parse information out of a QCMG variant MAF


=head1 SYNOPSIS

 use QCMG::Variant::VariantMaf;

 my $file = 'APGI_3205.Somatic.ALL.snv.maf',
 my $maf  = QCMG::Variant::VariantMaf->new( file==> $file, verbose => 1 );


=head1 DESCRIPTION

QCMG MAF files contain extensive structured headers that contain
information that can be used to judge the validity of the MAF.  This
class has methods to extract that information.


=head1 PUBLIC METHODS

=over

=item B<new()>
 
 my $maf  = QCMG::Variant::VariantMaf->new( file==> $file, verbose => 1 );

Pass in the name of the file (compulsory), and the verbose level
(optional).

=item B<verbose()>

 $qsnp->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

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
