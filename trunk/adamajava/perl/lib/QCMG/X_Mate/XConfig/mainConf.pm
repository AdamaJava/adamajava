package QCMG::X_Mate::XConfig::mainConf;

=head1 NAME

QCMG::X_Mate::XConfig::mainConf

=head1 SYNOPSIS

  use QCMG::X_Mate::XConfig::mainConf
  my $obj = QCMG::X_Mate::XConfig::mainConf->new('f_name' => 'my.conf');
  $obj->verify_config
  $obj->verify_standard
  $obj->verify_options
  $obj->verify_genome
  $obj->verify_junction
  $obj->verify_isas

=head1 DESCRIPTION

This module contains all configuraiton values, as well as methods to parse the
config values from the config file and check the values for validity and conflicts.

=head2 Methods

=over 5

=cut

use strict;
use QCMG::X_Mate::XConfig::ISASConf;
use Object::InsideOut 'QCMG::BaseClass::Config';
use QCMG::BaseClass::Tools;
use File::Basename;
use Carp;

# define the constants
use constant {TRUE => 'true', FALSE => 'false', COLOR_SPACE => 0, BASE_SPACE => 1};

# Attr:
#  parameter from configure file straiteway
my @exp_name :Field :Get(exp_name);
my @raw_tag_length :Field :Get(raw_tag_length);
my @output_dir :Field :Get(output_dir);
my @raw_csfasta :Field :Get(raw_csfasta);
my @expect_strand :Field :Get(expect_strand) :Default('0');

# for genome mapping by mapreads
my @max_multimatch :Field :Get(max_hits) :Default(10);
my @mask :Field :Get(mask);
my @recursive_maps :Field :Get(recursive_maps);	#array reference 
my @genomes :Field :Get(genomes); #array reference
my @mapreads :Field :Get(mapreads);
my @schema_dir :Field :Get(schema_dir);
my @space_code :Field :Get(space_code) :Default(0); # switch between colour space and base space mapping.

# parameters for ISAS
my @isas :Field :Get(isas);
my @isas_genome :Field :Get(isas_genome);
my @isas_junc :Field :Get(isas_junc);

# optional
my @map_ISAS :Field :Get(map_ISAS) :Default('false');
my @quality_check :Field :Get(quality_check) :Default('false'); 
my @raw_qual :Field :Get(raw_qual);
my @run_rescue :Field :Get(run_rescue) :Default('false');
my @map_junction :Field :Get(map_junction) :Default('false');
my @rescue_program :Field :Get(rescue_program);
my @rescue_window :Field :Get(rescue_window);
my @junction_library :Field :Get(junc_lib); #return an array reference
my @junction_index :Field :Get(junc_index); #return an array reference
my @scratch_dir :Field :Get(scratch_dir) :Default('/scratch/');

#  optional queue submission string:
my @qsub_command :Field :Get(qsub_command) :Default('qsub -l walltime=960:00:00'); # note: .o, .e and shell scripts are created automatically.

# the parameter without getting from configure file directly
my @max_mismatch :Field :Get(max_mismatch) :Default(0);
my @chr_names :Field :Get(chr_names); #return an array reference
my @tag_lengths :Field :Get(map_lengths); #return an array reference
my @objTool :Field :Std(objTool);

#  sam file parameters:
my @make_sam :Field :Get(make_sam);
my @qv_file :Field :Get(quality_file);
my @ma_to_gff_threads :Field :Get(threads);
my @multi_sam :Field :Get(multi_sam);
my @gff_to_sam_program :Field :Get(gff_to_sam_program);


sub _init : Init {

    my ($self, $arg) = @_;
    my $conf_para = {};
    $self->QCMG::BaseClass::Config::read_config($conf_para);
    &verify_config($self, $conf_para);

}


=item * verify_config($configurationObject)

Verify all the sections of the configuraiton in turn.  This method
calls each of the other methods in this module as required.  If a 
configuraiton value is flagged as invalid, the error message from the
appropriate method propogates up to this method which exits the
program with the terminal error message.

=cut

sub verify_config {
    my ($self, $conf_para) = @_;

    my $fname = $self->QCMG::BaseClass::Config::f_conf;

    # reduce all non nessary die message
    $SIG{__DIE__} = sub { };

    # verify standard parameters here
    my $mess = TRUE;
    foreach my $key (keys %$conf_para) {
        if ($key eq "standard parameters") {
            $mess = &verify_standard($self, $key, $conf_para->{$key});
        }
        elsif ($key eq "options") { $mess = &verify_options($self, $key, $conf_para->{$key}) }
        if ($mess ne TRUE) { croak "ERROR in configure file $fname:\n$mess\n" }
    }

    if ($map_ISAS[$$self] eq TRUE) {
        $mess = &verify_isas($self, "genome isas", $conf_para->{'genome isas'});

        # make sure the raw tag file is under output directory
        my $dir = $raw_csfasta[$$self];
        if ($output_dir[$$self] =~ m/^$dir[\\\/]{0,1}$/) {
            croak "ERROR in configure file $fname:\n Please move raw tag file to the specified output directory\n";
        }
    }
    else {
        $mess = &verify_genome(
            $self,
            "genome mapping",
            $conf_para->{'genome mapping'},
            $raw_tag_length[$$self]);
    }
    if ($mess ne TRUE) { croak "ERROR in configure file $fname:\n$mess\n" }

    if ($map_junction[$$self] eq TRUE) {
        my $key = "junction mapping";
        my $num;
        if   ($map_ISAS[$$self] eq TRUE) { $num = scalar(@{$tag_lengths[$$self]}) }
        else                             { $num = scalar(@{$recursive_maps[$$self]}) }
        $mess = &verify_junction($self, $key, $conf_para->{$key}, $num);
        if ($mess ne TRUE) { croak "ERROR in configure file $fname:\n$mess\n" }
    }

}



=item * verify_standard($configSection, \%configSectionData)

verify all the required (standard) configuration values.

=cut

sub verify_standard {
    my ($self, $section, $para) = @_;

    my %check = (
        'exp_name'       => 0,
        'output_dir'     => 0,
        'raw_csfasta'    => 0,
        'expect_strand'  => 0,
        'raw_tag_length' => 0);
        
    foreach my $currentKey (keys %$para) {
        $currentKey =~ tr/[A-Z]/[a-z]/;
        my $currentValue = $para->{$currentKey};
        if ($currentKey eq "exp_name") {
            return "\t parameter value for \"exp_name\" in section [$section] must be alphnumic and '_'"
              unless $currentValue =~ m/^\w+$/;
            $self->set(\@exp_name, $currentValue);
        }
        elsif ($currentKey eq "output_dir") {
            return "\t can't find output directory \"$currentValue\" listed in section [$section]. Please create it first."
              unless -d $currentValue;
            return "\t please end directory of $currentKey in section [$section] with '\\' or '\/'"
              unless $currentValue =~ m/[\\\/]$/;
            $self->set(\@output_dir, $currentValue);
        }
        elsif ($currentKey eq "raw_csfasta") {
            return "\t cannot find raw tag file: $currentValue listed in section [$section]"
              unless -e $currentValue;
            $self->set(\@raw_csfasta, $currentValue);
        }
        elsif ($currentKey eq "expect_strand") {
            return "\t parameter value for \"expect_strand\" listed in section [$section] must be '+', '-' or '0'"
              unless $currentValue =~ m/^[\+\-0]{1}$/;
            $self->set(\@expect_strand, $currentValue);
        }
        elsif ($currentKey eq "raw_tag_length") {
            return "\t parameter value for \"raw_tag_length\" in section [$section] is between 0 to 99 "
              unless $currentValue =~ m/^\d{1,2}$/;
            $self->set(\@raw_tag_length, $currentValue);
        }
        else { return "invalid parameter '$currentKey' listed in section [$section]" }
        if ($check{$currentKey} == 0) { $check{$currentKey} = 1 }
        else {
            return "\t same parameter \"$currentKey\" appeared more than once in section [$section]";
        }
    }

    foreach my $key (keys %check) {
        if ($check{$key} == 0) { return "can't find parameter \"$key\" in section [$section]" }
    }

    return TRUE;
}


=item * verify_options($configSection, \%configSectionData)

verify all the optional configuration values.

=cut

sub verify_options {
    my ($self, $section, $para) = @_;
    use constant {QUAL => 'quality_check', RESC => 'run_rescue', JUNC => 'map_junciton'};

    if   (exists $para->{'scratch_dir'}) { $self->set(\@scratch_dir, $para->{'scratch_dir'}); }
    else                                 { $self->set(\@scratch_dir, "/scratch/"); }

    # check quality check ralated parameters
    if (exists $para->{'quality_check'}) {
        if ($para->{'quality_check'} eq TRUE) {
            return "\t cannot find parameter \"raw_qual\" " unless exists $para->{'raw_qual'};
            return "\t cannot find raw tag quality file: $raw_qual[$self]"
              unless -e $para->{'raw_qual'};
            $self->set(\@raw_qual,      $para->{'raw_qual'});
            $self->set(\@quality_check, TRUE);
        }
        elsif (($para->{'quality_check'} ne FALSE)) {
            return "\t invalid value $para->{'quality_check'} assigned to paramter 'quality_check' in section[$section]";
        }
    }

    # check run_rescue related parameters
    if (exists $para->{'run_rescue'}) {
        if (($para->{'run_rescue'} eq TRUE)) {
            return "\t cannot find parameter \"rescue_window\" "
              unless exists $para->{'rescue_window'};
            return "\t Current the rescue window size is \[0,999\], your window size setting is out of this range, please contact Administrator"
              unless $para->{'rescue_window'} =~ m/\d{1,3}/;
            return "\t cannot find parameter \"rescue_program\" "
              unless exists $para->{'rescue_program'};
            return "\t cannot find rescue program: $raw_qual[$self]"
              unless -e $para->{'rescue_program'};
            $self->set(\@run_rescue,     TRUE);
            $self->set(\@rescue_window,  $para->{'rescue_window'});
            $self->set(\@rescue_program, $para->{'rescue_program'});
        }
        elsif (($para->{'run_rescue'} ne FALSE)) {
            return "\t invalid value $para->{'run_rescue'} assigned to paramter 'run_rescue' in section[$section]";
        }
    }

    # check junction mapping paramters
    $self->set(\@map_junction, FALSE);
    if (exists $para->{'map_junction'}) {
        if ($para->{'map_junction'} eq TRUE) { $self->set(\@map_junction, TRUE) }
        elsif (($para->{'map_junction'} ne FALSE)) {
            return "\t invalid value \"$para->{'map_junction'}\" assigned to paramter \"map_junction\" in section[$section]";
        }
    }

    # check map_ISAS
    $self->set(\@map_ISAS, FALSE);
    if (exists $para->{'map_isas'}) {
        if ($para->{'map_isas'} eq TRUE) { $self->set(\@map_ISAS, TRUE) }
        elsif (($para->{'map_isas'} ne FALSE)) {
            return "\t invalid value \"$para->{'map_ISAS'}\" assigned to paramter \"map_ISAS\" in section[$section]";
        }
    }
    
    
    #  check whether we are mapping base_space:
    $self->set(\@space_code, 0);
    if (exists $para->{'base_space'}) {
        if ($para->{'base_space'} eq TRUE) {
            $self->set(\@space_code, 1);
        }
        elsif ($para->{'base_space'} eq FALSE) {
            $self->set(\@space_code, 0);
        }
        else {
            return "\t invalid value \"$para->{'base_space'}\" assigned to paramter \"base_space\" in section[$section]";
        }
    }

    $self->set(\@qsub_command, "qsub -l walltime=960:00:00");
    if (exists $para->{'qsub_command'}) {
        $self->set(\@qsub_command, $para->{'qsub_command'});
    }

    return TRUE;
}


=item * verify_genome($configSection, \%configSectionData)

verify all the genomic mapping specific configuration values.

=cut

sub verify_genome {
    my ($self, $section, $para, $raw_tag_length) = @_;

    my %check = (
        'mask'           => 0,
        'recursive_maps' => 0,
        'genomes'        => 0,
        max_multimatch   => 0,
        mapreads         => 0,
        schema_dir       => 0);

    foreach my $currentKey (keys %$para) {
        $currentKey =~ tr/[A-Z]/[a-z]/;
        my $currentValue = $para->{$currentKey};
        $check{$currentKey} = 1;

        if ($currentKey eq "max_multimatch") {
            return "\t the allowed value for max_multimatch is between 0-99 in section [$section]"
              unless $currentValue =~ m/^\d{1,2}$/;
            $self->set(\@max_multimatch, $currentValue);
        }
        elsif ($currentKey eq "mask") {
            return "each base value of the mask only allow (0,1) and the length of mask should equal to raw tag length ($raw_tag_length)"
              unless $currentValue =~ m/^[01]{$raw_tag_length}$/;
            $self->set(\@mask, $currentValue);
        }
        elsif ($currentKey eq "recursive_maps") {

            # check each sub mapping parameter unique
            # extract all recursive mapping tag length into the one demension parameter array
            # convert it to an array
            if (ref($currentValue) ne "ARRAY") {
                my @array = ();
                push @array, $currentValue;
                $currentValue = \@array;
            }
            my %hash    = ();
            my $max_mis = 0;
            foreach my $map (@$currentValue) {
                return "\t the \"$map\" of $currentKey should follow the formart: \"tag_length.mismatch.adj\""
                  unless $map =~ m/^\d{2}\.\d\.\d$/;
                my ($l, $mis, $adj) = split(/\./, $map);
                if ($mis > $max_mis) { $max_mis = $mis }
                return "\t tag with same length can't be mapped more than once, see \"$currentKey\" in section [$section]"
                  if exists $hash{$l};
                $hash{$l} = 1;
                push @{$tag_lengths[$$self]}, $l;
            }
            $self->set(\@recursive_maps, $currentValue);
            $self->set(\@max_mismatch,   $max_mis);
        }
        elsif ($currentKey eq "genomes") {
            my @array = ();
            if (ref($currentValue) ne "ARRAY") { push @array, $currentValue; }
            else                               { @array = @$currentValue }
            my %hash = ();
            foreach my $g (@array) {
                return "\t the genome file \"$g\" is not exsting, which listed in section [$section]"
                  unless -e $g;
                return "\t same genome file listed more than once -- \"$g\" in section [$section]"
                  if exists $hash{$g};
                $hash{$g} = 1;
                my $chr = fileparse($g, qr/\.[\w\.]+/);
                push @{$chr_names[$$self]}, $chr;
            }
            $self->set(\@genomes, \@array);
        }
        elsif ($currentKey eq "mapreads") {
            return "\t cannot find program \"$currentValue\" listed in $currentKey in section [$section]"
              unless -e $currentValue;
            $self->set(\@mapreads, $currentValue);
        }
        elsif ($currentKey eq "schema_dir") {
            return "\t cannot find schema directory: \"$currentValue\" listed in section [$section]"
              unless -e $currentValue;
            $self->set(\@schema_dir, $currentValue);
        }
        else { next }
    }

    foreach my $key (keys %check) {
        if ($check{$key} == 0) {
            return "can't find parameter \"$key\" in section [$section]"
              unless $key eq "schema_dir";
            my $dir = dirname($mapreads[$$self]) . "/schemas/";
            $self->set(\@schema_dir, $dir);
        }
    }

    return TRUE;
}


=item * verify_junction($configSection, \%configSectionData)

verify all the junction mapping configuration values.

=cut

sub verify_junction {
    my ($self, $section, $para, $num_juncLib) = @_;
    my %check = ('library' => 0, 'index' => 0);

    foreach my $currentKey (keys %$para) {
        $currentKey =~ tr/[A-Z]/[a-z]/;
        my $currentValue = $para->{$currentKey};
        my @array        = ();
        if (ref($currentValue) ne "ARRAY") { push @array, $currentValue; }
        else                               { @array = @$currentValue }
        if (scalar(@array) != $num_juncLib) {
            return "the number of junction library or index don't equal to the times of recursive mapping";
        }

        foreach my $junc (@array) {
            return "\t the junction library or index file \"$junc\" is not existing, which listed in section [$section]"
              unless -e $junc;

        }
        if ($currentKey eq "junction_library") {
            $self->set(\@junction_library, \@array);
            $check{'library'}++;
        }
        elsif ($currentKey eq "junction_index") {
            $self->set(\@junction_index, \@array);
            $check{'index'}++;
        }
        else {
            return "\t Can't recognise parameter \"$currentKey\", which listed in section [$section]";
        }
    }

    foreach my $key (keys %check) {
        if ($check{$key} == 0) {
            return "can't find  junction $key information in section [$section]";
        }
    }

    # make sure both junction library and index file start same is use mapreads
    if ($map_ISAS[$$self] eq FALSE) {
        for (my $i = 0 ; $i < $num_juncLib ; $i++) {
            my $LJunc = fileparse($junction_library[$$self]->[$i]);
            my $IJunc = fileparse($junction_index[$$self]->[$i]);
            $LJunc =~ s/\.[^\W]+//g;
            $IJunc =~ s/\.[^\W]+//g;
            if ($LJunc ne $IJunc) {
                return
                  "Differnt junction name $LJunc and $IJunc.If you use mapread program, both juction ".
                  "library and index file must follow format: junction_name.suffix. You can use different ".
                  "suffix for both files but the junction name must be same.";
            }
        }
    }

    # check for ISAS
    if ($map_ISAS[$$self] eq TRUE) {
        for (my $i = 0 ; $i < scalar(@{$junction_library[$$self]}) ; $i++) {
            my ($chr, $path, $suffix) = fileparse($junction_library[$$self]->[$i], '\.fa');
            if ($chr !~ m/^chr\d+/i) {
                return "please change chromosome file name to number [1,100], eg. chr90.fa ";
            }
            elsif ($suffix !~ m/^\.fa$/) { return "the suffix of the chromosome file must be \.fa" }
            elsif ($path !~ m/[\\\/]reference[\\\/]/) {
                return "the choromsome file must under directory 'reference' ";
            }
            else {
                $path =~ s/reference[\\\/]//;
                $chr  =~ s/chr//i;
                my $junc_name = fileparse($junction_index[$$self]->[$i]);
                $junc_name =~ s/\.[^\W]+//g;
                push @{$isas_junc[$$self]},
                  QCMG::X_Mate::XConfig::ISASConf->new(
                    'database'   => $path,
                    'chr'        => "$chr,$chr",
                    'mode'       => $isas_genome[$$self]->[$i]->mode,
                    'verbose'    => $isas_genome[$$self]->[$i]->verbose,
                    'limit'      => $isas_genome[$$self]->[$i]->limit,
                    'filter'     => $isas_genome[$$self]->[$i]->filter,
                    'global'     => $isas_genome[$$self]->[$i]->global,
                    'junc_name'  => $junc_name,
                    'space_code' => $space_code[$$self]);
            }
        }
    }

    return TRUE;
}

=item * verify_isas($configSection, \%configSectionData)

verify all the ISAS configuration values.

=cut

sub verify_isas {
    my ($self, $section, $para) = @_;

    # get tag length and max_mismatch for recursive mapping
    return "can't find parameter global in section [$section] " unless exists $para->{'global'};
    if (ref($para->{'global'}) ne "ARRAY") {
        my @array = ();
        push @array, $para->{'global'};
        $para->{'global'} = \@array;
    }

    $self->set(\@max_mismatch, 0);
    foreach my $e (@{$para->{'global'}}) {
        my ($l, $mis) = split(",", $e);
        if ($space_code[$$self] == COLOR_SPACE) {
            return "read length must be 25, or 33 through 62, please reset to 'global' in section [$section]"
              unless (($l == 25) || ($l >= 33 && $l <= 62));
        }
        elsif ($space_code[$$self] == BASE_SPACE) {
            return "read length must be between 25 and 88 in section [$section]"
              unless (($l >= 25) && ($l <= 88));
        }
        push @{$tag_lengths[$$self]}, $l;
        if ($mis > $max_mismatch[$$self]) { $max_mismatch[$$self] = $mis }
    }
    my $num = scalar(@{$tag_lengths[$$self]});

    return "please identify ISAS program name and full path in section [$section]"
      unless exists $para->{isas};
    return "can't find program \"$para->{isas}\" listed in $para->{isas} in section [$section] "
      unless -e $para->{isas};
    $self->set(\@isas, $para->{isas});

    for (my $i = 0 ; $i < scalar(@{$tag_lengths[$$self]}) ; $i++) {
        my $isasConf = undef;
        $isasConf = QCMG::X_Mate::XConfig::ISASConf->new(
            'database'      => $para->{'database'},
            'chr'           => $para->{'chr'},
            'mode'          => $para->{'mode'},
            'verbose'       => $para->{'verbose'},
            'limit'         => $para->{'limit'},
            'filter'        => $para->{'filter'},
            'global'        => $para->{'global'}->[$i],
            'chrRenameFile' => $para->{'chrname_index'},
            'space_code'    => $space_code[$$self]);

        push(@{$isas_genome[$$self]}, $isasConf);
        my $mess = $isas_genome[$$self]->[-1]->check_para($section);
        if ($mess ne TRUE) { return $mess }
    }

    # get all original chromosomes name
    my $hash = $isas_genome[$$self]->[0]->IndexRename;
    foreach my $key (sort(keys %$hash)) { push @{$chr_names[$$self]}, $hash->{$key} }

    return TRUE;
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
