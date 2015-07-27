package QCMG::IO::INIFile;

###########################################################################
#
#  Module:   QCMG::IO::INIFile.pm
#  Creator:  John V Pearson
#  Created:  2012-09-13
#
#  This class implements a basic system for reading INI-style 
#  config files.  The rules for construction of a valid INI file are
#  outlined in the POD for the program.  This class does not do any
#  checking of the INI file.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( cluck confess );
use IO::File;
use Data::Dumper;
use vars qw($VERSION);

use QCMG::Util::QLog;

( $VERSION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    confess "new() requires a 'file' parameter\n" unless $params{file};

    my $self = { 'filename'              => $params{'file'},
                 'verbose'               => ( $params{'verbose'} || 0 ),
                 'CreationTime'          => localtime().'',
                 'Version'               => $VERSION, 
                 'section_order'         => [],
                 'sections'              => {},
                 'unprocessed_sections'  => {},
                 'allow_duplicate_rules' => 0,
                 'unprocessed_rules'     => 0,
               };
    bless $self, $class;

    # Check whether user asked to allow duplicate names for rules within
    # a section
    $self->_allow_duplicate_rules( 1 )
        if (exists $params{allow_duplicate_rules} and
            defined $params{allow_duplicate_rules});

    # Check whether user asked to have the rules also made available in
    # unprocessed form, i.e. an array of lines exactly as they were in
    # the original INI file.
    $self->_unprocessed_rules( 1 )
        if (exists $params{unprocessed_rules} and
            defined $params{unprocessed_rules});

    $self->_read_config_file;

    return $self;
}


sub filename {
    my $self = shift;
    return $self->{'filename'} = shift if @_;
    return $self->{'filename'};
}


sub verbose {
    my $self = shift;
    return $self->{'verbose'} = shift if @_;
    return $self->{'verbose'};
}


sub section_names {
    my $self = shift;
    return @{ $self->{'section_order'} };
}


sub section {
    my $self = shift;
    my $sect = shift;
    #my $sect = uc(shift);
    return undef unless exists $self->{sections}->{$sect};
    return $self->{sections}->{$sect};
}


sub unprocessed_section {
    my $self = shift;
    my $sect = shift;
    return undef unless exists $self->{unprocessed_sections}->{$sect};
    my @params = @{ $self->{unprocessed_sections}->{$sect} };
    return \@params;
}


sub param {
    my $self  = shift;
    my $sect  = shift;
    my $param = shift;
    #my $param = uc(shift);
    my $section = $self->section($sect);
    return undef unless defined $section;
    return undef unless exists $self->section($sect)->{$param};
    return $self->section($sect)->{$param};
}


sub to_text {
    my $self = shift;
 
    # Get name of sections in order
    my @sects = $self->_sections();

    my $text = '';
    foreach my $sect_name (@sects) {
        $text .= '['.$sect_name."]\n";
        my @params = sort keys %{ $self->section( $sect_name ) };
        foreach my $param (@params) {
            $text .= $param .'='. $self->param($sect_name,$param) ."\n";
        }
    }
    
    return $text;
}


###########################################################################
#                          PRIVATE METHODS                                #
###########################################################################


sub _read_config_file {
    my $self = shift;

    my $infile = $self->filename;
    my $fh = IO::File->new( $infile, 'r');
    die "Cannot open file configuration file [$infile]: $!\n"
        unless (defined $fh);

    # Read contents of file
    my $text = '';
    while (my $line = $fh->getline) {
        #chomp $line;
        $line =~ s/^\s*//;         # strip leading spaces
        $line =~ s/\s*$//;         # strip trailing spaces
        next unless $line;         # skip blank lines
        next if $line =~ /^[;#]/;  # skip comments (starts with ; or #)
        $text .= $line . "\n";
    }

    # Split contents so it ends up as a sequence of pairs of [title] and
    # "rules" elements.
    my @sections = split /^\[([\w:]+)\]\s*\n/m, $text;
    shift @sections;  # ditch leading blank element

    while (@sections) {
        # Read off a pair of title/rules elements
        #my $section_name = uc( shift @sections );
        my $section_name = shift @sections;
        my @params  = split /\n/, shift @sections;
        my %params  = ();
        foreach my $param (@params) {
            # If unprocessed_rules, push the param onto an array
            if ($self->_unprocessed_rules) {
                push @{ $self->{unprocessed_sections}->{ $section_name } }, $param;
            }
            else {
                my ($key,$val) = split /=/, $param, 2;
                $key =~ s/\s+$//;
                $val =~ s/^\s+//;
                #$key = uc($key);
                $key = $key;
                # Special handling if duplicate rules are allowed
                if ($self->_allow_duplicate_rules) {
                    push @{ $params{$key} }, $val;
                }
                else {
                    if (exists $params{$key}) {
                        warn "in section $section_name, rule $key has duplicate values: ",
                             $params{$key},', ',$val,"\n";
                    }
                    $params{ $key } = $val;
                }
            }
        }
        push @{ $self->{'section_order'} }, $section_name;
        $self->{'sections'}->{ $section_name } = \%params;
    }
}


sub _sections {
    my $self = shift;
    return @{ $self->{'section_order'} };
}


sub _allow_duplicate_rules {
    my $self = shift;
    return $self->{'allow_duplicate_rules'} = shift if @_;
    return $self->{'allow_duplicate_rules'};
}


sub _unprocessed_rules {
    my $self = shift;
    return $self->{'unprocessed_rules'} = shift if @_;
    return $self->{'unprocessed_rules'};
}


1;


__END__

=head1 NAME

QCMG::IO::INIFile - Parse INI-style config file


=head1 SYNOPSIS

 use QCMG::IO::INIFile;

 my $ini = QCMG::IO::INIFile->new( 
               file    => 'qpindel_APGI_2353.ini',
               verbose => 1 );


=head1 DESCRIPTION

This module reads a INI-style configuration file.


=head1 PUBLIC METHODS

=over 2

=item B<new()>

 my $ini = QCMG::IO::INIFile->new( 
               file    => 'qpindel_APGI_2353.ini',
               verbose => 1 );

 my $ini = QCMG::IO::INIFile->new( 
               file                  => 'qpindel_APGI_2353.ini',
               allow_duplicate_rules => 1,
               unprocessed_rules     => 1 );

The new() method accepts the following parameters:

=over 4

=item B<file>

The configuration file to be processed (Windows INI-style).

=item B<verbose>

Verbose level as documented below in L<verbose()>.

=item B<allow_duplicate_rules>

Normal behaviour is that if multiple rules with the same name appear
within the same section, a warning is output for the second and
subsequent rules and the LAST rule is the one that has it's value
stored, i.e. each duplicate rule writes over the valeu for the previous
rule so the last rule wins!  If you set this parameter, the rules will
be stored in arrays, even if there is only one occurrence of a rule. 

=item B<unprocessed_rules>

Normal behaviour is that rules are stored in a hash so (a) there is no
way to get back the original order of the rules, and (b) if the rules
are not of the form name=value then then entire line will end up as they
key in the hash - not useful for cases where a data file is in
psuedo-INI file format but the rules are actually CSV lines.  In this
case, you specify this parameter and the rules are stored in an
array for each section.  In this case you need to use a different method
to access the arrays of rules - unprocessed_section().

=back

=item B<creator_module_version()>

VERSION of the software module that created the current object.  This
not the same as the VERSION of the currently loaded software module.

=item B<creation_time()>

String showing time of object creation.

=item B<filename()>

=item B<section_names()>

 @section_names = $ini->section_names();

Array of names of sections in the order they appeared in the INI file.

=item B<section()>

Returns a pointer to the hash of riules for the named section.

=item B<unprocessed_section()>

 $ra_rules = $ini->unprocessed_section( 'Header' );

Returns a pointer to an array of the unprocessed rules lines for the
given section.

=item B<param()>

Pass a section name and a rule name and it given you back the value.

=item B<to_text()>

Returns a string representation of the original file.  Does NOT
currently work if the rules lines were not name=valu format, i.e. any
file where the user needed to specify 'unprocessed_rules' will not work
correctly.

=item B<verbose()>

Prints additional diagnostic and informational messages if set to some
positive integer.  The larger the value, the more detailed the output.

=back


=head1 DEPENDENCIES

=over 2

=back


=head1 AUTHORS

=over 2

=item John Pearson, L<jpearson@tgen.org>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2014

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
