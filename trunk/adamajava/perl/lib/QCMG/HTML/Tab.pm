package QCMG::HTML::Tab;

##############################################################################
#
#  Module:   QCMG::HTML:Tab.pm
#  Author:   John V Pearson
#  Created:  2011-09-08
#
#  Data container for HTML Tabs
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak );
use Data::Dumper;
use Pod::Usage;
use QCMG::Util::QLog;
use vars qw( $SVNID $REVISION  );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { id          => $params{id} || '',
                 content     => $params{content} || '',
                 subtabs     => [],
                 verbose     => $params{verbose} || 0 };
    bless $self, $class;
}


sub id {
    my $self = shift;
    return $self->{id} = shift if @_;
    return $self->{id};
}


sub content {
    my $self = shift;
    return $self->{content} = shift if @_;
    return $self->{content};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub subtabs {
    my $self = shift;
    return $self->{subtabs} = shift if @_;
    return $self->{subtabs};
}


sub add_content {
    my $self = shift;
    my @html = @_;

    $self->content( $self->content . join('',@html) );
}


sub add_subtab {
    my $self = shift;
    my $tab  = shift;

    die "add_subtab() must be passed a QCMG::HTML::Tab object"
        unless ref( $tab ) eq 'QCMG::HTML::Tab';

    push @{ $self->subtabs }, $tab;
}


sub as_html {
    my $self = shift;

    qlogprint( "Converting tab ". $self->id ." to HTML\n" ) if $self->verbose;

    my $html = '<div class="pane" id="' . $self->id .'">' ."\n";
    my @tabs = @{ $self->subtabs };

    # Build the UL that lists the available tabs
    if (scalar @tabs) {
        $html .= '<ul class="tabs">' . "\n";
        foreach my $tab (@tabs) {
            $html .= '<li><a href="#">' . $tab->id . "</a></li>\n";
        }
        $html .= '</ul>'.  "\n\n";
    }

    $html .= $self->content;

    # Capture HTML for any subtabs (triggers recursion)
    if (scalar @tabs) {
        foreach my $tab (@tabs) {
            $html .= $tab->as_html;
        }
    }

    $html .= "</div>\n\n";

    return $html;
}


1;
__END__

=head1 NAME

QCMG::HTML::Tab - Perl module for a Tab in a TabbedPage


=head1 SYNOPSIS

 use QCMG::HTML::TabbedPage;
 my $page = QCMG::HTML::TabbedPage->new();
 my $tab = QCMG::HTML::Tab->new( id => 'MyTab' );
 $page->add_tab( $tab );


=head1 DESCRIPTION

This class is a data container for a tab to be included in a
QCMG::HTML::TabbedPage and has no real use outside that context.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $tab = QCMG::HTML::Tab->new( id = 'MyTab' );

=item B<id()>

=item B<content()>

=item B<subtabs()>

=item B<verbose()>

=item B<add_content()>

=item B<add_subtab()>

=item B<as_html()>

 $tab->as_html();

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


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
