package QCMG::FileDir::DirectoryReport;

##############################################################################
#
#  Module:   QCMG::FileDir::DirectoryReport.pm
#  Creator:  John V Pearson
#  Created:  2010-09-16
#
#  This module summarizes disk usage by directory and (optionally) file.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use QCMG::FileDir::FileObject;
use QCMG::FileDir::DirectoryObject;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    die "You must supply a dir parameter to a new DirectoryReport"
       unless (exists $params{dir} and $params{dir});

    my $self = { dir         => $params{dir},
                 level       => $params{level} || 1,
                 file_objs   => {},
                 verbose     => $params{verbose} || 0 };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub dir {
    my $self = shift;
    return $self->{dir};
}


sub as_xml {
    my $self    = shift;
    my $depth   = shift;
    my $ra_exts = shift;

    my $xml = '<DirectorySizeReport module="DirectoryReport"' .
              ' module_version="' . $REVISION . '"' .
              ' start_time="' . localtime() . '"' .
              ' directory="' . $self->dir->name . '"' .
              ' depth="' . $depth . '"' .
              ' extensions="' . join(',',@{$ra_exts}) . "\">\n";

    my $indent='  ';

    $xml .= $self->dir_to_xml_1( $self->dir, $depth,
                                 $indent, $ra_exts );

    $xml .= "</DirectorySizeReport>\n";

    return $xml;
}


sub dir_to_xml_1 {
    my $self    = shift;
    my $dir     = shift;
    my $depth   = shift;
    my $indent  = shift;
    my $ra_exts = shift;

    # If depth is defined and is non-zero and this directory level 
    # is lower than depth then return immediately
    return '' if (defined $depth and $depth and $dir->level > $depth);

    my @dir_names  = sort keys %{ $dir->dirs };
    my @file_names = sort keys %{ $dir->files };

    # Count files and subdirectories
    my $file_count = 0;
    my $dir_count  = 0;
    $file_count++ foreach @file_names;
    $dir_count++ foreach @dir_names;

    # Open xml report
    my $dir_size = $dir->size_as_string;
    $dir_size =~ s/^\s+//;
    my $xml = ($indent x $dir->level) . '<DirSizeDirectory' .
              ' name="' . $dir->name . '"' .
              ' size="' . $dir_size . '"' .
              ' size_in_bytes="' . $dir->size . '"' .
              ' file_count="' . $file_count . '"' .
              ' subdir_count="' . $dir_count . '"' .
              ' level="' . $dir->level . "\">\n";

    foreach my $dir_name (@dir_names) {
        $xml .= $self->dir_to_xml_1( $dir->directory( $dir_name ),
                                     $depth,
                                     $indent,
                                     $ra_exts );
    }

    foreach my $file_name (@file_names) {
        my $myfile = $dir->file( $file_name ); 
        my $text_size = $myfile->size_as_string;
        $text_size =~ s/^\s+//;
        my $file_xml = ($indent x ($dir->level+1)) . '<DirSizeFile' .
                       ' name="' . $myfile->name . '"' .
                       ' size="' . $text_size . '"' .
                       ' size_in_bytes="' . $myfile->size . "\" />\n";
        if ($self->verbose >= 2) {
            $xml .= $file_xml;
        }
        else {
            # Process for extensions 
            foreach my $ext (@{ $ra_exts }) {
                if ( $myfile->name =~ m/\.$ext$/ ) {
                    $xml .= $file_xml;
                }
            }
        }
    }

    # Close out directory entry
    $xml .= $indent x $dir->level . "</DirSizeDirectory>\n";

    return $xml;
}


sub text_report_1 {
    my $self    = shift;
    my $depth   = shift;
    my $ra_exts = shift;

    my $indent='   ';

    return $self->dir_to_text_1( $self->dir, $depth, $indent, $ra_exts );
}


sub dir_to_text_1 {
    my $self    = shift;
    my $dir     = shift;
    my $depth   = shift;
    my $indent  = shift;
    my $ra_exts = shift;

    # If depth is defined and is non-zero and this directory level 
    # is lower than depth then return immediately
    return '' if (defined $depth and $depth and $dir->level > $depth);

    my $text = $dir->to_text( $indent );

    my @dir_names  = sort keys %{ $dir->dirs };
    my @file_names = sort keys %{ $dir->files };

    foreach my $dir_name (@dir_names) {
        $text .= $self->dir_to_text_1( $dir->directory( $dir_name ),
                                         $depth,
                                         $indent,
                                         $ra_exts );
    }

    foreach my $file_name (@file_names) {
        my $myfile = $dir->file( $file_name ); 
        if ($self->verbose >= 2) {
            $text .= $myfile->to_text( $indent );
        }
        else {
            # Process for extensions 
            foreach my $ext (@{ $ra_exts }) {
                if ( $myfile->name =~ m/$ext$/ ) {
                    $text .= $myfile->to_text( $indent );
                }
            }
        }
    }

    return $text;
}


1;

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
