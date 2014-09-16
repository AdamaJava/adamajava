package QCMG::HTML::TabbedPage;

##############################################################################
#
#  Module:   QCMG::HTML:TabbedPage.pm
#  Author:   John V Pearson
#  Created:  2011-09-08
#
#  Helps to abstract away lots of the drama involved in creating a
#  tabbed page.
#
#  $Id: TabbedPage.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Data::Dumper;
use Pod::Usage;
use QCMG::HTML::Tab;
use Carp qw( carp croak );

use vars qw( $SVNID $REVISION  );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: TabbedPage.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { title   => $params{title} || 'QCMG TabbedPage',
                 headers => $params{headers} || '',
                 content => $params{content} || '',
                 tabs    => [],
                 meta    => {},
                 google  => $params{google} || 0,
                 verbose => $params{verbose} || 0 };
    bless $self, $class;
}


sub title {
    my $self = shift;
    return $self->{title} = shift if @_;
    return $self->{title};
}


sub headers {
    my $self = shift;
    return $self->{headers} = shift if @_;
    return $self->{headers};
}


sub content {
    my $self = shift;
    return $self->{content} = shift if @_;
    return $self->{content};
}


sub tabs {
    my $self = shift;
    return $self->{tabs} = shift if @_;
    return $self->{tabs};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub use_google_charts {
    my $self = shift;
    return $self->{google} = shift if @_;
    return $self->{google};
}


sub add_content {
    my $self = shift;
    my $html = shift;

    $self->content( $self->content . $html );
}


sub add_headers {
    my $self = shift;
    my $html = shift;

    $self->headers( $self->headers . $html );
}


sub add_meta {
    my $self    = shift;
    my $name    = shift;
    my $content = shift;

    $self->{meta}->{$name} = $content;
}


sub add_subtab {
    my $self = shift;
    my $tab  = shift;

    die "add_subtab() must be passed a QCMG::HTML::Tab object"
        unless ref( $tab ) eq 'QCMG::HTML::Tab';

    push @{ $self->tabs }, $tab;
}


sub as_html {
    my $self = shift;

    my $html = $self->html_header . $self->content;

    # Build the UL that lists the available tabs
    my @tabs   = @{ $self->tabs };
    if (scalar @tabs) {
        $html .= '<ul class="tabs">' . "\n";
        foreach my $tab (@tabs) {
            $html .= '<li><a href="#">' . $tab->id . "</a></li>\n";
        }
        $html .= '</ul>'. "\n\n";
    }

    if (scalar @tabs) {
        foreach my $tab (@tabs) {
            $html .= $tab->as_html;
        }
    }

    $html .= $self->html_footer;

    return $html;
}


# For some reason which I haven't worked out yet, if the block of Google
# javascript script refs are after the block of tabbing scripts and
# styles then the tabbing doesn't work.  Go figure.

sub html_header {
    my $self = shift;
    my $header = <<E_O_HEADER;
<HTML>
<HEAD>
<TITLE>TTTT</TITLE>

<link rel="stylesheet" href="http://grimmond.imb.uq.edu.au/styles/qcmg.css" type="text/css" />

<!-- From Ollie's qProfiler implementation -->
<style> 
.header {font-family: Verdana, Helvetica, Arial;color: rgb(0,66,174);background: rgb(234,242,255);font-size: 15px;}
.desc{padding: 5px 10px;font-family: Verdana, Helvetica, Arial; font-size:12px}
.butt{font-family: Verdana, Helvetica, Arial; font-size:12px}
</style>

<!-- Tabbing system -->
<script src="http://cdn.jquerytools.org/1.2.5/full/jquery.tools.min.js"></script>
<style> 
ul.tabs             { margin:0 !important;  padding:0;  height:30px; border-bottom:1px solid #666;}
ul.tabs li          { float:left;  text-indent:0;  padding:0;  margin:0 !important;  list-style-image:none !important;}
ul.tabs a           { float:left;  font-family: Verdana, Helvetica, Arial; font-size:12px; display:block; padding:5px 30px;
                      text-decoration:none;  border:1px solid #666; border-bottom:0px; height:18px; background-color:rgb(234,242,255);
                      color:rgb(0,66,174); margin-right:2px; position:relative; top:1px; outline:0;  -moz-border-radius:4px 4px 0 0;}
ul.tabs a:active     { background-color:#ddd;  border-bottom:1px solid #ddd;  color:#CCCCCC;  cursor:default;}
ul.tabs a:hover      { background-position: -420px -31px; background-color:#CCFFFF;  color:#333; }
ul.tabs a.current, ul.tabs a.current:hover, ul.tabs li.current a { background-position: -420px -62px; cursor:default !important;
                       font-weight:bold;    color:#000066 !important;}
ul.tabs a.s          { background-position: -553px 0; width:81px; }
ul.tabs a.s:hover    { background-position: -553px -31px; }
ul.tabs a.s.current  { background-position: -553px -62px; }
ul.tabs a.l          { background-position: -248px -0px; width:174px; }
ul.tabs a.l:hover    { background-position: -248px -31px; }
ul.tabs a.l.current  { background-position: -248px -62px; }
ul.tabs a.xl         { background-position: 0 -0px; width:248px; }
ul.tabs a.xl:hover   { background-position: 0 -31px; }
ul.tabs a.xl.current { background-position: 0 -62px; }.panes .pane {
display:none;}
</style>

<!-- GOOGLE -->

<!-- XTRA -->

<script type="text/javascript">
function setupDoc() {
    <!-- This JavaScript snippet activates the tabs -->
    jQuery("ul.tabs").tabs("> .pane");
};             
</script>      
</HEAD>
<BODY onload="setupDoc();">
E_O_HEADER

    # If we're using Google Charts API we need some more javascript
    if ($self->use_google_charts) {
        my $google_js = "<!-- Google Chart -->\n<script type=\"text/javascript\" src=\"http://www.google.com/jsapi\"></script>";
        $header =~ s/<!-- GOOGLE -->/$google_js/;
    }

    # Sub in title, could be user-supplied or default
    my $title = $self->title;
    $header =~ s/TTTT/$title/;
    
    # If the user supplied extra header html, use it
    if ($self->headers) {
        my $extra_headers = $self->headers;
        $header =~ s/<!-- XTRA -->/$extra_headers/;
    }

    # If the user supplied meta tags, insert them directly after the
    # TITLE element
    my @meta_keys = sort keys %{ $self->{meta} };
    if (scalar @meta_keys) {
        my $meta_headers = "</TITLE>\n\n";
        foreach my $key (@meta_keys) {
            $meta_headers .= '<meta name="'. $key .'" content="'.
                             $self->{meta}->{$key} ."\" >\n";
        }
        $header =~ s/<\/TITLE>/$meta_headers/;
    }

    return $header;
}


sub html_footer {
    my $self = shift;

    # If we are not using google charts, we still need an "onload" call
    # to activate the tabs but it's placed in the header <body> element.
    # If we are using Google Charts API we return our special footer.

    my $footer = <<E_O_FOOTER;
</BODY>
</HTML>
E_O_FOOTER

#    my $google_footer = <<E_O_GOOGLE;
#<script type="text/javascript">
#google.setOnLoadCallback( function() {
#    <!-- This JavaScript snippet activates the tabs -->
#    jQuery("ul.tabs").tabs("> .pane");
#});             
#</script>      
#</BODY>
#</HTML>
#E_O_GOOGLE

#    if ($self->use_google_charts) {
#        return $google_footer;
#    }

    return $footer;
}


1;
__END__

=head1 NAME

QCMG::HTML::TabbedPage - Perl module for creating Tabbed HTML pages


=head1 SYNOPSIS

 use QCMG::HTML::TabbedPage;
 my $page = QCMG::HTML::TabbedPage->new( title => 'My Report' );
 my $tab = QCMG::HTML::Tab->new( id => 'MyTab' );
 $page->add_subtab( $tab );


=head1 DESCRIPTION

This class is a data container for a tab to be included in a
QCMG::HTML::TabbedPage and has no real use outside that context.


=head1 PUBLIC METHODS

=over

=item B<new()>

=item B<id()>

=item B<content()>

=item B<tabs()>

=item B<verbose()>

=item B<add_content()>

=item B<add_headers()>

=item B<add_subtab()>

=item B<as_html()>

=back 


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: TabbedPage.pm 4663 2014-07-24 06:39:00Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014

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
