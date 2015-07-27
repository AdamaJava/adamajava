package QCMG::BaseClass::SingleSelect;

=head1 NAME

QCMG::BaseClass::SingleSelect

=head1 SYNOPSIS

  use QCMG::BaseClass::SingleSelect;
  my $obj = QCMG::BaseClass::SingleSelect->new(%argv);
  my $collated = ["/data/test.ma.35.3.0.collated", "/data/test.ma.30.3.0.collated"];
  $obj->SingleSelect($collated);
  $obj->afSelect;
  $obj->equalSim;
  $obj->strandSim;

=head1 DESCRIPTION

This module codes methods to select single mapping tags, and tags based on certain criteria, such as expect strand,
Number of mismatches. Note that only the tags mapping to the junction library
expected strand are now single selected.  This is because there is no true way to determine
the correct mapping position of a tag if it multimaps. In the case of junction tags,
we create a library where all junctions are ordered in the same sense.  Hence we know that
if a tag maps in the unexpected direction onto the junction library, and the library has been
created using a strand specific protocol, then it is probably a mapping error.  In such cases, the &strandSim
routine will preferentially choose a single mapping expect tag over one or more unexpect mapping tags and 
will also take into consideration the number of mismatches.

=head2 Methods

=over 4

=cut

use strict;
use Object::InsideOut;

use constant {TRUE => "true", FALSE => "false"};

# Attr:
#<<<
my @output_dir :Field :Arg('Name' => 'output_dir','Mandatory' => 1, 'Regexp' => qr/^output[_]{1,2}dir$/i);
my @exp_name :Field :Arg('Name' => 'exp_name','Mandatory' => 1, 'Regexp' => qr/^exp_n[am]{1,2}e$/i);
my @max_mismatch :Field :Arg('Name' => 'max_mismatch', 'Mandatory' => 1);
my @chr_names :Field :Arg('Name' => 'chr_names', 'Mandatory' => 1);
my @expect_strand :Field :Arg('Name' => 'expect_strand', 'Mandatory' => 1);
my @objTool :Field :Arg('Name' => 'objTool', 'Mandatory' => 1,  'Regexp' => qr/^objtool/i);
my @f_singles :Field :Get(f_singles); # an array of output files which reporting each single selected position
my @fPos_forWig :Field :Get(out_Positive);
my @fNeg_forWig :Field :Get(out_Negative);
#>>>

# methods
sub _init : Init {
    my ($self, $arg) = @_;

    foreach my $chr (@{$chr_names[$$self]}) {

        # create SiM file
        my $f = $output_dir[$$self] . "$chr." . $exp_name[$$self] . ".SIM";
        push @{$f_singles[$$self]}, $f;

        # convert the output formart for wigplot
        my $output = $output_dir[$$self] . "$chr." . $exp_name[$$self] . ".for_wig.positive";
        push @{$fPos_forWig[$$self]}, $output;
        $output = $output_dir[$$self] . "$chr." . $exp_name[$$self] . ".for_wig.negative";
        push @{$fNeg_forWig[$$self]}, $output;
    }
}

=item * SingleSelect;

This function read the collated file and outputs single mapping
tags.  If we are working with stranded mappings, it also calls strandSim 
on any multimapping tags.

=cut

sub SingleSelect {

    my ($self, $ref_collated) = @_;

    # create output files
    my %outs = ();
    foreach my $chr (@{$chr_names[$$self]}) {
        my $f = $output_dir[$$self] . "$chr." . $exp_name[$$self] . ".SIM";
        open(my $fh, ">$f")
          or $objTool[$$self]->Log_DIED("can't create output for single selection -- $f");
        $outs{$chr} = $fh;
    }
    foreach my $f (@$ref_collated) {
        open(IN, $f)
          or $objTool[$$self]->Log_DIED("can't open collated file: $f in Class SingleSelct");
        while (my $line = <IN>) {
            if ($line !~ m/^\>/) { next }
            chomp($line);
            my @matches  = split(/\t/, $line);
            my $id       = shift(@matches);
            my $freq     = shift(@matches);
            my $selected = FALSE;

            #  if unstranded data, do not select a single lowest mismatched mapping
            #  from a multiple mapping tag
            if ($expect_strand[$$self] eq "0") {
                if ($freq == 1) {
                    my ($chr, $pos, $mis) = split(/\./, $matches[0]);
                    my $seq = <IN>;
                    my $fh  = $outs{$chr};
                    print $fh "$id,$pos.$mis\n$seq";
                }
                next;
            }
            else {

                #  it is stranded data, so select a mapping based on the strand (if we can)
                $selected = &StrandSiM($self, \@matches, $expect_strand[$$self]);

                if ($selected eq FALSE) { next; }

                #  report the single selected position for stranded data
                my ($chr, $pos, $mis) = split(/\./, $selected);
                my $seq = <IN>;
                my $fh  = $outs{$chr};
                print $fh "$id,$pos.$mis\n$seq";
            }

        }
        close(IN);
    }

    foreach my $key (keys %outs) { close($outs{$key}) }

}


=item * afSelect;

This function classify the single file by strand and chromosome name, and the convert th format to suit to wiggle plot.

=cut

sub afSelect {

    my ($self) = @_;

    my $pm = new Parallel::ForkManager(5);

    # parallel to prepare pre wig files
    foreach my $f (@{$f_singles[$$self]}) {
        $pm->start and next;
        open(IN, $f) or $objTool[$$self]
          ->Log_DIED(" can't open file $f in sub QCMG::BaseClass::SingleSelect::afSelect\n");
        (my $fp = $f) =~ s/SIM/for_wig\.positive/;
        open(POS, ">$fp") or $objTool[$$self]
          ->Log_DIED(" can't open file $fp in sub QCMG::BaseClass::SingleSelect::afSelect\n");
        (my $fn = $f) =~ s/SIM/for_wig\.negative/;
        open(NEG, ">$fn") or $objTool[$$self]
          ->Log_DIED(" can't open file $fn in sub QCMG::BaseClass::SingleSelect::afSelect\n");

        while (<IN>) {
            if (!/^\>/) { next }
            chomp();
            my ($id, $map) = split(/\,/);
            my ($pos, $mis) = split(/\./, $map);
            my $seq = <IN>;
            chomp($seq);
            my $l = length($seq) - 1;

            if (($pos >= 0)) {
                my $start = $pos;
                my $end   = $start + $l - 1;
                print POS "$start\t$end\t1\n";
            }
            else { my $end = abs($pos); my $start = $end - $l + 1; print NEG "$start\t$end\t1\n" }
        }
        close(IN);
        close(NEG);
        close(POS);

        # sort output file by start position
        my $f_tmp = "$f.tme";
        system("sort -n -k1 $fp > $f_tmp");
        unlink($fp);
        rename($f_tmp, $fp);

        system("sort -n -k1 $fn > $f_tmp");
        unlink($fn);
        rename($f_tmp, $fn);

        $pm->finish;
    }

    # wait all pre_wig files are created
    $pm->wait_all_children;
}



=item * EqualSim(@matches)

depreciated

=cut

sub EqualSiM {
    my ($self, $matches) = @_;

    # get the multiposition with smallest mismatch value
    my $min_mis = $max_mismatch[$$self];
    my @selects = ();
    foreach my $match (@$matches) {
        my ($chr, $pos, $mis) = split(/\./, $match);
        if ($mis == $min_mis) { push @selects, $match }
        elsif ($mis < $min_mis) { @selects = (); push @selects, $match; $min_mis = $mis }
    }

    my $select = FALSE;
    if (scalar(@selects) == 1) { $select = $selects[0] }

    return $select;
}


=item * StrandSiM($matchesString, $expect_strand)

Choose a single mapping from a list of multimapping tags 
based on which strand we expect those tags to.

=cut

sub StrandSiM {

    my ($self, $matches, $expect_strand) = @_;

    # classify all mismathed position by strand, initial them refer to empty array
    my %exp_mis = ();
    my %non_mis = ();
    for (my $i = 0 ; $i <= $max_mismatch[$$self] ; $i++) { $exp_mis{$i} = []; $non_mis{$i} = [] }

    # get the multiposition with smallest mismatch value
    foreach my $match (@$matches) {
        my ($chr, $pos, $mis) = split(/\./, $match);
        if ((($pos >= 0) && ($expect_strand eq "+")) || (($pos < 0) && ($expect_strand eq "-"))) {
            push @{$exp_mis{$mis}}, $match;
        }
        else { push @{$non_mis{$mis}}, $match }
    }

    # search single mapping position from expect strand position
    my $select = FALSE;
    for (my $i = 0 ; $i <= $max_mismatch[$$self] ; $i++) {
        my $ref = $exp_mis{$i};
        if (scalar(@$ref) == 1) {
            $select = shift(@$ref);
            last;
        }
    }
    if ($select eq FALSE) {
        for (my $i = 0 ; $i <= $max_mismatch[$$self] ; $i++) {
            my $ref = $non_mis{$i};
            if (scalar(@$ref) == 1) { $select = $ref->[0]; last }
        }
    }

    return $select;
}

1;
__END__

=back

=head1 AUTHORS

=over 3

=item Qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=back

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
