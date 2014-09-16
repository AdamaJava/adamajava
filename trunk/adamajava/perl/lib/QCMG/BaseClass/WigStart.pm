package QCMG::BaseClass::WigStart;

=head1 NAME

QCMG::BaseClass::WigStart

=head1 SYNOPSIS

  use QCMG::BaseClass::WigStart;
  my $obj = QCMG::BaseClass::WigStart->new;
  $obj->main($fileArray, "wiggle");

=head1 DESCRIPTION

A perl module which creates wiggle and start files

=head2 Methods

=over 2

=cut

use Object::InsideOut;
use strict;
use QCMG::BaseClass::Tools;
use File::Basename;

use Carp;

# attr
#<<<
my @exp_name :Field :Arg('Name' => 'exp_name','Mandatory' => 1, 'Regexp' => qr/^exp_n[am]{1,2}e$/i);
my @output_dir :Field :Arg('Name' => 'output_dir','Mandatory' => 1, 'Regexp' => qr/^output[_]{1,2}dir$/i);
my @objTool :Field :Arg('Name' => 'objTool', 'Mandatory' => 1,  'Regexp' => qr/^objtool/i);
my @strand :Field :Std(strand);
my @output :Field :Get(output);
use constant { Num_PARA => 4 };
#>>>

# methods
sub main {
    my ($self, $in_array, $flag, $name, $description) = @_;

    my $postfix;
    if    ($flag =~ /^wi[g]{1,2}[le]{0,2}$/i) { $postfix = "wiggle" }
    elsif ($flag =~ /^start[s]{0,2}$/i)       { $postfix = "start" }
    else {
        $objTool[$$self]->Log_DIED(
            "can't distinguish output file type -- $flag, only \"wiggle\" or \"start\" file can be accept here");
        return 0;
    }

    my $pm = new Parallel::ForkManager(Num_PARA);
    $objTool[$$self]->Log_PROCESS("creating $postfix plot");

    foreach my $f (@$in_array) {
        $pm->start and next;
        my $output = "$f.$postfix";
        my $mess;
        if ($postfix eq "start") { &create_start($self, $f, $output) }
        else                     { &create_wig($self, $f, $output) }
        $pm->finish;
    }

    $pm->wait_all_children;

    $objTool[$$self]->check_died;

    # join all paralled wig plot into one
    $output[$$self] = $output_dir[$$self] . $exp_name[$$self] . ".$postfix";

    open(OUT, ">$output[$$self]")
      or $objTool[$$self]->Log_DIED("can't create $postfix plot file: $output[$$self]");

    # add head line on final wiggle file
    print OUT
      "track type=bedGraph name=\"$name\" description=\"$description\"".
      " visibility=full color=255,0,0 altColor=0,100,200 priority=20 \n";

    foreach my $f (@$in_array) {
        my $in = "$f.$postfix";

        # get chromosome name
        if ($in !~ m/(chr[\w\d]{1,})\./g) {
            $objTool[$$self]->Log_DIED("can't find chromosome name from file: $in");
        }
        my $chr = $1;
        open(IN, $in) or
          $objTool[$$self]->Log_DIED("can't open file: $in for creating final $postfix plot file");
        while (<IN>) {
            chomp();
            my ($start, $end, $score) = split(/\t/);
            my $round = int($score + .5);
            if ($round > 0) { print OUT "$chr\t$start\t$end\t$round\n" }
        }
        close(IN);

    }
    close(OUT);

    return 1;
}


sub create_wig {

    # the input file named .for_wig must be sorted
    my ($self, $in_sort, $f_wig) = @_;

    my @StartEnd = ();
    &sort_StartEnd(\@StartEnd, $in_sort);

    open(my $fh_wig, ">$f_wig") or $objTool[$$self]->Log_DIED("can't open file $f_wig");
    open(SORTED,     $in_sort)  or $objTool[$$self]->Log_DIED("can't open file $in_sort");
    my $i_start      = shift(@StartEnd);
    my @next_start   = ();
    my %current_line = (start => 0, end => 0, scort => 0);
    foreach my $i_end (@StartEnd) {
        if ($i_end == $i_start) { next }

        my @current_start = @next_start;

        #  read a line from <SORTED>
        if (($current_line{'start'} == $i_start) || ($current_line{'start'} == 0)) {
            if ($current_line{'start'} != 0) { push(@current_start, {%current_line}) }

            #  %current_line = ();
            while (my $line = <SORTED>) {
                chomp($line);
                my ($start, $end, $score) = split(/\t/, $line);
                %current_line = (start => $start, end => $end, score => $score);

                # it belongs to next range
                if ($start > $i_start) { last }

                #  for current range [$i_start,$i_end)
                if ($start == $i_start) {
                    push(@current_start, {start => $start, end => $end, score => $score});
                }

                #  wrong range
                else {
                    $objTool[$$self]
                      ->Log_WARNING("$line is out of range ($i_start,$i_end)\nsee file $in_sort");
                }
            }
        }

        my $mess = &count_score($i_start, $i_end, \@current_start, \@next_start, $fh_wig);
        if ($mess ne "true") { $objTool[$$self]->Log_DIED($mess); last }
        $i_start = $i_end;
    }

    close(SORTED);
    close($fh_wig);

    return 1;
}

sub create_start {

    # the input file named .for_wig must be sorted
    my ($self, $in_sort, $f_start) = @_;

    # open both input and output file
    open(SORT,  $in_sort)    or $objTool[$$self]->Log_DIED("can't open file: $in_sort \n");
    open(START, ">$f_start") or $objTool[$$self]->Log_DIED("can't open file: $f_start \n");

    # if the input file exist and nonzero size
    if (-s $in_sort) {

        # read the first line from the input file
        my $line = <SORT>;
        chomp($line);
        my ($current_start, $current_end, $current_score) = split(/\t/, $line);

        # for following lines
        my $next_line;
        my $total_score = $current_score;
        while ($next_line = <SORT>) {
            chomp($next_line);
            my ($next_start, $next_end, $next_score) = split(/\t/, $next_line);
            if ($next_start == $current_start) {
                $total_score += $next_score;
            }
            else {
                my $end = $current_start + 1;
                print START "$current_start\t$end\t$total_score\n";
                $total_score   = $next_score;
                $current_start = $next_start;
            }
        }

        # print last start position into output file
        my $end = $current_start + 1;
        print START "$current_start\t$end\t$total_score\n";
    }

    close(SORT);
    close(START);
    return 1;
}

sub count_score {
    my ($i_start, $i_end, $current_start, $next_start, $fh) = @_;

    my $total = 0;
    @$next_start = ();

    my $flag = 1;
    foreach my $a (@$current_start) {
        if ($a->{'start'} < $i_start) {
            $flag = 0;
            print "$a->{'start'} : $i_start\n";
            last;
        }

        if ($a->{'end'} < $i_end) {
            $flag = 0;
            print "$a->{'end'} : $i_end \n";
            last;
        }
        if ($a->{'start'} > $i_start) { last }   
        $total += $a->{'score'};
        $a->{'start'} = $i_end;

        # filter out start == end posion
        if ($a->{'start'} < $a->{'end'}) { push(@$next_start, {%$a}) }
    }

    if ($flag == 0) {

        # print some markd
        foreach my $a (@$current_start) {
            print "flag=0: $a->{'start'}\t$a->{'end'}\t$a->{'score'} \n";
        }
        print $fh "$i_start\t$i_end\tERROR\n";
        return "error in sub QCMG::BaseClass::WigStart::count_score\n";
    }

    # write right value to output
    if ($total > 0) { print $fh "$i_start\t$i_end\t$total\n" }

    return "true";
}

sub sort_StartEnd {
    my ($array, $input_sort) = @_;

    open(SORTED, $input_sort) or die "can't open $input_sort\n";
    while (<SORTED>) {
        chomp();
        my ($start, $end, $score) = split(/\t/);
        push(@$array, $start);
        push(@$array, $end);
    }
    close(SORTED);

    my @sorted = sort { $a <=> $b } @$array;

    # release array
    @$array = ();

    # make sure each element in the array is unique and negative value is not allowed
    my $last = -1;
    foreach my $edge (@sorted) {
        if ($edge > $last) { push @$array, $edge; $last = $edge }
    }
}

1;
__END__

=item * $obj->main($fileArray, "wiggle") or $obj->maing($fileArray, "start") ;

This function creates wiggle plot file or start file based on the single selected or rescued position.

An example of first input file: chr1.test.for_wig 

# start	end	scores

1500	1534	1

1520	1554	2

An example of second input file: chr3.test.for_wig

# start	end	scores

1510	1544	1

The output will be: test.wiggle  

chr1	1500	1520	1

chr1	1520	1534	3

chr2	1534	1554	2

chr3	1510	1544	1

or test.start


chr1	1500	1

chr1	1520	2

chr3	1510	1

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
