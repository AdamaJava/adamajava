package QCMG::FileDir::DirectoryObject;

##############################################################################
#
#  Module:   QCMG::FileDir::DirectoryObject.pm
#  Creator:  John V Pearson
#  Created:  2010-09-16
#
#  This internal class implements a directory which can contain other
#  directories and files.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;
use Carp qw( carp croak );
use Data::Dumper;
use QCMG::FileDir::FileObject;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    croak "You must supply a name parameter to a new DirectoryObject"
       unless (exists $params{name} and $params{name});

    # Parent must be supplied but can be undef
    croak "You must supply a parent parameter to a new DirectoryObject"
       unless (exists $params{parent});

    my $self = { name        => $params{name},
                 parent      => $params{parent},  # DirectoryObject
                 level       => ($params{level} ? $params{level} : 1),
                 dir_names   => [],
                 file_names  => [],
                 dir_objs    => {},
                 file_objs   => {},
                 size        => 0,
                 verbose     => ($params{verbose} ? $params{verbose} : 0) };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub name {
    my $self = shift;
    return $self->{name};
}


sub parent {
    my $self = shift;
    return $self->{parent};
}


sub level {
    my $self = shift;
    return $self->{level};
}


# To do this, we'll need to know the name of the parent directory as
# well.

sub full_pathname {
    my $self = shift;
    if (defined $self->parent) {
        return $self->parent->full_pathname . '/'. $self->name;
    }
    else {
        $self->name;
    }
}



sub dirs {
    my $self = shift;
    return $self->{dir_objs};
}


sub dir_names {
    my $self = shift;
    return @{ $self->{dir_names} };
}


sub add_dir_names {
    my $self = shift;
    my @dirs = @_;
    push @{ $self->{dir_names} }, @dirs;
}


sub files {
    my $self = shift;
    return $self->{file_objs};
}


sub file_names {
    my $self = shift;
    return @{ $self->{file_names} };
}


sub add_file_names {
    my $self  = shift;
    my @files = @_;
    push @{ $self->{file_names} }, @files;
}


sub size {
    my $self = shift;
    return $self->{size} = shift if @_;
    return $self->{size};
}


sub directory {
    my $self = shift;
    my $name = shift;

    # We can only return the directory if we find one that matches the name
    if (exists $self->dirs->{ $name } ) {
        return $self->dirs->{ $name };
    }
    else {
        #warn "Directory called $name has not been defined";
        return undef;
    }
}


sub add_directory {
    my $self        = shift;
    my $directory   = shift;  # DirectoryObject

    # Store the directory if it does not already exist
    if (! defined $self->directory( $directory->name() )) {
        $self->dirs->{ $directory->name() } = $directory;
    }
    else {
        croak 'A directory called ' . $directory->name() .
              ' already exists under '. $self->name();
    }
}


sub file {
    my $self = shift;
    my $name = shift;

    #print Dumper $self, $name;
    #confess;

    # We can only return the file if we find one that matches the name
    if (exists $self->files->{ $name } ) {
        return $self->files->{ $name };
    }
    else {
        return undef;
    }
}


sub add_file {
    my $self = shift;
    my $file = shift;  # FileObject

    # Store the file if it does not already exist
    if (! defined $self->file( $file->name )) {
        $self->files->{ $file->name } = $file;
    }
    else {
        croak 'A file called ' . $file->name .
              ' already exists under '. $self->name;
    }
}


sub to_text {
    my $self = shift;
    my $indent = shift;

    return $self->size_as_string . 
           $indent x $self->level . 
           $self->name . "\n";
}


sub _get_names_of_files_and_subdirectories {
    my $self = shift;

    # Extract the directories and files
    my $this_dir = $self->full_pathname();

    opendir(DIR, $this_dir) || croak "Can't opendir [$this_dir]: $!";
    my @everything = readdir(DIR);
    closedir DIR;

    my @all_dirs  = ();
    my @all_files = ();
    foreach my $thing (@everything) {
        # keep all directories except current and parent
        if (-d "$this_dir/$thing" and
                $thing ne '.' and $thing ne '..' and
                $thing ne '.svn' ) {
            push @all_dirs, $thing;
        }
        elsif (-f "$this_dir/$thing") {
            push @all_files, $thing;
        }
        else {
            #warn "This may be something unexpected: $thing";
        }
    }

    $self->add_file_names( @all_files );
    $self->add_dir_names( @all_dirs );
}


sub _process_files {
    my $self = shift;

    foreach my $file ($self->file_names) {
        my $new_file = QCMG::FileDir::FileObject->new(
                             name => $file,
                             parent  => $self,
                             verbose => $self->verbose );
        #print Dumper $new_file;
        $self->add_file( $new_file );
    }
}


sub _process_subdirs {
    my $self = shift;

    foreach my $dir ($self->dir_names) {
        my $new_dir = QCMG::FileDir::DirectoryObject->new(
                            name    => $dir,
                            parent  => $self,
                            level   => $self->level +1,
                            verbose => $self->verbose );
        $new_dir->process();
        $self->add_directory( $new_dir );
    }
}


sub _tally_size() {
    my $self = shift;

    my $size = 0;
    foreach my $file (values %{$self->files}) {
        #print 'tallying file '. $file->name . ' ' . $file->size . "\n";
        $size += $file->size;
    }
    foreach my $dir (values %{$self->dirs}) {
        #print 'tallying subdir  '. $dir->name . ' ' . $dir->size . "\n";
        $size += $dir->size;
    }

    $self->size( $size );
    #print 'total dir  '. $self->name . ' ' . $self->size . "\n";
}


sub process {
    my $self = shift;

    $self->_get_names_of_files_and_subdirectories();
    $self->_process_files();
    $self->_process_subdirs();
    $self->_tally_size();
}


sub logprint {
    my $self = shift;
    my $vlvl = shift;  # verbose level
    if ($self->verbose >= $vlvl) {
        print STDOUT '[' . localtime() . '] ';
        print STDOUT $_ foreach @_;
    }
}


sub size_as_string {
    my $self = shift;
    my $size = $self->size;
    my $stmp = ( $size > 1024**3 ) ? sprintf( "%6.1f", $size / 1024**3 ).'G' :
               ( $size > 1024**2 ) ? sprintf( "%6.1f", $size / 1024**2 ).'M' :
               ( $size > 1024**1 ) ? sprintf( "%6.1f", $size / 1024**1 ).'K' :
                                     sprintf( "%6.0f",   $size ).'B' ;
    return $stmp;
}


1;
__END__


=head1 NAME

QCMG::FileDir::DirectoryReport - Perl module for reporting on a directory


=head1 SYNOPSIS

 use QCMG::FileDir::DirectoryObject;


=head1 DESCRIPTION

This module provides an interface for reporting on directories including
sizes of files with specified extensions.  Each DirectoryObject can
contain multiple DircetoryObjects and FileObjects.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $dir = QCMG::Bioscope::DirectoryObject->new(
               file    => '/panfs/seq_raw/S0416_20100211_1_FragBC'
               verbose => 0 );
 print $dir->as_xml();

=item B<as_xml()>

 $ps->as_xml();

Returns the contents of the parsed DirectoryObject as XML.
Note that this does <B>not</B> include a document type line.

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
