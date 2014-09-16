package QCMG::SeqResults::ReportMetrices;


###########################################################################
#
#  Module:   QCMG::SeqResults::ReportMetrices.pm
#  Creator:  Matthew J Anderson
#  Created:  2012-11-20
#
#  This Module is a collection of tools for retiving report metrix values.
#
#  $Id: ReportMetrices.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;							# Good practice
use warnings;						# Good practice

use Pod::Usage;
#use Data::Dumper;
use HTTP::Headers;                  # From CPAN
use HTML::HeadParser;               # From CPAN
use JSON qw( encode_json );         # From CPAN
use Exporter qw( import );

#use QCMG::SeqResults::Config;		# QCMG SeqResults module

use vars qw( $SVNID $REVISION @EXPORT_OK );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: ReportMetrices.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

@EXPORT_OK = qw( coverage_report_metrics );


################################################################################

=pod

=head1 METHODS

B<coverage_report_metrics()>

Extracts the metric values from the 'meta' tag in the header of a coverage 
html report.

Parameters:
 scalar: coverage_report		Path to coverage report html file.
 scalar: coverage_tags			Array of valid metric tags.

Returns:
 scalar: String in JSON format.

=cut

sub coverage_report_metrics {
	my $coverage_report	= shift;
	my $coverage_tags	= shift;
	
	my $html = "";
	#print "report_path $coverage_report\n";
	open FILE, $coverage_report	or die "$coverage_report - $!";
	while (<FILE>) {
	   $html .= $_;
	}
	close(FILE) or warn "Unable to close the file handle: $!";
	
    my $headers = HTTP::Headers->new;
    my $headparser = HTML::HeadParser->new($headers);
    $headparser->parse($html);

    my $coveage_values = {};
    foreach my $tag_type ( @{$coverage_tags} ){
        #print 'X-Meta-'.$tag_type."\n";
        if ( my $meta_tag = $headparser->header("X-Meta-${tag_type}") ) {
            #print $meta_tag."\n";
            $coveage_values->{$tag_type} = $meta_tag;
        }
    }
    
    if ($coveage_values){
        $coveage_values = encode_json $coveage_values;
    }else{
        $coveage_values = "";
    }
    return $coveage_values;
}

1;


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
