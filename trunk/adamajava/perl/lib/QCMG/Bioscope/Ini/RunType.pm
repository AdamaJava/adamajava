package QCMG::Bioscope::Ini::RunType;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::RunType.pm
#  Creator:  John V Pearson
#  Created:  2010-08-23
#
#  Superclass for all runtype-specific modules such as Fragment.pm,
#  Matepair.pm etc.  This module holds the logic for creating
#  directories etc.  It consumes some of the same parameters that are
#  used by the Ini.pm and its subclasses, e.g. run_name, run_date,
#  ini_root_dir.  It can also output a config file that lists all of the
#  parameters in use.
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    die "$class - you must supply a value for ini_root_dir to new()"
        unless (exists $params{ini_root_dir} and $params{ini_root_dir});
    die "$class - you must supply a value for run_name to new()"
        unless (exists $params{run_name} and $params{run_name});
    die "$class - you must supply a value for run_date to new()"
        unless (exists $params{run_date} and $params{run_date});
    die "$class - you must supply a value for barcode to new()"
        unless (exists $params{barcode} and $params{barcode});
    die "$class - you must supply a value for physical_division to new()"
        unless (exists $params{physical_division} and
                       $params{physical_division});

    my $self = { ini_root_dir      => $params{ini_root_dir},
                 run_name          => $params{run_name},
                 run_type          => $params{run_type},
                 physical_division => $params{physical_division},
                 barcode           => $params{barcode},
                 run_date          => $params{run_date},
                 verbose           => ($params{verbose} ? $params{verbose} : 0)
                };
    bless $self, $class;

    $self->{mapset} = join('.', $self->{run_name},
                                $self->{physical_division},
                                $self->{barcode} );

    $self->_create_directory;

    return $self;
}


sub _create_directory {
    my $self = shift;

    # We'll consider this as a 3-part operation:
    # 1. check whether ini_root_dir exists else create
    # 2. check whether ini_root_dir/mapset_name exists else create
    # 3. check whether ini_root_dir/mapset_name/run_date exists else create 

    my $dir = $self->{ini_root_dir};

    if (! -d $dir) {
        die "The directory ini_root_dir [$dir] must already exist";
    }
    $dir = join('/',$dir,$self->{mapset});
    $dir =~ s/\/\//\//g;
    if (! -d $dir) {
        mkdir $dir or die "Unable to create directory $dir: $!";
    }
    $dir = join('/',$dir,$self->{run_date});
    $dir =~ s/\/\//\//g;
    if (! -d $dir) {
        mkdir $dir or die "Unable to create directory $dir: $!";
        print "creating directory $dir\n" if $self->verbose;
    }

    $self->{directory} = $dir;
}


sub directory {
    my $self = shift;
    return $self->{directory};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}


1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::Runtype - Superclass for runtype-specific classes


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::RunType;
 use vars qw( @ISA );
 @ISA = qw( QCMG::Bioscope::Ini::RunType );


=head1 DESCRIPTION

This module is the superclass for runtype-specific modules such as
Fragment.pm and MatePair.pm.  It should never be directly invoked by a
user but should be subclassed in any new runtype module.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $ini = QCMG::Bioscope::Ini::RunType->new(
                ini_root_dir     => '/panfs/imb/home/uqjpear1/bioscope/',
                run_name         => 'S0436_20100517_2_Frag',
                run_date         => '20100804',
                );

=back


=head1 SEE ALSO

=over

=item QCMG::Bioscope::Ini::Fragment

=item QCMG::Bioscope::Ini::MatePair

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
