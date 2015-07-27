package QCMG::BaseClass::ISAS;

=head1 NAME

QCMG::BaseClass::ISAS

=head1 SYNOPSIS

  use QCMG::BaseClass::ISAS;
  my $obj = QCMG::BaseClass::ISAS->new(%parameters);
  $obj->make_colorspace_filename
  $obj->make_basespace_filename
  $obj->mapping
  $obj->collation_colors
  $obj->collation_bases
  $obj->check_setting
  $obj->check_general_setting
  $obj->check_color_setting
  $obj->check_base_setting

=head1 DESCRIPTION

This module wraps the ISAS mapping engine.  It provides methods to generate appropriate filenames for IO, 
methods to execute ISAS runs, parse ISAS output into collated mapping files and set ISAS settings appropriate
for the specified run.

=head2 Methods

=over 4

=cut

use strict;
use Object::InsideOut;
use File::Basename;
use File::Spec::Functions qw( catdir );
use Carp;
use Parallel::ForkManager;
use constant {TRUE => 'true', NUM_PARA => 5, COLOR_SPACE => 0, BASE_SPACE => 1};

# Attr:
# <<<
# parameters for whole mapping 
my @exp_name :Field :Arg('Name' => 'exp_name','Mandatory' => 1, 'Regexp' => qr/^exp_n[am]{1,2}e$/i);
my @output_dir :Field :Arg('Name' => 'output_dir','Mandatory' => 1, 'Regexp' => qr/^output[_]{1,2}dir$/i);
my @isas :Field :Arg('Name' => 'isas','Mandatory' => 1, 'Regexp' => qr/^isas$/i);

# parameters for run ISAS
my @database :Field :Arg('Name' => 'database', 'mandatory' => 1):Get(database);
my @chr :Field :Arg('Name' => 'chr', 'mandatory' => 1):Get('chr');  
my @mode :Field :Arg('Name' => 'mode', 'mandatory' => 1) :Default('2va') :Get(mode);
my @verbose :Field :Arg('Name' => 'verbose', 'mandatory' => 1) :Get(verbose) :Default(1);
my @limit :Field :Arg('Name' => 'limit', 'mandatory' => 1) :Get(limit) :Default(10);
my @filter :Field :Arg('Name' => 'filter', 'mandatory' => 1) :Get(filter):Default(0);
my @global :Field :Arg('Name' =>'global', 'mandatory' => 1) :Get(global);
my @file :Field :Arg('Name' => 'file', 'Mandatory' => 1, 'Regexp' => qr/^file$/i);
my @chrRename :Field :Arg('Name' => 'chrRename', 'Mandatory' => 1); #a hash
my @space_code :Field :Arg('Name' => 'space_code', 'Mandatory' => 1);

# to store all mapping outputs name from mapping method, which will be used for collation methods
my @f_mapped :Field :Get(f_mapped);
my @f_nonMatch : Field :Get(f_nonMatch);
my @f_collated :Field :Get(f_collated);
my @objTool :Field :Arg('Name' => 'objTool', 'Mandatory' => 1,  'Regexp' => qr/^objtool/i);
# >>>

# methods
sub _init : Init {
    my ($self, $arg) = @_;

    my ($file, $length) = undef;

    if ($space_code[$$self] == COLOR_SPACE) {
        ($file, $length) = &make_colorspace_filename($self);
    }
    elsif ($space_code[$$self] == BASE_SPACE) {
        ($file, $length) = &make_basespace_filename($self);
    }

    $self->set(\@f_mapped, $file);

    $file =~ s/txt$/collated/;
    $self->set(\@f_collated, $file);

    $file = $output_dir[$$self] . $exp_name[$$self] . ".mers$length.nonMatch";
    $self->set(\@f_nonMatch, $file);

}

=item * make_colorspace_filename()

Create the ISAS filename from user specified parameters for 
colorspace mapping output.

=cut

sub make_colorspace_filename {
    my $self = shift;

    my $f = $file[$$self];
    (my $mod = $mode[$$self]) =~ tr/[a-z]/[A-Z]/;
    $f .= ".Mode" . $mod;

    my ($l, $m) = split(',', $global[$$self]);
    if   ($l == 25) { $f .= ".Length$l" }
    else            { $f .= ".Global$l-$m" }

    $f .= "-Limit" . $limit[$$self];

    if    ($verbose[$$self] == 1) { $f .= "-NormalOutput" }
    elsif ($verbose[$$self] == 0) { $f .= "-NonVerbose" }
    elsif ($verbose[$$self] == 2) { $f .= "-UniqueOnly" }

    if   ($filter[$$self] == 0) { $f .= "-NoFilter.txt" }
    else                        { $f .= "-FilterLevel" . $filter[$$self] . ".txt" }

    return ($f, $l);
}


=item * make_basespace_filename()

Create the ISAS filename from user specified parameters for
basespace mapping output

=cut

sub make_basespace_filename {
    my $self = shift;

    my $f = $file[$$self];

    my ($l, $m) = split(',', $global[$$self]);

    $f .= ".ReadLength$l-Mismatches$m";
    $f .= "-Limit" . $limit[$$self];
    $f .= "-Ungapped";

    if    ($verbose[$$self] == 1) { $f .= "-Normal" }
    elsif ($verbose[$$self] == 0) { $f .= "-NonVerbose" }
    elsif ($verbose[$$self] == 2) { $f .= "-UniqueOnly" }

    if   ($filter[$$self] == 0) { $f .= "-NoFilter.txt" }
    else                        { $f .= "-FilterLevel" . $filter[$$self] . ".txt" }

    return ($f, $l);
}


=item * mapping()

Call an ISAS mapping run.  First set all the parameters for the run.  Do this by
calling ISAS for each parameter (from the command line it only allows for one
parameter to be specified at a time).  Then call the actual mapping run.

=cut

sub mapping {
    my ($self) = @_;

    # check log file, make sure any other paralled job are died
    $objTool[$$self]->check_died;

    if (!(-e $file[$$self])) { $objTool[$$self]->Log_DIED("can't find reads file: $file[$$self]") }

    # for example, after quality check may no qualified tag with mers35, but lots of qualified with mers30
    my $output = $f_mapped[$$self];
    if (-z $file[$$self]) {
        $objTool[$$self]->Log_WARNING("no reads in reads file: $file[$$self]!");
        open(OUT, ">$output")
          or $objTool[$$self]->Log_DIED("can't create mapping output file: $output!");
        close(OUT);
        return 1;
    }

    # run ISAS
    my @comm = ();
    push @comm, $isas[$$self] . " database=" . $database[$$self];
    push @comm, $isas[$$self] . " chr=" . $chr[$$self];
    push @comm, $isas[$$self] . " verbose=" . $verbose[$$self];
    push @comm, $isas[$$self] . " limit=" . $limit[$$self];
    push @comm, $isas[$$self] . " filter=" . $filter[$$self];

    if ($space_code[$$self] == COLOR_SPACE) {
        push @comm, $isas[$$self] . " mode=" . $mode[$$self];
        push @comm, $isas[$$self] . " global=" . $global[$$self];
    }
    elsif ($space_code[$$self] == BASE_SPACE) {

        #  base space ISAS does not use the 'global' parameter, and the 'mode' parameter
        #  takes form of <length>,<mmismatch>.  This corosponds directly with the Xmate 'global'
        #  parameter.  So to keep the configuration file as consistent as possible, we will
        #  simply substitute the 'global' parameters for base space isas with the 'mode' parameters.
        #  this means the 'mode' parameter will be ignored.
        push @comm, $isas[$$self] . " mode=" . $global[$$self];
    }

    for (my $i = 0 ; $i < scalar(@comm) ; $i++) {
        my $command = $comm[$i];
        system($command) == 0
          or $objTool[$$self]
          ->Log_DIED("Get failure return value during run ISAS, please retry command: $command");
        sleep(30);
    }
    if ($space_code[$$self] == COLOR_SPACE) {
        &check_setting($self, $database[$$self], 'settings-colors.txt');
    }
    elsif ($space_code[$$self] == BASE_SPACE) {
        &check_setting($self, $database[$$self], 'settings-bases.txt');
    }
    $objTool[$$self]->check_died;

    my $command = $isas[$$self] . " file=" . $file[$$self];
    my $rc      = system($command);
    sleep(30);
    if ($rc != 0) {
        $objTool[$$self]->Log_DIED(
            "Get failure return value $rc during run ISAS, please retry command: $command");
        return -1;
    }

    # change output file name
    my @fTXT = <$output_dir[$$self]*.txt>;
    my $flag = 0;
    foreach my $f (@fTXT) {
        if ($f =~ m/Stats\.txt$/) {
            my $fNew = $f;
            $fNew =~ s/\.txt$//;
            rename($f, $fNew);
            $flag++;
            next;
        }
        if ($f =~ m/$file[$$self]/) {
            if ($space_code[$$self] == COLOR_SPACE) {
                my $fNew = $f;
                $fNew =~ s/\.txt$/\.ma/;
                rename($f, $fNew);
                $self->set(\@f_mapped, $fNew);
            }

            $flag++;
            next;
        }
    }
    if ($flag != 2) {
        $objTool[$$self]->Log_DIED(
            "can't find ISAS mapped output, such as $f_mapped[$$self]; It might be ISAS died, please retry $command ");
        return -1;
    }

}


=item * collation_colors()

This function collates all mapped positions from an ISAS run.  It writes both
the 'collated' file (containing all unique and multi mappers), and the 'nonMatch'
file (containing all unmapped tags).  The nonMatch file is then chopped to a new
specified length, and remapped.  Note that the output format for ISAS in colourspace
is diffferent from base space ISAS, hence the different methods.  This method takes
its parameters from the module directly. 

=cut

sub collation_colors {
    my $self = shift;

    $objTool[$$self]->check_died;

    # open output file
    open(COLLATED, ">$f_collated[$$self]")
      or $objTool[$$self]->Log_DIED("can't create collated file: $f_collated[$$self]");
    open(NON_MATCHED, "> $f_nonMatch[$$self]")
      or $objTool[$$self]->Log_DIED("can't create non-matched file: $f_nonMatch[$$self]");

    # get chromosome original name, if junction we name it as same as the junction index file
    # get a mapped file and start reading tag from this file
    open(MAP, $f_mapped[$$self])
      or $objTool[$$self]->Log_DIED("can't open mapped file $f_mapped[$$self]");
    while (<MAP>) {
        if (!m/^>/) { next }
        chomp();
        my @maps  = split(',');
        my $id    = shift(@maps);
        my $seq   = <MAP>;
        my $tatal = scalar(@maps);
        if ($tatal == 0) { print NON_MATCHED "$id\n$seq"; next }

        print COLLATED "$id\t$tatal";
        foreach my $m (@maps) {
            my ($chr, $mm);
            if ($m =~ m/\d+_/) { ($chr, $mm) = split('_', $m) }
            else               { $chr = 1; $mm = $m }
            $chr = $chrRename[$$self]->{$chr};
            print COLLATED "\t$chr.$mm";
        }
        print COLLATED "\n$seq";
    }

    # close all files
    close(MAP);
    close(COLLATED);
    close(NON_MATCHED);
}


=item * collation_bases()

This function collates all mapped positions from an ISAS run.  It writes both
the 'collated' file (containing all unique and multi mappers), and the 'nonMatch'
file (containing all unmapped tags).  The nonMatch file is then chopped to a new
specified length, and remapped.  ISAS Bases takes as input a fastq format file,
so when we are writing the 'nonmatch' file here, we need to refer to and parse the 
original fastq file to retrieve the quality strings for each sequence.
Note that the output format for ISAS in colourspace
is diffferent from base space ISAS, hence the different methods.  This method takes
its parameters from the module fields directly.

=cut

sub collation_bases {

    my ($self, $fastqFile) = @_;

    open(MAPPINGS, $f_mapped[$$self])
      or $objTool[$$self]->Log_DIED("can't read from ISASbases output file: $f_mapped[$$self]: $!");
    open(FASTQ, $file[$$self])
      or $objTool[$$self]->Log_DIED("can't read from FASTQ file: $file[$$self]: $!");
    open(COLLATED, ">$f_collated[$$self]")
      or $objTool[$$self]->Log_DIED(
        "can't write to collated file [$f_collated[$$self]] when parsing ISASBases output file: $!\n");
    open(NON_MATCHED, ">$f_nonMatch[$$self].tmp")
      or $objTool[$$self]->Log_DIED("can't write to nonMatchedFile [$f_nonMatch[$$self].tmp] :$!");

    #  this method relies on the fact that the isas bases output is ordered
    #  the same as the fastq input.  It reads the mapping output
    #  and the fastq file, writing unmapped tags to the nonMatched file, and mapped
    #  tags to the collated file.
    my ($seq, $id) = undef;
    while (my $fastqLine = <FASTQ>) {
        while (my $mappingLine = <MAPPINGS>) {

            #  get the current sequence and id
            if ($mappingLine =~ /^>(\w+)\s+\@(\S+)/) {
                ($seq, $id) = ($1, $2);
            }
            else {
                $objTool[$$self]->Log_DIED("Failed to parse sequence and id from $mappingLine");
            }

            #  get the next line, if it is not blank, this sequence is mapped.
            #  continue to read the mappings until we get a blank line, then
            #  write out the information to the collated file.
            $mappingLine = <MAPPINGS>;
            if ($mappingLine =~ /\S+/) {
                my @mappings = ();
                my $done     = 0;
                while (!$done) {
                    if ($mappingLine =~ /^\s+\w+\s+([\-]?)(\d+)\:(\d+)\.\.\d+\s+(\d+)/) {
                        my $chr = $chrRename[$$self]->{$2};
                        push(@mappings, "$chr.$1$3.$4");
                        $mappingLine = <MAPPINGS>;
                    }
                    else {
                        print COLLATED ">$id\t"
                          . (scalar @mappings) . "\t"
                          . join("\t", @mappings)
                          . "\n$seq\n";
                        $done = 1;
                    }
                }
            }

            #  otherwise this sequence is unmapped, so write the fastq sequence to the nonMatch file.
            else {
                my $foundFastq = 0;
                while (!$foundFastq) {
                    if ($fastqLine =~ /^\@$id\s+/) {
                        for (my $i = 0 ; $i < 4 ; $i++)
                        {    # fastq files must have 4 lines per sequence.
                            print NON_MATCHED $fastqLine;
                            $fastqLine = <FASTQ>;
                        }
                        $foundFastq = 1;
                    }
                    else {
                        for (my $i = 0 ; $i < 4 ; $i++) {
                            $fastqLine = <FASTQ>
                              ;    # eat the lines for this sequence (ie, it must have been mapped).
                        }
                    }
                }
            }
        }
    }

    close MAPPINGS;
    close FASTQ;
    close COLLATED;
    close NON_MATCHED;

    #  rename the non-matched tmp file to the non-matched file.
    rename("$f_nonMatch[$$self].tmp", $f_nonMatch[$$self]);
}


=item * check_setting($ISASDatabasePath, $settingsFilePath)

The first of 4 subroutines to parse the settings of ISAS files. ISAS writes the 
last session's setting to an output file ready for the next run.  Because we need
to call ISAS once for each setting in a mapping run, and there are no restrictions
on when ISAS can be called, we are required to check whether the settings have been 
altered between calls.  This method takes as input the ISAS database name and the settings
file location, then forks off to either basespace or colorspace routines. 

=cut

sub check_setting {
    my ($self, $database, $f_setting) = @_;

    my $file = catdir($database, $f_setting);
    open(SET, $file) or $objTool[$$self]->Log_DIED("cannot open ISAS setting file: $file");
    my @settingLines = <SET>;
    close SET;

    if ($space_code[$$self] == COLOR_SPACE) {
        &check_color_setting($self, $file, @settingLines);
    }
    elsif ($space_code[$$self] == BASE_SPACE) {
        &check_base_setting($self, $file, @settingLines);
    }
}


=item * check_general_setting($settingFilePath, $settingValue, $settingName)

Some ISAS settings are shared between base space and color space, so code to check these
only once here.

=cut

sub check_general_setting {
    my ($self, $file, $value, $para) = @_;

    my $flag = 0;
    if (defined($para)) {
        print "checking $para\n";
    }
    else {
        print "para not defined\n";
    }
    if ($para =~ m/FirstChromosome/i) {
        if   ($chr[$$self] =~ m/^$value,\d+$/) { $flag++ }
        else                                   { $flag = -1; }
    }
    elsif ($para =~ m/LastChromosome/i) {
        if   ($chr[$$self] =~ m/^\d+,$value$/) { $flag++ }
        else                                   { $flag = -1; }
    }
    elsif ($para =~ m/Limit/i) {
        if   ($value == $limit[$$self]) { $flag++ }
        else                            { $flag = -1; }
    }
    elsif ($para =~ m/VerboseOutput/i) {
        if   ($value == $verbose[$$self]) { $flag++ }
        else                              { $flag = -1; }
    }
    elsif ($para =~ m/FilterLevel/i) {
        if   ($value == $filter[$$self]) { $flag++ }
        else                             { $flag = -1; }
    }

    if ($flag == -1) {
        $objTool[$$self]->Log_DIED("wrong value in seting file: $file, see the line: $value,$para");
        return -1;
    }

    return $flag;
}


=item * check_color_setting($settingFilePath, @settingLines)

check the settings specific to colorspace ISAS runs.

=cut

sub check_color_setting {
    my ($self, $file, @lines) = @_;

    my $flag = 0;

    foreach my $line (@lines) {
        if ($line =~ m/^\#/) { next }
        chomp($line);
        my ($value, $para) = split(",", $line);
        my $generalFlag = &check_general_setting($self, $file, $value, $para);
        if ($generalFlag > 0) { $flag += $generalFlag; next; }

        elsif ($para =~ m/ExtendedReadLength/i) {
            if   ($global[$$self] =~ m/^$value\,\d+$/) { $flag++ }
            else                                       { $flag = -1 }
        }

        if ($flag == -1) {
            $objTool[$$self]->Log_DIED("wrong value in seting file: $file, see the line: $line");
            return -1;
        }
    }

    if ($flag != 6) {
        $objTool[$$self]->Log_DIED(
            "Can't find some parameters in setting file: $file, which is listing in configure");
        return -1;
    }

    return 1;
}


=item * check_base_setting($settingFilePath, @settingLines)

Check the settings specific to basespace ISAS.

=cut

sub check_base_setting {
    my ($self, $file, @lines) = @_;
    my $flag = 0;

    foreach my $line (@lines) {
        if ($line =~ m/^\#/) { next }
        chomp($line);
        my ($value, $para) = split(",", $line);
        my $generalFlag = &check_general_setting($self, $file, $value, $para);
        if ($generalFlag > 0) { $flag += $generalFlag; next; }
        if ($para =~ m/AllowedMismatches/i) {
            if   ($global[$$self] =~ m/^\d+,$value$/) { $flag++ }
            else                                      { $flag = -1 }
        }
        elsif ($para =~ m/ReadLength/i) {
            if   ($global[$$self] =~ m/^$value,\d+$/) { $flag++ }
            else                                      { $flag = -1 }
        }

        if ($flag == -1) {
            $objTool[$$self]->Log_DIED("wrong value in seting file: $file, see the line: $line");
            return -1;
        }
    }

    if ($flag != 7) {
        $objTool[$$self]->Log_DIED(
            "Can't find some parameters in setting file: $file, which is listing in configure");
        return -1;
    }
    else { print "successfully saving setting into file: $file\n" }

    return 1;
}

1;


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
