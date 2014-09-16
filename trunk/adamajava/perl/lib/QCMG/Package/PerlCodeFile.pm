package QCMG::Package::PerlCodeFile;

###########################################################################
#
#  Module:   QCMG::Package::PerlCodeFile
#  Creator:  John V Pearson
#  Created:  2011-09-19
#
#  Data container for a file containing perl code (script or module).
#
#  $Id: PerlCodeFile.pm 4664 2014-07-24 08:17:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Pod::Strip;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4664 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: PerlCodeFile.pm 4664 2014-07-24 08:17:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    die "Must pass a 'file' parameter to new() method"
        unless (exists $params{file} and defined $params{file});

    my $self = { file     => $params{file},
                 contents => '',
                 requires => [],
                 classes  => [],
                 searches => [],
                 verbose  => $params{verbose} || 0 };
    bless $self, $class;

    $self->_initialise();
    return $self;
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub contents {
    my $self = shift;
    return $self->{contents};
}


sub classes {
    my $self = shift;
    return @{$self->{classes}};
}


sub requires {
    my $self = shift;
    return $self->{requires};
}


sub searches {
    my $self = shift;
    return $self->{searches};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub _initialise {
    my $self = shift;

    my $file = $self->file;

    # Slurp file
    my $contents;
    {
        local $/ = undef;
        my $fh = IO::File->new( $file, 'r' );
        die "Unable to open perl code file [$file] for reading: $!"
            unless defined $fh;
        $contents = $fh->getline;
        $fh->close;
    }
    $self->{contents} = $contents;

# JVP 2011-09-23
#
# Need to rethink this code - for files with multiple classes, not sure
# how to handle version - should be done on a per-class basis.
# Part of the problem is our current eva{} block which is faulty - it 
# will trigger a search for the class which is NOT what we want - we want
# the version of the code we already have parsed out of the text file.
# We are going to need to do something like eval the whole perl code
# text and then pull versions out of that.  Tricky-sticky-icky.
#
#    # If a class was specified then try to get a version number
#    if ($self->class) {
#        eval 'use '.$self->class;
#        if ($@) {
#            warn $@;
#        }
#        else {
#            # Try to get the class version with eval-trickery
#            $self->{version} = eval '$'.$self->class."::VERSION" ||
#                               eval '$'.$self->class."::REVISION" || '';
#        }
#    }

    my $code = $self->code;

    # Try to spot any use/require statements in code
    while ($code =~ m/^\s*use\s+([^\s\(]+).*$/mg) {
        my $module = $1;
        chomp $module;
        $module =~ s/;$//;  # ditch trailing ;
        push @{ $self->requires }, $module; 
    }

    # Try to spot any packages inside this file
    while ($code =~ m/^\s*package\s+([^\s]+).*$/mg) {
        my $class = $1;
        chomp $class;
        $class =~ s/;$//;  # ditch trailing ;
        push @{ $self->{classes} }, $class; 
    }
}


sub code {
    my $self = shift;

    # Comment out any POD blocks using Pod::Strip.  Note the unusual call
    # structure where you register the string where the POD-stripped
    # text will be placed.  If $nostrip is set then the POD is replaced
    # by empty comments to keep line numbers intact

    my $podstrip = Pod::Strip->new;
    my $podless  = '';
    $podstrip->output_string(\$podless);
    $podstrip->parse_string_document( $self->contents );

    # __END__ and __DATA__ can be problematic.  If they just delineate
    # the end of code and start of POD then we are safe in just dropping
    # everything after the token BUT if __DATA__ is being used to hide
    # data in the code file then we are screwed.  For the moment, we
    # just drop and hope for the best.  Caveat Emptor.

    my @patterns = ( qw/ __END__ __DATA__ / );
    foreach my $pattern (@patterns) {
        if ($podless =~ /^$pattern\s*$/m) {
            my ($code,$other) = split /^$pattern\s*$/m, $podless, 2;
            $podless = $code;
            warn $pattern .' found in file '. $self->file ."\n"
                if ($self->verbose);
        }
    }

    return $podless;
}


sub matching_requires {
    my $self    = shift;
    my $pattern = shift;

    my @matches = ();
    foreach my $module (@{ $self->requires }) {
        if ($module =~ m/$pattern/) {
            #print " use/require $module matches pattern $pattern\n";
            push @matches, $module; 
        }
    }

    return @matches;
}


1;

__END__

=head1 NAME

QCMG::Package::PerlCodeFile - File containing perl code


=head1 SYNOPSIS

 use QCMG::Package::PerlCodeFile;


=head1 DESCRIPTION

This module provides a data container for a file that contains perl
code.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $pf = QCMG::Package::PerlCodeFile->new( file => $filename );

This method creates a new PerlCodeFile object from a perl code file.
Initialization includes parsing the file so some of the values for this
object (including file name) cannot be changed once the object has been
created.  Parameters that can be supplied include:

 file - string; compulsory; code file to be parsed
 verbose - integer; higher numbers give more logging

=item B<file()>

 print $pf->file;

This value cannot be changed as it relates to the file that was parsed
to create this object.

=item B<contents()>
 
 $text = $pf->contents;

This accessor-only method returns the full text of the perl code file
as read from disk.  It is not processed in any way.

=item B<code()>

 $code = $pf->code;

This method returns the contents of the perl code file with POD
stripped out.

=item B<classes()>

This accessor-only method returns a list of any classes that appear to
be present in this perl code file.  Should be 0 if the file is a simple
script but both scripts and module files are allowed to contain multiple
classes so all bets are off on this one.  Caveat-bloody-emptor.

=item B<verbose()>

Print progress and diagnostic messages. 
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: PerlCodeFile.pm 4664 2014-07-24 08:17:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

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
