package QCMG::Package::Collection;

###########################################################################
#
#  Module:   QCMG::Package::Collection
#  Creator:  John V Pearson
#  Created:  2011-09-22
#
#  Data container for managing a collection of PerlCodeFile objects that
#  are to be packaged into a script.
#
#  $Id$
#
###########################################################################


use strict;
use warnings;

use Data::Dumper;
use QCMG::Package::PerlCodeFile;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { pcfs     => [],
                 by_class => {},
                 verbose  => $params{verbose} || 0 };
    bless $self, $class;
}


sub pcfs {
    my $self = shift;
    return @{ $self->{pcfs} };
}


sub add_pcf {
    my $self = shift;
    my $pcf  = shift;

    die "add_module() only accepts objects of class QCMG::Package::PerlCodeFile"
        unless (ref($pcf) eq 'QCMG::Package::PerlCodeFile');
    
    # Store the PerlCodeFile object
    push @{ $self->{pcfs} }, $pcf;

    # Create lookup based on classes (if any)
    foreach my $class ($pcf->classes) {
        $self->{by_class}->{$class} = $pcf;
    }
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub pcf_by_class {
    my $self  = shift;
    my $class = shift;

    return undef unless 
        (defined $class and exists $self->{by_class}->{$class});
    return $self->{by_class}->{$class};
}


sub packaged_code {
    my $self  = shift;

    my $newtext = '';
    foreach my $pcf ($self->pcfs) {
        $newtext .= "\n\n" . '#'x75 . "\n# qperlpackage inlined module\n".
                    '#'x75 . "\n\n";
        $newtext .= $pcf->code;
    }

    return $newtext;
}


sub manifest {
    my $self = shift;

    my $explanation = <<EOEXP;
#
#  This script has been processed through the QCMG-developed
#  qperlpackage utility which 'inlines' modules by renaming them
#  and inserting them directly into the script.  This simplifies
#  distribution of perl scripts by removing external dependencies.
#  This comment block is the qperlpackage manifest which lists all
#  of the modules that have been inlined.
#
EOEXP

    my $manifest = '#'x75 . "\n#\n#  qperlpackage manifest\n" .
                   $explanation;
    foreach my $pcf ($self->pcfs) {
        $manifest .= '#  Inlined from: ' . $pcf->file . "\n";
        foreach my $class ($pcf->classes) {
            $manifest .= "#   Original Module Name: $class\n";
            $manifest .= "#   Inline Module Name:   " . 
                         $self->_new_name( $class ) . "\n";
        }
        $manifest .= "#\n";
    }
    $manifest .= '#'x75 . "\n\n";

    return $manifest;
}


sub new_module_names {
    my $self  = shift;

    my %renames = ();
    foreach my $pcf ($self->pcfs) {
        # Create the new module names
        foreach my $class ($pcf->classes) {
            $renames{ $class } = $self->_new_name( $class );
        }
    }

    return \%renames;
}


sub _new_name {
    my $self = shift;
    my $name = shift;

    my $newname = 'QPP_' . $name;
    $newname =~ s/::/_/g;
    return $newname;
}


1;

__END__

=head1 NAME

QCMG::Package::Collection - Collection of QCMG::Package::PerlCodeFile
objects for perl modules


=head1 SYNOPSIS

 use QCMG::Package::Collection;

 my $pc = QCMG::Package::Collection->new();
 my $m = QCMG::Package::PerlCodeFile->new( file => 'HoHo.pm' );
 $pc->add_module( $m );

 my $packaged_text = $pc->as_text;


=head1 DESCRIPTION

This module helps to manage a collection of perl modules being prepared
for packaging.  It holds QCMG::Package::PerlCodeFile objects.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $pc = QCMG::Package::Collection->new( verbose => 1 );

This method creates a new Collection object from a perl code file.

=item B<verbose()>

Print progress and diagnostic messages. 
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


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
