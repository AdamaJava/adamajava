package QCMG::BaseClass::Mapping;

=head1 NAME

QCMG::BaseClass::Mapping

=head1 SYNOPSIS

  use QCMG::BaseClass::Mapping;
  my $obj = QCMG::BaseClass::Mapping->new(%argv);
  $obj->mapping;
  $obj->collation;
  $obj->get_schema;

=head1 DESCRIPTION

This module is a wrapper for the ABI (Life Tech) software Mapreads.  It calls mapreads 
to map tags to chromosomes listed on the hash table "genomes" element, 
and then collates all mapped tags into one file. Here we make an example to list all necessary arguments:

	my %argv = (
		'genomes' => ["/data/chr1.fa", "/data/chr2.fa"],
	        'exp_name' => "test"
	        'output_dir' => "/data/",
	        'mapreads' => "/data/mapreads",
	        'max_hits' =>  10,
	        'tag_file' => "/data/test.csfasta",
	        'mask' => "1111111111",
	        'tag_length' => 10,
	        'mismatch' => 1,
	        'adj_error' => 0,
	        'f_log' => "/data/test.log",
	);

Before it calls mapreads program, it will create related schema file according to above arguments.

=head2 Methods

=over 4

=cut

use strict;
use Object::InsideOut;
use File::Basename;
use File::Temp qw/tempfile tempdir/;
use Carp;
use Parallel::ForkManager;

use constant {TRUE => 'true', NUM_PARA => 5, COLOR_SPACE => 0, BASE_SPACE => 1};

# Attr:
#<<<
# parameters for whole mapping 
my @exp_name :Field :Arg('Name' => 'exp_name','Mandatory' => 1, 'Regexp' => qr/^exp_n[am]{1,2}e$/i);
my @output_dir :Field :Arg('Name' => 'output_dir','Mandatory' => 1, 'Regexp' => qr/^output[_]{1,2}dir$/i);
my @mapreads :Field :Arg('Name' => 'mapreads','Mandatory' => 1, 'Regexp' => qr/^mapread[s]{0,1}$/i);
my @max_hits :Field :Arg('Name' => 'max_hits','Regexp' => qr/^max[_]{0,2}hit[s]{0,1}$/i, 'Default' => 10);
my @tag_file :Field :Arg('Name' => 'tag_file', 'Mandatory' => 1, 'Regexp' => qr/^tag[s]{0,1}[_]{0,2}file$/i);
my @mask :Field :Arg('Name'=> 'mask', 'Mandatory' =>1, 'Regexp' => qr/^mask$/i );
my @space_code :Field :Arg('Name' => 'space_code', 'Regexp' => qr/^space\_code$/i, 'Default' => '0');
my @qsub_command :Field :Arg('Name' => 'qsub_command', 'Regexp' => qr/^qsub\_command$/i, 'Default' => 'qsub -l walltime=960:00:00'); 

# parameters for individual mapping
my @tag_length :Field :Arg('Name' => 'tag_length', 'Mandatory' => 1, 'Regexp' => qr/^tag_{1,2}length$/i);  #here only contain a single value of current mapping tag length
my @mismatch :Field :Arg('Name' => 'mismatch', 'Mandatory' => 1);
my @genomes :Field;	#it is an array reference
my @adj_errors :Field :Arg('Name' => 'adj_errors', 'Mandatory' => 1,'Regexp' => qr/^adj_{1,2}errors{0,1}/i) :Default(0);
my @schema_dir :Field ;
my @schema_file :Field;
my @scratch_dir :Field :Arg('Name' => 'scratch_dir', 'Mandatory' => 1, 'Regexp' => qr/^scratch_dir$/i);

# parameters for optional parameter to call f2m.pl
my @start :Field :Arg('Name' => 'start', 'Default' => 0);
my @multi :Field :Arg('Name' => 'multi', 'Default' => 0);

# to store all mapping outputs name from mapping method, which will be used for collation methods
my @f_mapped :Field;
my @f_nonMatch : Field :Get(f_nonMatch);
my @f_collated :Field :Get(f_collated);
my @objTool :Field :Arg('Name' => 'objTool', 'Mandatory' => 1,  'Regexp' => qr/^objtool/i);

my %args :InitArgs = ( 'genomes' =>{'Mandatory' =>1, 'Regexp' => qr/^genome[s]{0,1}$/i}, );

#>>>

# methods
sub _init : Init {
    my ($self, $arg) = @_;

    if ($tag_length[$$self] !~ m/\d\d/) {
        $objTool[$$self]->Log_DIED(
            "current mapping tag length ($tag_length[$$self]) is not in the rang [10,99]!");
    }
    if ($mismatch[$$self] !~ m/\d/) {
        $objTool[$$self]
          ->Log_DIED("current mismatch value ($mismatch[$$self]) is not in the rang [0,9]!");
    }
    if ($adj_errors[$$self] !~ m/[012]{1}/) {
        $objTool[$$self]->Log_DIED(
            "wrong adjacent error setting ($adj_errors[$$self]) -- not in the rang [0,2]!");
    }
    if ($space_code[$$self] !~ m/\d/) {
        $objTool[$self]->log_DIED(
            "Space code must be 0 (colorspace) or 1 (basespace).  See mapreads usage, S=## parameter.");
    }

    # setting all necessary parameters for current mapping
    if (ref($arg->{'genomes'}) eq "ARRAY") { 
        $self->set(\@genomes, $arg->{'genomes'}) }
    else { push @{$genomes[$$self]}, $arg->{'genomes'} }

    # get all mapped tag output names
    foreach my $f_gen (@{$genomes[$$self]}) {
        my $chr = fileparse($f_gen, qr/\.[\w\.]+/);
        my $output = $output_dir[$$self]
          . "$chr.$exp_name[$$self].ma.$tag_length[$$self].$mismatch[$$self].$adj_errors[$$self]";
        push @{$f_mapped[$$self]}, $output;
    }

    $self->set(\@f_collated,
        $output_dir[$$self]
          . "$exp_name[$$self].ma.$tag_length[$$self].$mismatch[$$self].$adj_errors[$$self].collated");
    $self->set(\@f_nonMatch,
        $output_dir[$$self]
          . "$exp_name[$$self].ma.$tag_length[$$self].$mismatch[$$self].$adj_errors[$$self].nonMatch");
    my $mymask = substr($mask[$$self], 0, $tag_length[$$self]);
    $self->set(\@mask, $mymask);

    # create schema here
    if (exists($arg->{'schema_dir'})) { $self->set(\@schema_dir, $arg->{'schema_dir'}) }
    else { $self->set(\@schema_dir, dirname($mapreads[$$self]) . "/schemas/") }
    &get_schema($self);

}


=item * $obj->mapping;

This function passes all arguments to mapreads and submits the command to a queueing system, 
it will wait until all mapping jobs are done.

=cut

sub mapping {
    my ($self) = @_;

    # store all queue mapping job flag files (*.success)
    my %q_file = ();

    if (!(-e $tag_file[$$self])) {
        $objTool[$$self]->Log_DIED("can't find reads file: $tag_file[$$self]");
    }

    # 	my $pm = new Parallel::ForkManager(NUM_PARA);

    # map to all reference genomes
    foreach my $f_gen (@{$genomes[$$self]}) {

        # 		$pm->start and next;
        # check log file, make sure any other paralled job are died
        $objTool[$$self]->check_died;

        # get reference genome name
        my $chr = fileparse($f_gen, qr/\.[\w\.]+/);

        # insert reference genome name in top of reads file and get output name
        my $output = $output_dir[$$self]
          . "$chr.$exp_name[$$self].ma.$tag_length[$$self].$mismatch[$$self].$adj_errors[$$self]";

        # for example, after quality check may no qualified tag with mers35, but lots of qualified with mers30
        if (-z $tag_file[$$self]) {
            $objTool[$$self]->Log_WARNING("no reads in reads file: $tag_file[$$self]!");
            open(OUT, ">$output")
              or $objTool[$$self]->Log_DIED("can't create mapping output file: $output!");
            close(OUT);
        }

        # submit mapping jobs to queue system
        else {

            # call mapreads straightly
            my $tagLength = $tag_length[$$self];
            if ($space_code[$$self] == BASE_SPACE) {
                $tagLength = $tag_length[$$self] - 1;
            }
            my $comm =
              "$mapreads[$$self] $tag_file[$$self] $f_gen M=$mismatch[$$self] S=$space_code[$$self] "
              . "L=$tagLength X=$mask[$$self] T=$schema_file[$$self] A=$adj_errors[$$self] "
              . "O=$start[$$self] Z=$max_hits[$$self] I=$multi[$$self] > $output";

            # create shell file to submit jobs to queue system
            my $mysh     = "$output.sh";
            my $flagFile = "$output." . "success";
            open(SH, ">$mysh")
              or $objTool[$$self]->Log_DIED("can't create shell file for queuing system -- $mysh!");

            # enter scratch directory
            my $ScratchDir = $scratch_dir[$$self];
            if (-d $scratch_dir[$$self] && -w _) {
                $ScratchDir = tempdir("MatchXXXXX", CLEANUP => 1, DIR => $scratch_dir[$$self]);
            }
            print SH "if \[ -d $ScratchDir \]\nthen\ncd $ScratchDir\nfi\n";

            # run mapreads
            print SH "$comm\necho \$\? > $flagFile\n";
            close(SH);
            $comm = $qsub_command[$$self] . " -o $mysh.out -e $mysh.err $mysh";
            my $rc = system($comm);
            $q_file{$flagFile} = $rc;

        }

        # 		$pm->finish;

    }

    # 	$pm->wait_all_children;
    # check q_file hash size, if zero may have some errors
    $objTool[$$self]->wait_for_queue(\%q_file);
    $objTool[$$self]->check_died;

    # delete all flage file
    foreach my $k (keys %q_file) { unlink($k) }
    foreach my $f (@{$f_mapped[$$self]}) {
        unlink("$f.sh");
        unlink("$f.sh.err");
        unlink("$f.sh.out");
    }

    # 	return 1;

}

=item * $obj->collation( );

This function collates all mapped positions from all mapped files (see QCMG::BaseClass::Mapping::mapping), 
if the total mapped position for a tag is over max_hit, all these position will be thrown away. 
It also reports all nonmapped tags for the next mappin loop by writing them to the 'nonMatch' file

=cut

sub collation {
    my $self = shift;

    $objTool[$$self]->check_died;

    # open output file
    open(COLLATED, ">$f_collated[$$self]")
      or $objTool[$$self]->Log_DIED("can't create collated file: $f_collated[$$self]");
    open(NON_MATCHED, "> $f_nonMatch[$$self]")
      or $objTool[$$self]->Log_DIED("can't create non-matched file: $f_nonMatch[$$self]");

    # open all inputs file -- the mapped files
    my %fh = ();
    foreach my $f (@{$f_mapped[$$self]}) {
        open(my $handle, $f) or $objTool[$$self]->Log_DIED("can't open mapped tag file: $f!");
        my $chr = fileparse($f, qr/\.[\w\.]+/);
        $fh{$chr} = $handle;
    }

    # get a mapped file and start reading tag from this file
    my @chromosomes = keys %fh;
    my $first_chr   = shift(@chromosomes);
    my $first       = $fh{$first_chr};
    while (<$first>) {
        if (!m/^>/) { next }
        chomp();
        my @matches      = ();
        my @first_matche = split(/,/);
        my $tagid        = shift(@first_matche);
        foreach my $m (@first_matche) { push @matches, "$first_chr.$m" }

        # seek other mapped files
        foreach my $chr (@chromosomes) {
            my $second = $fh{$chr};
            while (<$second>) {
                if (m/^>/) {
                    chomp();
                    my @tmp_matche = split(/,/);
                    my $id         = shift(@tmp_matche);
                    if ($id ne $tagid) {
                        $objTool[$$self]->Log_DIED(
                            "differnt tag id order in mapped files from $first_chr and $chr!");
                        last;
                    }
                    foreach my $m (@tmp_matche) { push @matches, "$chr.$m" }
                    last;
                }
            }    
        }   

        # coutn current tag total matched positions
        my $seq   = <$first>;
        my $total = scalar @matches;
        if ($total == 0) {
            print NON_MATCHED "$tagid\n$seq";
        }
        elsif ($total > $max_hits[$$self]) {
            print COLLATED "$tagid\t$total\n$seq";
        }
        else {
            print COLLATED "$tagid\t$total";
            foreach my $m (@matches) { print COLLATED "\t$m" }
            print COLLATED "\n$seq";
        }

    }   

    # close all files
    foreach my $f (keys %fh) { close($fh{$f}) }
    close(COLLATED);
    close(NON_MATCHED);

}


=item * get_schema()

This method fetches the appropriate schema file for the current mapreads run.  The schema file
is used to specify an optimised gapped seed pattern for alignment.  If no appropriate schema file
exists, then this method will write one.

=cut

sub get_schema {
    my $self      = shift;
    my $tagLength = $tag_length[$$self];
    if ($space_code[$$self] == BASE_SPACE) {
        $tagLength =
          $tag_length[$$self] - 1; # when base space is encoded into colorspace, 
                                   # a single base remains at the start of the sequence.
                                   # this will be excluded from mapping, making a 45mer 
                                   # base space sequence a 44mer colour space sequence.
    }
    my $standard_schema = "$schema_dir[$$self]schema_$tagLength" . "_$mismatch[$$self]";

    # check mask whether it is full "1", then use the standard schema file
    if ($mask[$$self] =~ m/^1{$tag_length[$$self]}$/) {
        $self->set(\@schema_file, $standard_schema);
        return 0;
    }
    else {
        $self->set(\@schema_file,
            "$output_dir[$$self]$exp_name[$$self].schema_$tagLength" . "_$mismatch[$$self]");
    }
    my @positions = split(//, $mask[$$self]);

    # create new schema file
    open(FILE, $standard_schema)
      or $objTool[$$self]->Log_DIED("can't open standarded schema file: $standard_schema");
    my @indicies = <FILE>;
    close(FILE);

    open(SCHEMA, ">$schema_file[$$self]")
      or $objTool[$$self]->Log_DIED("can't create schema file: $schema_file[$$self]");
    foreach my $line (@indicies) {
        chomp($line);
        if ($line =~ m/\#/) { next; }
        my @index_positions = split(//, $line);
        my $count = 0;
        for (my $i = 0 ; $i < $tagLength ; $i++) {
            if ($positions[$i] == 1) {
                print SCHEMA $index_positions[$count];
                $count++;
            }
            else {
                print SCHEMA $positions[$i];
            }
        }
        print SCHEMA "\n";
    }
    close(SCHEMA);

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
