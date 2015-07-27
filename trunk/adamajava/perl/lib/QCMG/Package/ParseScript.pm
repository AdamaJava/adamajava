package QCMG::Package::ParseScript;

###########################################################################
#
#  Module:   QCMG::Package::ParseScript
#  Creator:  John V Pearson
#  Created:  2011-09-19
#
#  Parses perl script ready for inlining of Perl Modules.  Reads the
#  script, looks for modules that match the givern pattern, find the
#  modules in a list of library directories, recursively find any
#  matching modules called from the matching modules themselves, parse the
#  modules to remove any POD, rename all occurences of the modules across 
#  all of the code, create a  manifest of the names and versions of the
#  modules (may require use'ing the modules), inline the manifest and 
#  renamed modules, and write out the reworked scripts.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use QCMG::Package::PerlCodeFile;
use QCMG::Package::Collection;
use QCMG::Util::QLog;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    die "Must pass a 'file' parameter to new() method"
        unless (exists $params{file} and defined $params{file});

    my $self = { file       => $params{file},
                 pcf_obj    => QCMG::Package::PerlCodeFile->new(
                                  file => $params{file} ),
                 collection => QCMG::Package::Collection->new(),
                 insert_at  => '__END__',
                 libs       => $params{libs}     || [],
                 patterns   => $params{patterns} || [],
                 verbose    => $params{verbose}  || 0 };
    bless $self, $class;

    # Make sure we have an insert point
    my $file     = $self->{pcf_obj}->file;
    my $contents = $self->{pcf_obj}->contents;
    my $divider  = $self->{insert_at};

    if ($contents !~ /^$divider\s*$/m) {
        die "FATAL: script file $file does not appear to contain $divider'\n";
    }

    return $self;
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub pcf {
    my $self = shift;
    return $self->{pcf_obj};
}


sub contents {
    my $self = shift;
    return $self->pcf->contents;
}


sub collection {
    my $self = shift;
    return $self->{collection};
}


sub libs {
    my $self = shift;
    return $self->{libs};
}


sub patterns {
    my $self = shift;
    return $self->{patterns};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub _dump {
    my $file = shift;
    my $text = shift;

    my $fh = IO::File->new( $file, 'w' );
    die "Unable to open file [$file] for writing: $!" unless defined $fh;
    $fh->print( $text );
    $fh->close;
}


sub write {
    my $self = shift;
    my $file = shift;

    my $fh = IO::File->new( $file, 'w' );
    die "Unable to open file [$file] for writing: $!" unless defined $fh;
    qlogprint( "Writing file [$file]\n" ) if $self->verbose;
    $fh->print( $self->{newtext} );
    $fh->close;
}


sub inline_modules {
    my $self = shift;

    # Go through all the patterns looking for new submodules
    my @matches = ();
    foreach my $pattern (@{ $self->patterns }) {
        my @newmods = $self->pcf->matching_requires( $pattern );
        push @matches, @newmods;
    }

    foreach my $module (@matches) {
        $self->_find_and_parse_modules( $module );
    }

    # Now that we have all of the text, it's time to smush it all
    # together and do the subs that will rename the modules.

    my $newtext = $self->contents;
    my $modtext = $self->collection->packaged_code .
                  "\n\n__QPP_MANIFEST__\n";

    # Sub in the new text
    my $divider = $self->{insert_at};
    $newtext =~ s/^$divider\s*$/$modtext\n$divider\n/m;

    # Set a new local name for each inlined module and do the great 
    # module name swap-a-rama so the script uses the inlined modules.
    # For the inlined modules, any 'use' statement will now cause a
    # 'Cant locate' perl error at compile time so we also ditch all
    # the problematic use statements.

    my $rh_renames = $self->collection->new_module_names;
    foreach my $oldname (sort keys %{$rh_renames}) {
        my $newname = $rh_renames->{$oldname};
        qlogprint( "Substituting $newname for $oldname\n" ) if $self->verbose;
        $newtext =~ s/$oldname/$newname/g;
        # Drop superfluous 'use' and 'require' statements
        # - this catches: use X::Y qw( ... ); 
        $newtext =~ s/^\s*use\s+$newname\s+qw\(.*?\);\s*$//smg;
        # - this catches: use X::Y; 
        $newtext =~ s/^\s*use\s+$newname.*?$//mg;
        # - this catches: require X::Y; 
        $newtext =~ s/^\s*require\s+$newname.*?$//mg;
    }

    # We need to add the Manifest after the name swap-a-rama otherwise
    # the original module names all get sub'd out.
    my $manifest = $self->collection->manifest;
    $newtext =~ s/^__QPP_MANIFEST__$/$manifest\n/m;

    $self->{newtext} = $newtext;
}


# Recursive parser

sub _find_and_parse_modules {
    my $self   = shift;
    my $module = shift;

    # Exit if we've already successfully found this module
    qlogprint( "entering _find_and_parse_modules( $module )\n" )
        if ($self->verbose > 1);
    if ( defined $self->collection->pcf_by_class( $module ) ) {
        qlogprint( "  skipping $module - already found\n" ) 
            if ($self->verbose > 1);
        return;
    };

    # Search the libs until we find a matching .pm then parse it
    qlogprint( "  looking for module $module\n" ) if ($self->verbose > 1);
    my $pathname = $module . '.pm';
    $pathname =~ s/::/\//g;  # create Unix pathname

    my @searches = ();
    foreach my $lib (@{$self->libs}) {
        my $searchname = $lib . '/' . $pathname;
        push @searches, $searchname;
        qlogprint( "    looking for module $module as $searchname\n" )
            if ($self->verbose > 1);
        if (-r $searchname) {
            qlogprint( "Found module $module as $searchname\n" ) 
                if $self->verbose;
            my $pcf = QCMG::Package::PerlCodeFile->new( file  => $searchname );
            push @{ $pcf->searches }, @searches;
            $self->collection->add_pcf( $pcf );

            # Go through all the patterns looking for new submodules
            my @matches = ();
            foreach my $pattern (@{ $self->patterns }) {
                my @newmods = $pcf->matching_requires( $pattern );
                push @matches, @newmods;
            }

            foreach my $module (@matches) {
                $self->_find_and_parse_modules( $module );
            }
            last;  # drop out of lib loop once we have a hit
        }
    }
}

1;

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
