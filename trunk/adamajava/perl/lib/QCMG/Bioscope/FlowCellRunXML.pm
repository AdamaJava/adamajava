package QCMG::Bioscope::FlowCellRunXML;

##############################################################################
#
#  Module:   QCMG::Bioscope::FlowCellRunXML.pm
#  Creator:  John V Pearson
#  Created:  2010-09-24
#
#  Parses the com.apldbio.aga.common.model.objects.FlowcellRun XML file
#  that is created in the root seq_raw directory by current (SOLiD 4)
#  sequencing runs.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use Carp qw( carp croak cluck confess );
use File::Find;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;
    
    die "QCMG::Bioscope::FlowCellRunXML->new() requires a file parameter",
        unless (exists $params{file} and $params{file});

    my $self = { file                  => $params{file},
                 _xml                  => '',
                 verbose               => $params{verbose} || 0 };
    bless $self, $class;

    $self->_process_file();
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub _process_file {
    my $self = shift;

    my $infh = IO::File->new( $self->file, 'r' );
    die 'Cannot open flowcellrun xml file ', $self->file, " for reading: $!"
        unless defined $infh;


    my $contents = '';
    while (my $line = $infh->getline) {
        $contents .= $line;
    }

    $self->{_xml} = $contents;

    return $self;
}


sub as_xml {
    my $self = shift;

    my $xml .= '<FlowCellRun file="' . $self->file . '"' . ">\n" .
               $self->{_xml} .
               "</FlowCellRun>\n";

    return $xml;
}


1;
__END__


=head1 NAME

QCMG::Bioscope::FlowCellRunXML - Perl module for parsing XML run description file produced by sequencers


=head1 SYNOPSIS

 use QCMG::Bioscope::FlowCellRunXML;


=head1 DESCRIPTION

This module provides an interface for parsing XML run description files
created by the SOliD sequencers.  This file is usually found in the root
directory of the run under /panfs/seq_raw/.
This module will not usually be directly invoked by the user.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $fc = QCMG::Bioscope::FlowCellRunXML->new(
                file    => 'S0416_20100819_1_FragPEBC.xml',
                verbose => 0 );
 print $fc->as_xml();

=item B<as_xml()>

 $fc->as_xml();

Returns the contents of the XML file as XML.  Note that this
does not include a document type line.

=item B<verbose()>

 $blm->verbose();

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
