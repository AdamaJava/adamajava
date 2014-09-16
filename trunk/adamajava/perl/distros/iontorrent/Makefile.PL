###########################################################################
#
#  Script:   Makefile.PL
#  Author:   John V Pearson
#  Created:  2011-02-02
#
#  This file uses ExtUtils::MakeMaker to create a sensible Makefile
#  for the ngstools project.  For more details on how to influence the
#  contents of the Makefile that is written, read the POD (perldoc) for 
#  ExtUtils::MakeMaker.
#
#  $Id: Makefile.PL 4668 2014-07-24 10:18:42Z j.pearson $
#
###########################################################################

use ExtUtils::MakeMaker;

display_license();

my $version = '$Revision: 4668 $ ' =~ /\$Revision:\s+([^\s]+)/;

WriteMakefile(
    NAME         => 'iontorrent',
    VERSION      => $version,
    ABSTRACT     => 'Off-instrument tools for Ion Torrent sequencers',
    AUTHOR       => 'QCMG Bioinformatics Group',
);

sub display_license {

    print <<_EOL_

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
=cut
_EOL_
}
