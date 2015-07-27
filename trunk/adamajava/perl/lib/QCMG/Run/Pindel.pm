package QCMG::Run::Pindel;

###########################################################################
#
#  Module:   QCMG::Run::Pindel
#  Creator:  John V Pearson
#  Created:  2013-01-10
#
#  Runs executables form the pindel software package.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    my $self = { verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };
    bless $self, $class;

    # Set root for pindel install
    $self->pindel_root( $params{pindel_root} ?
                        $params{pindel_root} :
                        '/panfs/share/software/pindel024s/' ),

    # Setup various pindel binaries and check that they are executable
    $self->_setup_pindel_binaries;

    return $self;
}


sub _setup_pindel_binaries {
    my $self = shift;

    my %annbins = ( pindel2vcf_bin => 'pindel2vcf',
                    pindel_vcf     => 'pindel' );

    foreach my $key (sort keys %annbins) {
        my $bin = $annbins{$key};
        $self->{$key} = $self->pindel_root . $bin;
        die "Pindel $bin binary not executable: ", $self->{$key} ,"\n"
            unless (-e $self->{$key});
    }
}


sub pindel_root {
    my $self = shift;
    if (@_) {
        $self->{pindel_root} = shift;
        # Check that pindel_root has a trailing '/'
        $self->{pindel_root} .= '/' unless ($self->{pindel_root} =~ /\/$/);
        # We will need to reset the paths to all binaries
        $self->_setup_pindel_binaries;
    }
    return $self->{pindel_root};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub pindel2vcf {
    my $self   = shift;
    my %params = @_;

    # Check for mandatory inputs
    die "You must specify the infile to be converted\n" unless $params{infile};

    my $reference = ( exists $params{reference} ) ?
                    $params{reference} :
                    '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa';
    my $reference_label = ( exists $params{reference_label} ) ?
                          $params{reference_label} :
                          'GRCh37_ICGC_standard_v2';
    my $reference_date = ( exists $params{reference_date} ) ?
                         $params{reference_date} :
                         '2010-06-17';
    my $outfile = ( exists $params{outfile} ) ?
                  $params{outfile} :
                  $params{infile} . '.vcf';

     my $cmdline = $self->{pindel2vcf_bin} .' '.
                   "-r $reference ".
                   "-R $reference_label ".
                   "-d $reference_date ".
                   "-p $params{infile} ".
                   "-v $outfile"; 

    qlogprint( "pindel2vcf cmdline: $cmdline\n" ) if $self->verbose;

    if (system($cmdline) != 0) {
        # You can check all the failure possibilities by inspecting $? like this:
        if ($? == -1) {
            qlogprint "failed to execute pindel2vcf: $!\n";
        }
        elsif ($? & 127) {
            qlogprint( sprintf( "pindel2vcf died with signal %d, %s coredump\n",
                               ($? & 127), ($? & 128) ? 'with' : 'without' ));
        }
        else {
            qlogprint( sprintf "pindel2vcf exited with value %d\n", $? >> 8 );
        }
    }

    return 0;
}


1;

__END__


=head1 NAME

QCMG::Run::Pindel - Module to encapsulate running pindel executables


=head1 SYNOPSIS

 use QCMG::Run::Pindel;


=head1 DESCRIPTION

This module provides a standardised way to invoke executables from the
pindel software suite.

=head1 METHODS

=over

=item B<new()>

 my $pin = QCMG::Run::Pindel->new(
               pindel_root => '/panfs/share/software/pindel024s',
               verbose     => 1 );

This method creates a factory that can be used to execute the run
methods.  There are only 2 settable variables here - the root location
of the pindel software and the verbose level.

=item B<pindel_root()>

 $pin->pindel_root( '/panfs/share/jpearson/local_pindel/' );

This method will set the directory that will used by all methods to
locate pindel executables.  The default is
'/panfs/share/software/pindel024s' which is the correct location for QCMG
pindel on babyjesus so you do not have to set this variable unless you are
on a different machine or want to use a different location, e.g. a local
install for testing a new pindel version.

=item B<verbose()>

 $my_verbose = $pin->verbose;
 $pin->verbose( 2 );

=item B<pindel2vcf()>

 $pin->pindel2vcf( infile          => 'APGI_2353_ND_TD_D',
                   outfile         => 'my_shiny.vcf',
                   reference       => 'my_crazy_genome.fa',
                   reference_label => 'GRCh37_ICGC_standard_v9',
                   reference_date  => '2013-01-01' );

This method runs the pindel pindel2vcf executable.  It takes
5 parameters but only B<infile> is mandatory and the other 4 all have
defaults:

 reference -
 /panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa
 reference_label - GRCh37_ICGC_standard_v2
 reference_date - 2010-06-17
 outfile - infile name with .vcf extension added

=back 

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


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
