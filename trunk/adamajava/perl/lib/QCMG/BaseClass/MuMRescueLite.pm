package QCMG::BaseClass::MuMRescueLite;

=head1 NAME

QCMG::BaseClass::MuMRescueLite

=head1 SYNOPSIS

  use QCMG::BaseClass::MuMRescueLite;
  my $obj = QCMG::BaseClass::MuMRescutLite->new(%argv);
  $obj->main;
  $obj->preRescue;
  $obj->Rescue;
  $obj->afRescue;

=head1 DESCRIPTION

This module calls the MuMRescueLite.py program. MuMRescueLite.py uses a statical method to
choose more likely mapping positions for a multimapping read.

=head2 Methods

=over 4

=cut

use Object::InsideOut;
use strict;
use Parallel::ForkManager;

use constant {num_parallel => 4};

# Attr:
#<<<
# parameters for whole mapping 
my @output_dir :Field :Arg('Name' => 'output_dir','Mandatory' => 1, 'Regexp' => qr/^output[_]{1,2}dir$/i);
my @exp_name :Field :Arg('Name' => 'exp_name','Mandatory' => 1, 'Regexp' => qr/^exp_n[am]{1,2}e$/i);
my @max_mismatch :Field :Arg('Name' => 'max_mismatch', 'Mandatory' => 1);
my @chr_names :Field :Arg('Name' => 'chr_names', 'Mandatory' => 1);
my @rescue_program :Field :Arg('Name' => 'rescue_program', 'Mandatory' => 1);
my @rescue_window :Field :Arg('Name' => 'rescue_window', 'Mandatory' => 1);
my @objTool :Field :Arg('Name' => 'objTool', 'Mandatory' => 1,  'Regexp' => qr/^objtool/i);
my @max_hits :Field :Arg('Name' => 'max_hits', 'Mandatory' => 1, 'Regexp' => qr/^max_hits$/i );
my @fPos_forWig :Field :Get(out_Positive);
my @fNeg_forWig :Field :Get(out_Negative);
#>>>

# Methods:
sub _init : Init {
    my ($self, $arg) = @_;

    # report all final output file name
    my $chrs = $chr_names[$$self];
    for my $chr (@$chrs) {
        my $output = $output_dir[$$self] . "$chr." . $exp_name[$$self] . ".for_wig.positive";
        push @{$fPos_forWig[$$self]}, $output;
        $output = $output_dir[$$self] . "$chr." . $exp_name[$$self] . ".for_wig.negative";
        push @{$fNeg_forWig[$$self]}, $output;
    }

}


=item * main(\@listOfCollatedFiles);

There are three main step in this function. First, this module reads the collated file and convert the format; 
Second,it pass new formated file to MuMRescueLite.py which will run on queuing system, it will wait here until 
the rescue program is finished. Finally, it classify the rescue output by mapping strand and chromosome name; 
and convert the output format to suit wigplot module.

=cut

sub main {
    my ($self, $ref_collated) = @_;
    $objTool[$$self]->Log_PROCESS("Running MuMRescueLite ...");

    # reporting all multimpping position with lowest mismatch value using MuMRescueLite input formart
    my $f_forRescue = $output_dir[$$self] . $exp_name[$$self] . ".geno.for_rescue";
    &preRescue($self, $ref_collated, $f_forRescue);

    # run MuMRESCUELite
    my $f_rescued = "$f_forRescue.weighted";
    &Rescue($self, $f_forRescue, $f_rescued);

    # classify output from MuMRESCUELite by chromosome - for wigplot.
    &afRescue($self, $f_rescued);
    $objTool[$$self]->Log_SUCCESS("Finished MuMRescueLite!");
}


=item * preRescue(\@listOfCollatedFiles, $fileNameForTagsToRescue)

This method reads the collated files, and converts them to a format suitable for
the MumRescueLite.py program (written to $fileNameForTagsToRescue)

=cut

sub preRescue {

    my ($self, $inRef, $output) = @_;
    open(RESCUED, ">$output")
      or $objTool[$$self]->Log_DIED("can't create $output for sub preRescue \n ");

    foreach my $f_collated (@$inRef) {
        open(IN, $f_collated)
          or $objTool[$$self]->Log_DIED("Can't open collated file -- $f_collated in sub preRescue");
        while (my $line = <IN>) {

            # only read read tag id line
            if ($line !~ /^>/) { next }
            chomp($line);
            my @matches = split(/\t/, $line);
            my $tagid   = shift(@matches);
            my $total   = shift(@matches);

            #  throw this non mapped or over mapped tag
            if (($total == 0)||($total >= $max_hits[$$self])) { next }

            #  initialize this tag's mismatch
            my %mis = ();
            for (my $i = 0 ; $i <= $max_mismatch[$$self] ; $i++) { $mis{$i} = 0 }

            #  count this tag matched times at each mismatch value
            foreach my $p (@matches) {
                my ($chr, $position, $mismatch) = split(/\./, $p);
                $mis{$mismatch}++;
            }

            #  get the matched position with lowest mismatch value, throw other matched postion
            my $s_mismatch = -1;
            for (my $i = 0 ; $i <= $max_mismatch[$$self] ; $i++) {
                if ($mis{$i} > 0) { $s_mismatch = $i; last }
            }
            if ($s_mismatch == -1) {
                $objTool[$$self]->Log_DIED("error on <COLLATED> ($line) \n");
                last;
            }

            # report the selected position to
            my $seq = <IN>;
            chomp($seq);
            my $mers = length($seq);

            foreach my $m (@matches) {
                my ($chr, $position, $mismatch) = split(/\./, $m);
                if ($mismatch != $s_mismatch) { next }
                if ($position < 0) {
                    my $end   = abs($position);
                    my $start = $end - $mers + 1;
                    print RESCUED "$tagid\t$mis{$s_mismatch}\t$chr\t-\t$start\t$end\t1\n";
                }
                else {
                    my $start = $position;
                    my $end   = $start + $mers - 1;
                    print RESCUED "$tagid\t$mis{$s_mismatch}\t$chr\t+\t$start\t$end\t1\n";
                }
            }
        }
        
        close(IN);
    }
    
    close(RESCUED);
}


=item * Rescue($fileOfTagsToRescue, $fileNameForRescuedTags)

Run the MumRescueLite.py program, and wait for it to finish.  Results are
written to $fileNameForRescuedTags.

=cut

sub Rescue {

    my ($self, $inMuM, $outMuM) = @_;

    # Create shell script to run MuMRescueLite
    my $fSH    = $output_dir[$$self] . "RunLite.sh";
    my $fOK    = $output_dir[$$self] . "RunLite.ok";
    my $ferr   = $output_dir[$$self] . "RunLite.err";
    my $fout   = $output_dir[$$self] . "RunLite.out";
    my $fid    = $output_dir[$$self] . "RunLite.id";
    my $ftrace = $output_dir[$$self] . "traceJob.txt";

    open(SH, ">$fSH")
      or $objTool[$$self]->Log_DIED("can't create shell script file for running MuMRescueLite");
    my $comm =
      "python -i " . $rescue_program[$$self] . "  $inMuM $outMuM  " . $rescue_window[$$self];
    print SH $comm;

    # when MuMRescueLite compelete successfule, it will print 0 to $fOK
    print SH "\necho \$\? \> $fOK";
    close(SH);

    # debug
    my $rc = system("sh $fSH");

    # wait job to be compelete on the queuing system
    $comm = "qsub -l walltime=12:00:00 -o $fout -e $ferr $fSH > $fid";

    my %q_file = ($fOK => $rc);
    $objTool[$$self]->wait_for_queue(\%q_file);

    unlink($fSH);
    unlink($fOK);
    unlink($ferr);
    unlink($fout);
    unlink($fid);
}


=item * afRescue($fileContainingRescuedTags)

Read the file containing the rescued tags, parse these and add them to the mapping
output.

=cut

sub afRescue {

    my ($self, $f_rescued) = @_;

    my $pm          = new Parallel::ForkManager(5);
    my $chromosomes = $chr_names[$$self];

    # parallel to prepare pre wig files
    foreach my $chr (@$chromosomes) {
        $pm->start and next;
        open(IN, $f_rescued) or $objTool[$$self]->Log_DIED(" can't open file $f_rescued\n");
        my $fp = $output_dir[$$self] . "$chr." . $exp_name[$$self] . ".for_wig.positive";
        open(POS, ">$fp") or $objTool[$$self]->Log_DIED(" can't open file $fp\n");
        my $fn = $output_dir[$$self] . "$chr." . $exp_name[$$self] . ".for_wig.negative";
        open(NEG, ">$fn") or $objTool[$$self]->Log_DIED(" can't open file $fn\n");
        
        while (<IN>) {
            chomp();
            my ($id, $numPos, $chr_rescue, $strand, $start, $end, $freq, $score) = split(/\t/);
            if ($chr eq $chr_rescue) {
                if    (($strand eq "+") && ($score > 0)) { print POS "$start\t$end\t$score\n" }
                elsif (($strand eq "-") && ($score > 0)) { print NEG "$start\t$end\t$score\n" }
            }
        }
        close(IN);
        close(NEG);
        close(POS);

        # sort output file by start position
        my $f_tmp = $output_dir[$$self] . "$chr.forWig.tmp";
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
1;
__END__

=back

=head1 AUTHOR

qinying Xu (Christina) (q.xu@imb.uq.edu.au)

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
