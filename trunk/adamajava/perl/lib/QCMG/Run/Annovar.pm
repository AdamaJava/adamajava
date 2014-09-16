package QCMG::Run::Annovar;

###########################################################################
#
#  Module:   QCMG::Run::Annovar
#  Creator:  John V Pearson
#  Created:  2012-10-24
#
#  Runs executables form the annovar software package.
#
#  $Id: Annovar.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Annovar.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    my $self = { verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };
    bless $self, $class;

    # Set root for annovar install
    $self->annovar_root( $params{annovar_root} ?
                         $params{annovar_root} :
                         '/share/software/annovar/' ),

    # Setup various annovar binaries and check that they are executable
    $self->_setup_annovar_binaries;

    return $self;
}


sub _setup_annovar_binaries {
    my $self = shift;

    my %annbins = ( annotate_variation_bin => 'annotate_variation.pl',
                    convert2annovar_bin    => 'convert2annovar.pl' );

    foreach my $key (sort keys %annbins) {
        my $bin = $annbins{$key};
        $self->{$key} = $self->annovar_root . $bin;
        die "ANNOVAR $bin binary not executable: ", $self->{$key} ,"\n"
            unless (-e $self->{$key});
    }
}


sub annovar_root {
    my $self = shift;
    if (@_) {
        $self->{annovar_root} = shift;
        # Check that annovar_root has a trailing '/'
        $self->{annovar_root} .= '/' unless ($self->{annovar_root} =~ /\/$/);
        # We will need to reset the paths to all binaries
        $self->_setup_annovar_binaries;
    }
    return $self->{annovar_root};
}


sub annotate_variation_bin {
    my $self = shift;
    return $self->{annotate_variation_bin};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub annotate_variation {
    my $self   = shift;
    my %params = @_;

    # Check for mandatory inputs
    die "You must specify the infile to be annotated\n" unless $params{infile};

    my $mode = '-geneanno -dbtype ensgene';
    if ( exists $params{mode} and defined $params{mode} ) {
        if ($params{mode} =~ /^refgene$/i) {
            $mode = '-geneanno -dbtype refgene';
        }
        elsif ($params{mode} =~ /^ensgene$/i) {
            $mode = '-geneanno -dbtype ensgene';
        }
    }
    
    my $buildver = '-buildver hg19 ' . $self->annovar_root .'/hg19';
    if ( exists $params{buildver} and defined $params{buildver} ) {
        if ($params{buildver} =~ /^hg19$/i) {
            $buildver = '-buildver hg19 ' . $self->annovar_root .'/hg19';
        }
        elsif ($params{buildver} =~ /^mm9$/i) {
            $buildver = '-buildver mm9 ' . $self->annovar_root .'/mm9';
        }
    }

    # Check whether user chose to override default executable and then
    # check that it actually is executable
    my $annbin = ( exists $params{exec} and defined $params{exec} ) ?
                 $params{exec} :
                 $self->annotate_variation_bin;
    die "ANNOVAR annotate_variation.pl binary not executable: $annbin\n"
        unless (-e $annbin);

    my $cmdline = "$annbin $mode ". $params{infile} ." $buildver";

    qlogprint( "annotate_variation cmdline: $cmdline\n" ) if $self->verbose;

    if (system($cmdline) != 0) {
        # You can check all the failure possibilities by inspecting $? like this:
        if ($? == -1) {
            qlogprint "failed to execute annotate_variation: $!\n";
        }
        elsif ($? & 127) {
            qlogprint( sprintf( "annotate_variation died with signal %d, %s coredump\n",
                               ($? & 127), ($? & 128) ? 'with' : 'without' ));
        }
        else {
            qlogprint( sprintf "annotate_variation exited with value %d\n", $? >> 8 );
        }
    }

    return 0;
}


1;

__END__


=head1 NAME

QCMG::Run::Annovar - Module to encapsulate running annovar executables


=head1 SYNOPSIS

 use QCMG::Run::Annovar;


=head1 DESCRIPTION

This module provides a standardised way to invoke executables from the
annovar software suite.

=head1 METHODS

=over

=item B<new()>

 my $anv = QCMG::Run::Annovar->new(
               annovar_root => '/share/software/annovar',
               verbose        => 1 );

This method creates a factory that can be used to execute the run
methods.  There are only 2 settable variables here - the root location
of the annovar software (including annotations) and the verbose level.

=item B<annovar_root()>

 $anv->annovar_root( '/panfs/share/jpearson/local_annovar/' );

This method will set the directory that will used by all methods to
locate annovar executables and annotations.  The default is
'/share/sfotware/annovar' which is the correct location for QCMG annovar
on babyjesus so you do not have to det this variable unless you are on a
different machine or want to use a different location, e.g. a local
install for testing a new annovar version.

=item B<verbose()>

 $my_verbose = $anv->verbose;
 $anv->verbose( 2 );

=item B<annotate_variation()>

 $anv->annotate_variation( infile   => 'my_annover_file.txt',
                           mode     => 'ensgene',
                           buildver => hg19',
                           exec     => '/alternative/annotate_variation.pl' );

This method runs the annovar annotate_variation.pl executable.  It takes
4 parameters but only B<infile> is mandatory and the other 3 all have
defaults.  The default mode is B<ensgene> for ensembl gene annotations,
the B<buildver> default is B<hg19> for human annotations and the exec
default is the copy of B<annotate_variantion.pl> that is found in the
annovar_root directory (see B<annovar_root()> for how to set/reset
this directory).

=back 

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: Annovar.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012,2013

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
