package QCMG::FileDir::Finder;

##############################################################################
#
#  Module:   QCMG::FileDir::Finder.pm
#  Creator:  John V Pearson
#  Created:  2010-10-12
#
#  This module finds files or directories based on regular expression
#  matches.
#
#  $Id: Finder.pm 4662 2014-07-23 12:39:59Z j.pearson $
#
##############################################################################

use strict;
#use warnings;

use IO::File;
use Data::Dumper;
use Carp qw( carp croak );

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4662 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Finder.pm 4662 2014-07-23 12:39:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { dirs    => [],
                 files   => [],
                 verbose => ($params{verbose} ? $params{verbose} : 0) };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub dirs {
    my $self = shift;
    return @{ $self->{dirs} };
}


sub files {
    my $self = shift;
    return @{ $self->{files} };
}


sub find_directory {
    my $self    = shift;
    my $dir     = shift;
    my $pattern = shift;

    # Zero out list of matching directories
    $self->{dirs} = [];
    $self->_search_for_directories( $dir, $pattern );
    return @{ $self->{dirs} };
}


sub _search_for_directories {
    my $self    = shift;
    my $dir     = shift;
    my $pattern = shift;

    # Append '/' to dir if not already present;
    $dir .= '/' unless ($dir =~ /\/$/);
 
    qlogprint "processing dir $dir\n" if ($self->verbose > 1);

    opendir(DIR, $dir) || croak "Can't opendir [$dir]: $!";
    my @everything = readdir(DIR);
    closedir DIR;

    foreach my $thing (@everything) {
        # keep all directories except current and parent
        if (-d "$dir/$thing" and
                $thing ne '.' and $thing ne '..' and
                $thing ne '.svn' ) {
            # If subdir matches the pattern then save to list           
            if ($thing =~ /$pattern/) {
                qlogprint "found match: $thing\n" if ($self->verbose > 1);
                push @{ $self->{dirs} }, "$dir$thing";
            }
            # Search the subdir
            $self->_search_for_directories( "$dir$thing", $pattern );
        }
    }
}


sub find_file {
    my $self    = shift;
    my $dir     = shift;
    my $pattern = shift;

    # Zero out list of matching files
    $self->{files} = [];
    $self->_search_for_files( $dir, $pattern );
    return @{ $self->{files} };
}


sub _search_for_files {
    my $self    = shift;
    my $dir     = shift;
    my $pattern = shift;

    # Append '/' to dir if not already present;
    $dir .= '/' unless ($dir =~ /\/$/);
 
    qlogprint "processing dir $dir\n" if ($self->verbose > 1);

    opendir(DIR, $dir) || croak "Can't opendir [$dir]: $!";
    my @everything = readdir(DIR);
    closedir DIR;

    foreach my $thing (@everything) {
        # keep all directories except current and parent
        if (-d "$dir/$thing" and
                $thing ne '.' and $thing ne '..' and
                $thing ne '.svn' ) {
            # Search the subdir
            $self->_search_for_files( "$dir$thing", $pattern );
        }
        if (-f "$dir/$thing") {
            # If file matches the pattern then save to list           
            if ($thing =~ /$pattern/) {
                qlogprint "  found file $thing\n" if ($self->verbose > 1);
                push @{ $self->{files} }, "$dir$thing";
            }
        }
    }
}


sub find_symlinks {
    my $self    = shift;
    my $dir     = shift;
    my $pattern = shift;

    # Zero out list of matching directories
    $self->{symlinks} = [];
    $self->_search_for_symlinks( $dir );
    return @{ $self->{symlinks} };
}


sub _search_for_symlinks {
    my $self    = shift;
    my $dir     = shift;

    # Append '/' to dir if not already present;
    $dir .= '/' unless ($dir =~ /\/$/);

    qlogprint "processing dir $dir\n" if ($self->verbose > 1);

    opendir(DIR, $dir) || croak "Can't opendir [$dir]: $!";
    my @everything = readdir(DIR);
    closedir DIR;

    foreach my $thing (@everything) {
        # keep all directories except current and parent
        if (-d "$dir/$thing" and
                $thing ne '.' and $thing ne '..' and
                $thing ne '.svn' ) {
            # Search the subdir
            $self->_search_for_symlinks( "$dir$thing" );
        }
        if (-l "$dir/$thing") {
            # If file is a symlink then save to list           
            qlogprint "  found symlink $thing\n" if ($self->verbose > 1);
            push @{ $self->{symlinks} }, "$dir$thing";
        }
    }
}




1;
__END__


=head1 NAME

QCMG::FileDir::Finder - Perl module for finding files and directories


=head1 SYNOPSIS

 use QCMG::FileDir::Finder;


=head1 DESCRIPTION

This module provides a set of utility methods for finding files and
directories that match a given pattern.  It is intended to be used as a
singleton - you instantiate one of these objects and keep using it for
multiple queries.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $finder = QCMG::FileDir::Finder->new( verbose => 0 );

The only option that can be passed to the new method is B<verbose>.

=item B<find_directory()>

 my @dirs = $finder->find_directory( '/panfs/seq_mapper/', 'primary.012345' );

This method take 2 parameters - the root directory for the search and
the pattern to be used for the search.  The search directory will appear
at the start of every directory match so it might simplify later parsing
if you always pass in an absolute rather than relative path - your call.
Also note that it will not search .svn directories but it will traverse
other "dot" directories.

The pattern is only matched against the current directory name, not the
full pathname, e.g. against 'local', not '/usr/local'.  So you can't
look for patterns like 'local/blast' under '/usr'. 

=item B<find_file()>

 my @files = $finder->find_file( '/panfs/seq_mapper/', 'dedup.bam' );

As per B<find_directory()> but for files.

=item B<find_symlinks()>

 my @links = $finder->find_symlinks( '/panfs/seq_mapper/', 'job' );

As per B<find_directory()> but for symlinks.

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

$Id: Finder.pm 4662 2014-07-23 12:39:59Z j.pearson $


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
