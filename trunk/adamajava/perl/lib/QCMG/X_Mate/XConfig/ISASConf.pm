package QCMG::X_Mate::XConfig::ISASConf;

=head1 NAME

QCMG::X_Mate::XConfig::ISASConf

=head1 SYNOPSIS

  use QCMG::X_Mate::XConfig::ISASConf
  my $obj = QCMG::X_Mate::XConfig::ISASConf->new(
        'database'      => $para->{'database'},
        'chr'           => $para->{'chr'},
        'mode'          => $para->{'mode'},
        'verbose'       => $para->{'verbose'},
        'limit'         => $para->{'limit'},
        'filter'        => $para->{'filter'},
        'global'        => $para->{'global'}->[$i],
        'chrRenameFile' => $para->{'chrname_index'},
        'space_code'    => $space_code[$$self]);
  $obj->checkPara($section)
  $obj->getChrName($chrRenameFile)

=head1 DESCRIPTION

This is an auxiliary configuraiton and tools module for ISAS related configuration.

=head2 Methods

=over 5

=cut


use strict;
use Object::InsideOut;
use File::Spec::Functions qw( catdir );
use constant {TRUE => 'true', FALSE => 'false', COLOR_SPACE => 0, BASE_SPACE => 1};

# attr
my @database :Field :Arg('Name' => 'database', 'mandatory' => 1):Get(database);
my @chr :Field :Arg('Name' => 'chr', 'mandatory' => 1):Get('chr');
my @mode :Field :Arg('Name' => 'mode', 'mandatory' => 0) :Default('2va') :Get(mode);
my @verbose :Field :Arg('Name' => 'verbose', 'mandatory' => 1) :Get(verbose) :Default(1);
my @limit :Field :Arg('Name' => 'limit', 'mandatory' => 1) :Get(limit) :Default(10);
my @filter :Field :Arg('Name' => 'filter', 'mandatory' => 1) :Get(filter):Default(0);
my @global :Field :Arg('Name' => 'global', 'mandatory' => 1) :Get(global);
my @space_code :Field :Arg('Name' => 'space_code', 'mandatory' => 1) :Get(space_code) :Default(0);

my @chrRename :Field :Get('IndexRename'); #store a hash of renamed chromosme
my %init_args :InitArgs = ('chrRenameFile' => '', 'junc_name' => '');

# methods
sub _init : Init {
    my ($self, $arg) = @_;

    my $hash = {};
    if (exists($arg->{'chrRenameFile'})) {
        $hash = &getChrName($chr[$$self], $arg->{'chrRenameFile'});
    }
    elsif (exists($arg->{'junc_name'})) {
        my ($mis, $max) = split(',', $chr[$$self]);
        for (my $i = $mis ; $i <= $max ; $i++) { $hash->{$i} = $arg->{'junc_name'} }
    }
    $self->set(\@chrRename, $hash);

}


=item * check_para($section)

run checks on ISAS parameters.

=cut

sub check_para {
    my ($self, $section) = @_;

    # check database
    return "can't find 'reference' directory under \"$database[$$self]\" listed in database in section [$section]"
      unless -e catdir($database[$$self], "reference");

    # check chr
    return "wrong value $chr[$$self] for parameter \"chr\" in section [$section]  "
      unless $chr[$$self] =~ m/(\d+)\,(\d+)/;
    return "please list the smaller chromosome number first than the bigger one" unless $1 <= $2;

    # check verbose, limit and filter
    return "value $limit[$$self] for parameter \"limit\" isn't \[2,10\] in section [$section]"
      unless $limit[$$self];
    return "value $filter[$$self] for parameter \"filter\" isn't in range \[0,10\] in section [$section]"
      unless $filter[$$self] =~ m/^(\d|10)$/;
    return "value $verbose[$$self] for parameter \"verbose\" isn't in range\[0,2\] not in section [$section]"
      unless $verbose[$$self] =~ /^[0|1|2]$/;

    # check mode and global
    if ($space_code[$$self] == COLOR_SPACE) {
        my @Mode25 = (0, 1, 2, '2va');
        my @Mode50 = (3, '3VA', 4, '4VA', 5);
        my ($l,       $mis)    = split(/,/, $global[$$self]);
        my ($misMode, @others) = split(//,  $mode[$$self]);
        return "The mismatch number must be greater than $misMode as you set ISAS mode = $mode[$$self], see error: global=$global[$$self] "
          unless $mis >= $misMode;

        if (grep($mode[$$self], @Mode25)) {
            return "please set tag length in rang (25,33-62) for ISAS global mapping, see error: $global[$$self]  "
              unless (($l >= 33 && $l <= 62) || ($l == 25));
        }
        elsif (grep($mode[$$self], @Mode50)) {
            return "please set tag length in rang (50-62) for ISAS global mapping as you set mode=$mode[$$self], see error: $global[$$self]  "
              unless ($l >= 50 && $l <= 62);
        }
        else { return "invalid setting for parameter mode" }
    }
    elsif ($space_code[$$self] == BASE_SPACE) {
        my ($l, $mis) = split(/,/, $global[$$self]);
        if ($l > 88) {
            return "please set tag length less than 88 for ISASbases: see error: $global[$$self]";
        }
    }
    return TRUE;
}


=item * getChrName($renameFileLocation)

read the renamedChromosomes.txt file that isas produces and 
load it into this object so that we can decode the chromosome
numbers into names when writing mapping results.

=cut

sub getChrName {
    my ($chr, $f_index) = @_;

    my %rename = ();
    open(INDEX, $f_index) or die "can't open chromosome rename index file\n";
    while (<INDEX>) {
        chomp();
        my ($org, $numName) = split('\t');
        $org =~ s/\.fa//;
        if ($numName =~ m/chr(\d+)\.fa/) { $rename{$1} = $org }
        else { die "invalid file rename in index file -- $org\t$numName\n" }
    }
    close(INDEX);

    my ($mis, $max) = split(',', $chr);
    my @sorted = sort { $a <=> $b } (keys %rename);

    # check $mis, $max in the rename hash range
    my %ChrName_index = ();
    for (my $i = $mis ; $i <= $max ; $i++) { $ChrName_index{$i} = $rename{$i} }

    return \%ChrName_index;
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

This software is copyright 2010 by the Queensland Centre for Medical
Genomics. All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.  For commercial or governmental 
use, please contact licensing\@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/

=cut

