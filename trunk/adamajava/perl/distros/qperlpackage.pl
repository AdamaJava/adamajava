#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qperlpackage.pl
#  Author:   John V Pearson
#  Created:  2011-09-02
#
#  This script takes a perl script, looks for all of the required modules
#  that match specified pattern(s) and "inlines" those modules so that
#  their code in directly incserted into the script.  This allows for
#  distribution of a script without havng to also distribute copies of
#  non-CPAN modules that it depends on.
#
#  $Id: qperlpackage.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;

use QCMG::Package::ParseScript;
use QCMG::Util::QLog;

use vars qw( $REVISION $SVNID $CMDLINE );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qperlpackage.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|OPTIONS'
               ) unless (scalar @ARGV > 0);

    $CMDLINE = join(' ',@ARGV);

    # Setup defaults for important variables.

    my %params = ( input          => '',
                   output         => 'a.pl',
                   patterns       => [],
                   libs           => [],
                   version        => 0,
                   verbose        => 0,
                   help           => 0,
                   man            => 0 );

    # Use GetOptions module to parse commandline options
    my $results = GetOptions (
           'i|input=s'            => \$params{input},         # -i
           'o|output=s'           => \$params{output},        # -o
           'p|pattern=s'          =>  $params{patterns},      # -p
           'l|lib=s'              =>  $params{libs},          # -l
           'v|verbose+'           => \$params{verbose},       # -v
           'version!'             => \$params{version},       # --version
           'h|help|?'             => \$params{help},          # -?
           'man|m'                => \$params{man}            # -m
           );

    # --help, --man, --version
    pod2usage(1) if $params{help};
    pod2usage(-exitstatus => 0, -verbose => 2) if $params{man};
    if ($params{version}) {
        print "$SVNID\n";
        exit;
    }

    unless ($params{input}) {
        pod2usage(-exitstatus => 'NOEXIT', -verbose => 1);
        die "\nFATAL: the input script must be specified (-i)\n";
    }

    # Allow for ,-separated lists of patterns
    my @patterns = map { split /\,/,$_ } @{ $params{patterns} };
    unless (scalar(@patterns)) {
        pod2usage(-exitstatus => 'NOEXIT', -verbose => 1);
        die "\nFATAL: at least one pattern must be specified (-p)\n";
    }
    $params{patterns} = \@patterns;

    # Allow for ,-separated lists of libraries and ensure that all libs
    # from from @INC are added to the search space
    my @libs = map { split /\,/,$_ } @{ $params{libs} };
    push @libs, @INC;
    $params{libs} = \@libs;

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    my $script = QCMG::Package::ParseScript->new(
                      file     => $params{input},
                      libs     => $params{libs},
                      patterns => $params{patterns},
                      verbose  => $params{verbose} );
    $script->inline_modules;
    $script->write( $params{output} );

    qlogend();
}


__END__

=head1 NAME

qperlpackage.pl - Package a perl script with inlined QCMG modules


=head1 SYNOPSIS

 qperlpackage.pl [options]


=head1 ABSTRACT

Distributing a perl script is always a little less than convenient if
the script relies on inhouse non-CPAN modules as they must be
distributed with the script and installed by the end-user.  For
distribution purposes, it would be much more convenient if the script
were a single monolithic block of code that contained all of the
required local modules inlined.  This script makes that possible.

=head1 OPTIONS

 -i | --input         perl script to be distributed
 -o | --output        name of transformed script; default=a.pl
 -l | --lib           library directory to be searched for modules
 -p | --pattern       regex pattern to identify modules to be inlined
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

This script is designed for the default case which is pure-perl modules
without C inclusions and with one class per file.  Perl modules that
rely on C code carry that code with them and compile it as needed and
this script just isn't ever going to be able to cleanly carry around
that C code so if your module has C code then this script will almost
certainly not work for you.

The script to be inlined must contain a B<__END__> token so that there
is a safe place to insert all of the code from the inlined modules.

This script works by parsing the script specified with B<--input> and
looking for any B<use> or B<require> statements that refer to modules
that match any of the patterns specified with B<--pattern>.  If any
matching modules are found, the text of those modules will be parsed to
find more matching modules.  This process continues recursively
loading and parsing modules until no new matching modules are found.  It
then strips out any POD from the modules to be inlined and creates a new
local name for each module.  The code from all of the modules is
inserted into the original script immediately before the B<__END__>
token along with a B<qperlpackage manifest> comment block that lists all
of the files parsed and inlined and the old and new names of
all of the inlined modules.

The best way to get a feel for how qperlpackage works is to look at the
code of qperlpackage itself.  qperlpackage relies on 3 non-CPAN modules
so it is distributed as a qperlpackage processed script.  For example,
the qperlpackage manifest comment block for qperlpackage will look
something like:

 ###########################################################################
 #
 #  qperlpackage manifest
 #
 #  Inlined from: /panfs/home/jpearson/Devel/QCMGPerl/lib/QCMG/Package/ParseScript.pm
 #
 #   Original Module Name: QCMG::Package::ParseScript
 #   Inline Module Name:   QPP_QCMG_Package_ParseScript
 #
 #  Inlined from: /panfs/home/jpearson/Devel/QCMGPerl/lib/QCMG/Package/Collection.pm
 #
 #   Original Module Name: QCMG::Package::Collection
 #   Inline Module Name:   QPP_QCMG_Package_Collection
 #
 #  Inlined from: /panfs/home/jpearson/Devel/QCMGPerl/lib/QCMG/Package/PerlCodeFile.pm
 #
 #   Original Module Name: QCMG::Package::PerlCodeFile
 #   Inline Module Name:   QPP_QCMG_Package_PerlCodeFile
 #
 ###########################################################################



=head2 Commandline Options

=over

=item B<-i | --input>

Script to be transformed.

=item B<-o | --output>

Name to be used for transformed script.  Defaults to I<a.pl> (after the
C I<a.out>).

=item B<-l | --lib>

By default, qperlpackage will search all directory trees in @INC when
trying to load a module for inline'ing.  If you add extra directories
with B<--lib> then they are prepended to the directory list so they are
searched first.  This enables you to inline local development copies of
modules that are also installed in the central perl module repository.
You can specify as many extra library directories as you wish.  If you
are setting B<$PERL5LIB> to access local modules then you may find that
you do not need to specify any library directories in order to find
local modules to be inlined.

=item B<-p | --pattern>

This is the pattern for modules to be inlined.  You B<do not> want to be
inlining any modules from the default perl install so only modules that
match a pattern will be inlined.  You must specify at least one pattern
but you can specify as many more than one as you wish.  If a module
matches any of the patterns it will be inlined so keep that in mind -
it's easy to get excited and start to inline a whole heap of modules you
should bot be inlining.  Also note that the pattern is matched against
the perlish module names, not the filesystem names so B<QCMG::Package>
not B<QCMG/Package.pm>.  It's easy to catch a whole tree of modules by
specifying a pattern like B<QCMG::>.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will transform the qperlpackage.pl script
itself:

  qperlpackage.pl -i qperlpackage.pl -p 'QCMG::' \
      -o /usr/local/bin/qperlpackage.pl

B<N.B.> The spaces between the options (B<-i>, B<-l> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qperlpackage.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
