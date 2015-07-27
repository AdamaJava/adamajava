package QCMG::Util::IniFile;

##############################################################################
#
#  Module:   QCMG::Util::IniFile.pm
#  Creator:  John Pearson
#  Created:  2010-09-30
#
#  This perl module implements a generic system for reading 
#  Windows-style "Ini" configuration files.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;
use IO::File;
use Carp qw( croak carp );
use POSIX;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    croak "You must supply a file parameter to a new IniFile object"
        unless (exists $params{file} and $params{file});

    my $self = { file                 => $params{file},
                 sections             => {},
                 section_order        => [],
                 current_section_name => undef,
                 definition           => [],
                 verbose              => ($params{verbose} ?
                                          $params{verbose} : 0),
               };

    bless $self, $class;

    # We always have a #_No_Section_# section to catch any parameter
    # lines that appear before the first section.
    $self->_add_section( '#_No_Section_#' );
    $self->_current_section_name( '#_No_Section_#' );

    return $self;
}


sub file {
    my $self = shift;
    return $self->{'file'} = shift if @_;
    return $self->{'file'};
}


sub _current_section_name {
    my $self = shift;
    return $self->{'current_section_name'} = shift if @_;
    return $self->{'current_section_name'};
}


sub _current_section {
    my $self = shift;
    return $self->{'sections'}->{ $self->_current_section_name };
}


sub sections {
    my $self = shift;
    my @sections = ();
    foreach my $section_name ($self->section_names) {
        push @sections, $self->section( $section_name );
    }
    return \@sections;
}


sub section_names {
    my $self = shift;
    return @{ $self->{'section_order'} };
}


sub definition {
    my $self = shift;
    return $self->{'definition'} = shift if @_;
    return $self->{'definition'};
}


sub _check_section_is_valid {
    my $self    = shift;
    my $section = shift;
 
    foreach my $sect (keys %{$self->{'legal_lines'}}) {
        return $sect if ($section =~ /$sect/);
    }
    return undef;
}


sub _add_section {
    my $self = shift;
    my $section = shift;

    # Only check validity if a definition has been supplied
    if ($self->definition) {
        die "[$section] does not appear to be a valid section name.\n"
            unless $self->_check_section_is_valid( $section );
    }

    die "Section [$section] appears twice in config file.\n"
        if exists $self->{'sections'}->{$section};

    # Add new section to sections hash and order array
    my $new_section = {};
    $self->{'sections'}->{$section} = $new_section;
    push @{ $self->{'section_order'} }, $section;

    # Return reference to new section array;
    return $new_section;
}


sub section {
    my $self = shift;
    my $section = shift;

    # If section exists, return ref to storage hash else return undef
    if (exists $self->{'sections'}->{$section}) {
        return $self->{'sections'}->{$section};
    }
    else {
        return undef;
    }
}


sub read_config_file {
    my $self = shift;

    my $infile = $self->file;
    my $fh = IO::File->new( $infile, 'r');
    die "Cannot open file configuration file [$infile]: $!\n"
        unless (defined $fh);

    while(my $line = $fh->getline) {
        chomp $line;
        $line =~ s/^\s*//;         # strip leading spaces
        $line =~ s/\s*$//;         # strip trailing spaces
        next unless $line;         # skip blank lines
        next if $line =~ /^[;#]/;  # skip comments

        #print "Processing line - $line\n";

        # Check for new section
        if ($line =~ /^\[(\w+)\]$/) {
            my $section = uc($1);
            $self->_add_section( $section );
            $self->_current_section_name( $section );
        }
        elsif ($line =~ /^(\w+)\s*=\s*(.*)$/) {
            $self->add_var_val_pair( $1, $2 );
        }
        else {
            die "A line from the config file is not recognized: [$line]\n";
        }
    }
    return 1;
}


sub _check_variable_is_valid {
    my $self = shift;
    my $var  = shift;
 
    # Get name of matching section in legal_lines structure
    my $sect = $self->_check_section_is_valid( $self->_current_section_name );

    my $ra_lines = $self->{'legal_lines'}->{$sect};

    foreach my $valid_var (@{$ra_lines}) {
        #print "checking vars: $var =~ $valid_var\n";
        return $valid_var if ($var =~ /$valid_var/);
    }

    die 'In section [', $self->_current_section_name,
        "] of the config file, the line \"$var=...\" is not valid.\n";
}


sub add_var_val_pair {
    my $self = shift;
    my $var  = uc( shift );
    my $val  = shift;
 
    # Only check validity if a definition has been supplied
    if ($self->definition) {
        # Check that $var is legal in the current section
        return undef unless $self->_check_variable_is_valid( $var );
    }

    $self->_current_section->{ $var } = $val;

    return 1;
}


1;
__END__


=head1 NAME

QCMG::Util::IniFile - Perl module for reading Windows-style INI files


=head1 SYNOPSIS

 use QCMG::Util::IniFile;


=head1 DESCRIPTION

B<2012-06-06 JVP: this module is incomplete - it is missing key
functionality including "write" and checks for validity of key=value
pairs in sections>

This module will read Windows-style INI files.  Since there is no clear
consensus on the exact properties of an INI file, the expectations for
use with this module are laid out in detail below.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $ini = QCMG::Util::IniFile->new( file    => 'my_config.ini',
                                     verbose => 0 );

=item B<section()>

 $ini->section( 'XML' );

Returns a reference to the data structure for the specified section.

=item B<section_names()>

 my @sections_names = $ini->section_names;

Returns an array of section names in the order they appared in the INI
file.

=item B<file()>

=item B<sections()>

=item B<definition()>

=item B<section()>

=item B<read_config_file()>

=item B<add_val_val_pair()>

=item B<verbose()>

 $ini->verbose();

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

Copyright (c) The University of Queensland 2010-2012

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
