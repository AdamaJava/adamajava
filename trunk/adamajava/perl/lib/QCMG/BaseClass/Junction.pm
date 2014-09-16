package QCMG::BaseClass::Junction;

=head1 NAME

QCMG::BaseClass::Junction

=head1 SYNOPSIS

  use QCMG::BaseClass::Junction;
  my $obj = QCMG::BaseClass::Junction->new('conf' => $objConf);
  $obj->main($ref_junc_collated);
  $obj->CreateBed
  $obj->getJuncID
  $obj->SingleSelect


=head1 DESCRIPTION

This module reads all junction collated files, locates genomic positions for junction tag mappings,
creates BED files for junction tags, and single selects the mapping position with 
a specified strand and lower mismatch value.

=head2 Methods

=over

=cut

use strict;
use Object::InsideOut;
use File::Basename;
use File::Temp qw/tempfile/;
use QCMG::BaseClass::SingleSelect;
use QCMG::BaseClass::Tools;
use File::Basename;
use Carp;

# attr
# <<<
my @exp_name :Field :Arg('Name' => 'exp_name','Mandatory' => 1, 'Regexp' => qr/^exp_n[am]{1,2}e$/i);
my @output_dir :Field :Arg('Name' => 'output_dir','Mandatory' => 1, 'Regexp' => qr/^output[_]{1,2}dir$/i);
my @objTool :Field :Arg('Name' => 'objTool', 'Mandatory' => 1,  'Regexp' => qr/^objtool/i);
my @junc_index :Field :Arg('Name' => 'junc_index', 'Mandatory' => 1);
my @expect_strand :Field :Arg('Name' => 'expect_strand', 'Mandatory' => 1);
my @max_mismatch :Field :Arg('Name' => 'max_mismatch', 'Mandatory' => 1);

my @output :Field :Get(output);

use constant { Num_PARA => 4, TRUE => 'true', FALSE => 'false' };
# >>>

# methods
sub main {
    my ($self, $ref_collated) = @_;

    # check file number of both inputs and index
    if (scalar(@{$junc_index[$$self]}) != scalar(@$ref_collated)) {
        $objTool[$$self]->Log_DIED("odd  number between junction collated file and index");
    }

    my $pm = new Parallel::ForkManager(Num_PARA);
    $objTool[$$self]->Log_PROCESS("Collecting Junction mapping data and creating BED file");

    for (my $i = 0 ; $i < scalar(@$ref_collated) ; $i++) {

        # we can't parallel singleSelect since the module produce same output name if we use similar index file.
        # eg both a.fa.index and a.b.fa.index produce a.exp_name.SIM
        # but we can rename it
        my $f_single = &SingleSelect($self, $ref_collated->[$i], $junc_index[$$self]->[$i],
            $expect_strand[$$self]);
        rename($f_single, $ref_collated->[$i] . ".SIM");
        $f_single = $ref_collated->[$i] . ".SIM";
        $pm->start and next;
        my $fID = &getJuncID($self, $f_single, $junc_index[$$self]->[$i]);
        rename($fID, $ref_collated->[$i] . ".ID");
        $pm->finish;
    }
    $pm->wait_all_children;
    $objTool[$$self]->check_died;

    # BedFile name
    my @fIDs = ();
    foreach my $f (@$ref_collated) { push @fIDs, "$f.ID" }
    if ($expect_strand[$$self] eq "0") {
        my $output = $output_dir[$$self] . $exp_name[$$self] . ".junction.BED";
        &CreateBed($self, \@fIDs, $output, $expect_strand[$$self]);
    }
    else {
        my $output = $output_dir[$$self] . $exp_name[$$self] . ".expect.junc.BED";

        &CreateBed($self, \@fIDs, $output, $expect_strand[$$self]);

        # create unexpect strand tag BED file
        $output = $output_dir[$$self] . $exp_name[$$self] . ".unexpect.junc.BED";
        if   ($expect_strand[$$self] eq "+") { &CreateBed($self, \@fIDs, $output, "-") }
        else                                 { &CreateBed($self, \@fIDs, $output, "+") }
    }

    foreach my $f (@fIDs) { unlink($f) }

    $objTool[$$self]->Log_SUCCESS("Created junction BED file");
}


=item * CreateBed(\@fileIDs, $bedFileName, $expectStrand)

Create a BED file from the mapped junction information.

=cut

sub CreateBed {
    my ($self, $fIDs, $output, $SearchStrand) = @_;

    my %hits = ();

    foreach my $in (@$fIDs) {
        open(ID, $in)
          or $objTool[$$self]->Log_DIED("can't open input file $in in sub RNAJunction::CreateBed");
        while (<ID>) {
            chomp();
            my ($tagid, $tag_start, $tag_end, $strand, $mismatch, $juncid, $j_start, $j_end) = split(/\t/);
            if    ($SearchStrand eq "0")     { $hits{$juncid}++ }
            elsif ($SearchStrand eq $strand) { $hits{$juncid}++ }
        }
    }

    # report to output
    open(BED, ">$output")
      or $objTool[$$self]->Log_DIED("can't create BED file $output in sub RNAJunction::CreateBeD");

    # add head line here
    my $name = $exp_name[$$self];
    print BED
      "track nema=\"[$name] junctions\" description=\"[$name] SiM matches to 60mer informative junctions\" \n ";

    # write this junction id's information into BED file
    foreach my $id (keys %hits) {
        my @juncs  = split(/\_/, $id);
        my $strand = pop(@juncs);          # last element is strand
        my $end    = pop(@juncs);          # the second last element is junction end position
        my $start  = pop(@juncs);          # the third last lelement is junction start position
        my $chr    = join("_", @juncs);    # the rest are chromosome name
        $start -= 10;
        $end += 10;
        my $blockStart2 = $end - $start - 10;
        print BED
          "$chr\t$start\t$end\t$id\t$hits{$id}\t$strand\t$start\t$end\t0\t2\t10,10\t0,$blockStart2\n";
    }

    close(BED);

    return TRUE;
}


=item * getJuncID($singleSelectedJunctionFile, $junctionIndexFile)

Get the correct genomic location for this junction, ie, 
translate the mapping location of the junctino library to the
genomic location(s) for that junction.

=cut

sub getJuncID {

    my ($self, $f_single, $index) = @_;

    # convert input formart to "tagid start end strand mismatch"
    my ($hTMP, $fTMP) = tempfile("f_singleXXXXX", DIR => $output_dir[$$self]);
    open(IN, "$f_single")
      or
      $objTool[$$self]->Log_DIED("can't open the single selected file $f_single in sub getJunID");
    while (<IN>) {
        if (!/^\>/) { next }
        chomp();
        my ($id, $map) = split(/\,/);
        my ($pos, $mis) = split(/\./, $map);

        my $seq = <IN>;
        chomp($seq);
        my $l = length($seq);
        my ($start, $end, $strand) = (-1, -1, "0");
        if   ($pos < 0) { $end   = abs($pos); $start = $end - $l + 1;   $strand = "-" }
        else            { $start = $pos;      $end   = $start + $l - 1; $strand = "+" }

        print $hTMP "$id\t$start\t$end\t$strand\t$mis\n";
    }

    close(IN);
    close($hTMP);

    #  read all junction id into array
    open(INDEX, $index) or $objTool[$$self]->Log_DIED("can't open junction index file: $index\n");
    my @all_junc_id = ();
    while (<INDEX>) { chomp(); push @all_junc_id, [ split(/\t/) ] }
    close(INDEX);

    # get junction ID
    my $ff = "$f_single.ID";
    open(JUNC_ID, ">$ff")
      or $objTool[$$self]->Log_DIED("can't create file $ff to report the junction ID");

    # sort singel selected junction tap by start position
    system("sort -n -k2 $fTMP > $fTMP.sorted");
    open(SORTED, "$fTMP.sorted");
    my $i_search = 0;
    my $i_end    = scalar(@all_junc_id);
    while (my $line = <SORTED>) {
        chomp($line);
        my ($tagid, $tag_start, $tag_end, $strand, $mismatch) = split(/\t/, $line);

        # if we can't find junction id for this matched position,
        # this tag will be assign "...\tnoid\t0\t0\n"
        my $juncid  = "noid";
        my $j_start = 0;
        my $j_end   = 0;
        for (my $i = $i_search ; $i < $i_end ; $i++) {
            my ($id, $junc_start, $junc_end) =
              ($all_junc_id[$i]->[0], $all_junc_id[$i]->[1], $all_junc_id[$i]->[2]);

            # search next junction id
            if ($tag_start > $junc_end) { next }

            # check current junc id
            else {

                # found id, give right value to varible
                if (($tag_start >= $junc_start) && ($tag_end <= $junc_end)) {
                    $juncid  = $id;
                    $j_start = $junc_start;
                    $j_end   = $junc_end;
                }

                # stop search for current junction tag since it crossing two junction id or it start between two junctions -- "."
                # elsif( ($tag_start == ($junc_start - 1)) || ($tag_end > $junc_end) ){   $i_search = $i; last; }
                # stay at the current junction id for next tag on the sorted file
                $i_search = $i;
                last;
            }
        }

        # if we can't find junction id for this tag, the $i_search will start from last found junction id position
        print JUNC_ID
          "$tagid\t$tag_start\t$tag_end\t$strand\t$mismatch\t$juncid\t$j_start\t$j_end\n";
    }
    close(SORTED);
    close(JUNC_ID);
    unlink($fTMP);
    unlink("$fTMP.sorted");

    return $ff;

}


=item * SingleSelect($collatedFile, $junctionIndexFile, $expectStrand)

Choose a mapping for the junction based on the quality of mapping.  If a tag 
maps on the unexpect strand with many mismatches but single maps on the expect strand 
with zero mismatches then choose the unique mapping.

=cut

sub SingleSelect {
    my ($self, $f_collated, $index, $expect_strand) = @_;

    my $junc = fileparse($index, qr/\.[\w\.]+/);
    my @chrs = ();
    push @chrs, $junc;

    use QCMG::BaseClass::SingleSelect;

    my $objSingle = QCMG::BaseClass::SingleSelect->new(
        'output_dir'    => $output_dir[$$self],
        'exp_name'      => $exp_name[$$self],
        'chr_names'     => \@chrs,
        'max_mismatch'  => $max_mismatch[$$self],
        'objTool'       => $objTool[$$self],
        'expect_strand' => $expect_strand[$$self]);

    my @inputs = ();
    push @inputs, $f_collated;
    $objSingle->SingleSelect(\@inputs);
    my $ref_singles = $objSingle->f_singles;
    if (scalar(@$ref_singles) != 1) {
        $objTool[$$self]->Log_DIED(
            "There exists zero or more than one junction SIM files, see sub  RNAJunction::SingleSelect ");
    }

    # return the single selected file name, there is only one for junction
    return $ref_singles->[0];
}

1;
__END__

=back

=head1 AUTHORS

=over 3

=item Qinying Xu (Christina) (q.xu@imb.uq.edu.au)

=item David Wood (d.wood@imb.uq.edu.au)

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
