package QCMG::BaseClass::Tools;

=head1 NAME

QCMG::BaseClass::Tools

=head1 SYNOPSIS

  use QCMG::BaseClass::Tools
  my $obj = tools->new('f_log' => 'my.log');
  $obj->check_died();
  $obj->check_died("chr1");
  $obj->wait_for_queue(\%q_file);
  $obj->Pre_recursiveMap($output_dir,$exp_name,$raw_data, $array_taglength);
  $obj->chop_tag($f_nonmatched, $f_shorttag, $l_chop);
  $obj->chop_fastq_tag($self, $f_nonmatched, $f_shorttag, $l_chop);
  $obj->Log_PROCESS($str);
  $obj->Log_SUCCESS($str);
  $obj->Log_WARNING($str);
  $obj->Log_DIED($str);

=head1 DESCRIPTION

This module contains several independant functions to support various mapping pipelines, such as XMate, miRNA-Mate and others.

=head2 Methods

=over 5

=cut


use Object::InsideOut;
use File::Copy;

# set sleeping time for sub wait_for_queue
use constant {
    SLEEP       => 60,
    COLOR_SPACE => 0,
    BASE_SPACE  => 1,
    TRUE        => "true",
    FALSE       => "false"
};

# Attr:
my @f_log : Field;

=item * Init: my $obj = new QCMG::BaseClass::Tools ( %parameters )

This module takes as input a path to a log file.  
It immediately opens this file to write to.

=cut

sub _init : Init {
    my ($self, $arg) = @_;

    if (exists $arg->{'f_log'}) { $self->set(\@f_log, $arg->{'f_log'}) }

    if (-e $f_log[$$self]) {
        die "The log file ["
          . $f_log[$$self]
          . "] already exists, this mapping run has already been attempted. "
          . "Please delete the log file and associated data for this run and launch X-MATE again.";
    }
    open(LOG, ">$f_log[$$self]")
      or die "Can't create log file: $f_log[$$self]\n";
    close(LOG);

}

=item * check_died()  or $obj->check_died($mess) ;

This function will read the log file, check whether previous steps or paralling job are died. 
It return "1" for died job and "0" for all jobs are ok. When we pass a message to it, 
it will check whether any DIED message in the log file contain the $mess. eg,

example of log file: [DIED]: during chrx mapping

$obj->check_died("chrx") #it will return 1, since we found a DIED information contain string "chrx"

=cut

sub check_died {
    my @arg  = @_;
    my $self = $arg[0];
    open(LOG, $f_log[$$self]) or die "can't open log file";
    while (my $line = <LOG>) {
        if ($line =~ m/^\[DIED\]/) {
            if (!exists $arg[1]) { die "$line" }
            
            # check whether the DIED information contains $arg[1] message
            elsif ($line =~ /($arg[1])$/) { die "$line" }
        }
    }
    close(LOG);

    # if there is no died information in the log file, return 0
}

=item * wait_for_queue(\%q_file);

This function will check whether all files in the hash table (%q_file) are created or not. 
For example, one step of the pipeline is to submit all the mapping jobs to queueing system, 
meanwhile the next step can’t excute until all these mapping jobs are done. 
Once one mapping job is done, it will created a small file, 
eg. chr1.tag_20000.mers35.chr10.unique.csfasta.ma.35.3.success (see f2m.pl). 

we add all these output file name into a hash table and pass it to this function, 
then every 60 seconds it will check these files until all of them are detected.

=cut

sub wait_for_queue {
    my ($self, $q_file) = @_;

    # at each one minute check whether all files in this hash table are exist
    # it will continue checking until all files are exist
    foreach my $f (keys %$q_file) {
        if ($q_file->{$f} != 0) {
            &Log_DIED($self, "failed to submit job to queuing sytem");
            last;
        }

        # wait the flag file to be created
        while (!(-e $f)) { sleep( SLEEP ) }

        # only allow maximum 60 seconds to finish writing in flag file
        for (my $i = 0; $i < 60; $i++) {
            if (-z $f) { sleep(1) }
            else { last }
        }

        open(FF, $f) or &Log_DIED($self, "can't open the flag file -- $f");
        chomp(my $rc = <FF>);
        if ($rc == 0) { next }
        else {
            &Log_DIED($self,
                "failed jobs in queueing system. see *.err files in your output_dir for more information."
            );
        }
        close(FF);

    }
}

# prepare for non quality check mapping
sub GetFilesNameForMap {
    my ($self, $output_dir, $exp_name, $array_tagLength) = @_;

    my @outputs = ();

    # return choped tag files name for recursive mapping
    foreach my $l (@$array_tagLength) {
        my $f = "$output_dir$exp_name.mers$l.csfasta";
        push @outputs, $f;
    }

    return \@outputs;
}


=item * Pre_recursiveMap($output_dir, $exp_name, $f_raw, $array_tagLength );

This function will be called when we jump over the tag_quality check function. 
This function check the recursive mapping tag length listing in the array "$array_tagLength"; 
if the longest length in the array is shorter than the raw tag length, 
the raw tag will be chopped (see method chop_tag and chop_fastq_tag). 
Then several empty output will be created, in which the chopped tag will 
be stored during the recursive mapping. 

=cut
 
sub Pre_recursiveMap {
    my ($self, $output_dir, $exp_name, $f_raw, $array_tagLength, $space_code, $map_isas) = @_;

    my @outputs = ();

    # create empty input files to store choped tag for recursive mapping
    foreach my $l (@$array_tagLength) {
        my $f = "$output_dir$exp_name.mers$l.csfasta";
        if ($space_code == BASE_SPACE) {
            if ($map_isas eq FALSE) {
                $f = "$output_dir$exp_name.mers$l.fasta";
            }
            else {
                $f = "$output_dir$exp_name.mers$l.fastq";
            }
        }
        open(FF, ">$f")
          or &Log_DIED($self, "can't create input: $f for recursive mapping.");
        close(FF);
        push @outputs, $f;
    }

    # get the first tag and measure it's length
    my $seq;
    if (($space_code == BASE_SPACE) && ($map_isas eq TRUE)) {
        # this is fastq format, so just take the second line (which is the sequence).
        open(RAW, $f_raw)
          or &Log_DIED($self, "can't open raw tag file: $f_raw.");
        my $count = 0;
        while (my $line = <RAW>) {
            $count++;
            if ($count == 2) {
                $seq = $line;
                last;
            }
        }
        close(RAW);
    }
    else {
        open(RAW, $f_raw)
          or &Log_DIED($self, "can't open raw tag file: $f_raw.");
        while (my $line = <RAW>) {
            if ($line =~ m/^>/) { $seq = <RAW>; last; }
        }
        close(RAW);
    }

    # the mapping input file with the longest tag length;
    my $l_max = $array_tagLength->[0];
    my $f_max;
    my $l_chop;

    # copy the raw tag into first mapping input
    chop($seq);
    if ($space_code == COLOR_SPACE) {
        $f_max  = "$output_dir$exp_name.mers$l_max.csfasta";
        $l_chop = length($seq) - 1 - $l_max;
    }
    elsif ($space_code == BASE_SPACE) {
        if ($map_isas eq FALSE) {
            $f_max = "$output_dir$exp_name.mers$l_max.fasta";
        }
        else {
            $f_max = "$output_dir$exp_name.mers$l_max.fastq";
        }
        $l_chop = length($seq) - $l_max;
    }
    else {
        &LOG_DIED($self,
            "unrecognised code for nucleotide space type (eg, neither 0 (color-space), nor 1(base-space).  "
              . "Please set the space_code=N parameter in config file.");
    }

    if ($l_chop == 0) { copy($f_raw, $f_max) }

    # or chop the raw tag and report it into the first mapping input
    elsif (($l_chop > 0) && ($space_code == BASE_SPACE) && ($map_isas eq TRUE)) {
        &chop_fastq_tag($self, $f_raw, $f_max, $l_chop);
    }
    elsif ($l_chop > 0) {
        &chop_tag($self, $f_raw, $f_max, $l_chop);
    }
    else {
        &Log_DIED($self,
            "tag length \"$l_max\" for mapping is longer than raw tag length.");
    }

    return \@outputs;
}


=item * chop_tag($f_nonmatched, $f_shorttag, $l_chop);

This function will read tags from csfasta file $f_nonmatched and then chop 
the last few mers( $l_chop ), report it to $f_shorttag file 
which may created by method Pre_recursive.

=cut

sub chop_tag {

    # input is the tag didn't matched on the genome or juction
    # output is the tag choped last few mers
    my ($self, $f_nonmatched, $f_shorttag, $l_chop) = @_;

    # open the input file in which the tag are not matched
    open(IN, $f_nonmatched) or &Log_DIED($self, "can't open $f_nonmatched");

    # open the output file in which the tag's last few mers will be choped
    open(OUT, ">>$f_shorttag") or &Log_DIED($self, "can't open $f_shorttag");
    while (my $line = <IN>) {
        if ($line =~ /^>/) {
            my $id   = $line;
            my $sequ = <IN>;
            chomp($sequ);
            $sequ = substr($sequ, 0, length($sequ) - $l_chop);
            print OUT $id . $sequ . "\n";
        }
    }
    close(IN);
    close(OUT);
}


=item * chop_fastq_tag($f_nonmatched, $f_shorttag, $l_chop);

This function will read tags from fastq file $f_nonmatched and then chop 
the last few mers( $l_chop ), report it to $f_shorttag file 
which may created by method Pre_recursive.

=cut

sub chop_fastq_tag {
    my ($self, $f_nonmatched, $f_shorttag, $l_chop) = @_;

    # open the input file in which the tag are not matched
    open(IN, $f_nonmatched) or &Log_DIED($self, "can't open $f_nonmatched");

    # open the output file in which the tag's last few mers will be choped
    open(OUT, ">>$f_shorttag") or &Log_DIED($self, "can't open $f_shorttag");
    while (my $header = <IN>) {
        my $seq     = <IN>;
        my $header2 = <IN>;
        my $qual    = <IN>;
        chomp($seq);
        chomp($qual);
        $seqChop  = substr($seq,  0, length($seq) - $l_chop) . "\n";
        $qualChop = substr($qual, 0, length($seq) - $l_chop);
        print OUT $header . $seqChop . $header2 . $qualChop . "\n";
    }
    close(IN);
    close(OUT);
}


=item * Log_PROCESS($str);

This function will add a string -- "[PROCESS]:" at top of the $str and then write into LOG file.

=cut

sub Log_PROCESS {
    my ($self, $mess) = @_;

    # open the log file for this experiment;
    open(LOG, ">>$f_log[$$self]")
      or &Log_DIED($self, "can not open $f_log tools_mapping.pm (myLog)");

    # print current time
    print LOG scalar localtime(), "\n";

    # print the information which is passed into this function by a string value
    print LOG "[PROCESS]: $mess\n";

    close(LOG);

}


=item * Log_SUCCESS($str);

This function will add a string -- "[SUCCESS]:" at top of the $str and then write into LOG file.

=cut 

sub Log_SUCCESS {
    my ($self, $mess) = @_;

    # open the log file for this experiment;
    open(LOG, ">>$f_log[$$self]")
      or &Log_DIED($self, "can not open $f_log tools_mapping.pm (myLog)");

    # print current time
    print LOG scalar localtime(), "\n";

    # print the information which is passed into this function by a string value
    print LOG "[SUCCESS]: $mess\n";

    close(LOG);

}


=item * Log_WARNING($str);

This function will add a string -- "[WARNING]:" at top of the $str and then write into LOG file.

=cut

sub Log_WARNING {
    my ($self, $mess) = @_;

    # open the log file for this experiment;
    open(LOG, ">>$f_log[$$self]")
      or &Log_DIED($self, "can not open $f_log tools_mapping.pm (myLog)");

    # print current time
    print LOG scalar localtime(), "\n";

    # print the information which is passed into this function by a string value
    print LOG "[WARNING]: $mess\n";

    close(LOG);

}


=item * Log_DIED($str);

This function will add a string -- "[DIED]:" at top of the $str and then write into LOG file.

=cut

sub Log_DIED {
    my ($self, $mess) = @_;

    open(LOG, ">> $f_log[$$self]")
      or die "can't open log file: $f_log[$$self] in QCMG::BaseClass::Tools::Log_DIED\n";
    print LOG scalar localtime(), "\n";
    print LOG "[DIED]: $mess\n";
    close(LOG);

    die "[DIED]: $mess\n";
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
