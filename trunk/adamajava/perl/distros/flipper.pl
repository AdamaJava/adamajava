#!/usr/bin/perl -w

##############################################################################
#
#  Program:  flipper.pl
#  Author:   John V Pearson
#  Created:  2010-04-14
#
#  This script is part of the QCMG Perl repository and manages multiple
#  subprojects living within a single subversion repository and sharing
#  modules.
#
#  $Id: flipper.pl 1113 2011-09-22 14:13:32Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use File::Copy;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
#use XML::Simple qw(:strict);
use XML::Simple;

use vars qw( $VERSION $VERBOSE $LOGFH );

( $VERSION ) = '$Revision: 1113 $ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # First things first - if we are not running in the distros
    # directory of a QCMG checkout then drop out immediately.

    my $pattern = '^.*/QCMGPerl/distros[/]*$';
    my $pwd = `pwd`;
    chomp $pwd;
    die "\nERROR: flipper.pl must be invoked from within the distros/ \n",
        "subdirectory of a QCMGPerl project checked out from SVN.\n",
        "The pwd does not match the expected pattern:\n",
        " pattern: [$pattern]\n",
        " pwd:     [$pwd]\n\n"
        unless ($pwd =~ /$pattern/);

    # Setup defaults for important variables.

    my $project        = 'all';
    my $config         = 'flipper.xml';
       $VERBOSE        = 0;
    my $help           = 0;
    my $man            = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'p|project=s'          => \$project,       # -p
           'c|config=s'           => \$config,        # -c
           'v|verbose+'           => \$VERBOSE,       # -v
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    print "\nflipper.pl  v$VERSION  [" . localtime() . "]\n",
          "   project       $project\n",
          "   config file   $config\n",
          "   verbose       $VERBOSE\n\n" if $VERBOSE;

    my $cfg = FlipperConfig->new( file    => $config,
                                  verbose => $VERBOSE );

    # Report on all projects if no project selected
    if ($project =~ /all/i) {
        print $cfg->projects_as_text;
        exit;
    }
    elsif (defined $cfg->project( $project )) {
        print $cfg->project_as_text( $project ), "\n" if $VERBOSE;
        $cfg->purge_directories();

        my $dir = $cfg->project_dir( $project );
        exit unless $dir;  # if there's no project dir then we can ditch

        my @files   = $cfg->project_files( $project );
        my @scripts = $cfg->project_scripts( $project );

        copy( "$dir/$_", '../' ) foreach @files;
        copy( "$dir/src/$_", '../src' ) foreach @scripts;
    }
    else {
        print "[$project] is not a valid project name. Valid names are:\n    ";
        print join("\n    ", $cfg->projects), "\n";

    }
}


##############################################################################
#
#  Module:   FlipperConfig
#  Author:   John V Pearson
#  Created:  2008-04-14
#
#  Convenience class for handling XML configuration file for flipper.pl.
#
##############################################################################

package FlipperConfig;

use strict;
use warnings;
use Data::Dumper;

sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { 'file'    => ( $params{'file'}    || 'flipper.xml' ),
                 'verbose' => ( $params{'verbose'} || 0 ),
                 'xs'      => undef,
                 'rh_xml'  => undef };

    bless $self, $class;

    # Note that this set of XML::Simple options only works well if there
    # are 2 or more projects, otherwise it blows up.  Caveat emptor.
    
    $self->{xs} = XML::Simple->new( ForceArray => [ 'file' ],
                                    KeyAttr    => { directory => '+name',
                                                    project   => '+name' },
                                    GroupTags  => { projects  => 'project',
                                                    files     => 'file',
                                                    keepFiles => 'file' } );

    $self->{rh_xml} = $self->xs->XMLin( $self->file );

    print Dumper $self->rh_xml if ($self->verbose > 1);

    return $self;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub xs {
    my $self = shift;
    return $self->{xs};
}


sub rh_xml {
    my $self = shift;
    return $self->{rh_xml};
}


sub project {
    my $self    = shift;
    my $project = shift;

    return undef unless (exists $self->rh_xml->{projects}->{$project});
    return $self->rh_xml->{projects}->{$project};
}


sub projects {
    my $self = shift;

    return sort keys %{ $self->rh_xml->{projects} };
}



sub directories_to_be_purged {
    my $self = shift;

    return undef unless (exists $self->rh_xml->{directoriesToBePurged});
    return keys %{ $self->rh_xml->{directoriesToBePurged}->{directory} };
}


sub directory_to_be_purged {
    my $self = shift;
    my $dir  = shift;

    return $self->rh_xml->{directoriesToBePurged}->{directory}->{$dir};
}


sub projects_as_text {
    my $self = shift;

    my %projects = %{ $self->rh_xml->{projects} };

    my $text = '';
    foreach my $project (sort keys %projects) {
        $text .= $self->project_as_text( $project ). "\n";
    }

    return $text;
}


sub project_as_text{
    my $self    = shift;
    my $project = shift;

    my $text = join('',
               'Project:   ', $project, "\n",
               '  dir:     ', ($self->project_dir( $project ) || ''), "\n",
               '  files:   ',
               join( "\n           ", 
                     $self->project_files( $project ) ), "\n",
               '  scripts: ',
               join( "\n           ",
                     $self->project_scripts( $project ) ), "\n");

    return $text;
}


sub project_dir {
    my $self    = shift;
    my $project = shift;
    return undef unless (defined $self->project( $project ) and
                         defined $self->project( $project )->{dir});
    return $self->project( $project )->{dir};
}


sub project_files {
    my $self    = shift;
    my $project = shift;
    return ( ) unless (defined $self->project_dir( $project ));
    return @{ $self->project( $project )->{files} };
}


sub project_scripts {
    my $self    = shift;
    my $project = shift;

    return ( ) unless (defined $self->project_dir( $project ));
    my $dir = $self->project_dir( $project );

    my $file = "$dir/MANIFEST";
    my $fh = IO::File->new( $file, 'r' );
    die "Unable to open MANIFEST file [$file] for reading: $!"
        unless defined $fh;

    my @scripts = ();
    my $ctr = 1;
    while (my $line = $fh->getline) {
        chomp $line;
        next unless $line =~ /^src.*\/([^\/]+)$/;
        my $script = $1;
        push @scripts, $script;
    }

    return @scripts;
}


sub purge_directories {
    my $self = shift;

    foreach my $dir ($self->directories_to_be_purged) {
        my $rh_dir = $self->directory_to_be_purged( $dir );
        my %valid_files = map { $_ => 1 } @{ $rh_dir->{keepFiles} };

        # From here on we need $dir to have a trailing '/'
        $dir .= '/' unless ($dir =~ m/\/$/);

        opendir(DIR, $dir) || die "Can't opendir $dir: $ !";
        my @found_files = grep { -f "$dir/$_" } readdir(DIR);
        closedir DIR;
     
        print '  [', scalar(@found_files),
              "] potential files to be purged found for directory [$dir]\n"
            if scalar(@found_files) and $self->verbose;

        # Delete any found files if they are not on the valid list
        foreach my $file (@found_files) {
            if (! exists $valid_files{ $file }) {
                my $delete_name = $dir . $file;
                warn "deleting [$delete_name] ...\n" if $self->verbose;
                unlink $delete_name;
            }
        }
    }
}



__END__

=head1 NAME

flipper.pl - Flip between QCMG perl subprojects


=head1 SYNOPSIS

 flipper.pl [options]

 flipper.pl -p QCMGPerl
 
 flipper.pl -c flipper_test.xml

 flipper.pl -p QCMGPerl -c flipper_test.xml


=head1 ABSTRACT

The QCMGPerl subversion repository contains most of the perl projects
and modeuls developed by the QCMG bioinformatics team. Despite being a
single subversion repository, QCMGPerl contains multiple distributable
projects, each of which contains project-specific
files and scripts plus a number of modules drawn from the common store.
This script manages the file copying processes that allow multiple
independent projects to coexist in a single subversion repository.

=head1 OPTIONS

 -p | --project       name of subproject
 -c | --config        config file; default=flipper.xml
 -v | --verbose       print progress and diagnostic messages
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<flipper.pl> flips a QCMGPerl repository between different
subprojects.

=head2 Philosophy

The process for flipping between projects is:

 1. Read config file
 2. Look at <directoriesToBeCleaned> section of config file and for
    each directory, delete all files that are not named in the
    <keepFiles> element.
 3. For specified project, copy all named files from project directory
    to top directory.
 4. For specified project, read the MANIFEST file in the main project
    directory and work out which scripts should be copied to the 
    main scripts directory.
 5. Do summaries including parsing all scripts and modules named in
    project MANIFEST for "use" dependencies as an aid to keeping the
    project Makefile.PL up-to-date.

=head2 Config file

The config file is XML and starts with a E<lt>directoriesToBeCleaned>
section that lists all of the top level directories that will need to be
purged prior to copying over the project files.  Files that are valid to
stay in each directory (i.e.
they do not belong to any particular project) are listed in a
E<lt>keepFiles> element.
Examples include I<.cvsignore> and I<README.QCMGPerl> in the top
level directory.  All other files in the top
directory will be deleted.  For example:

 <directoriesToBeCleaned>
   <directory>
     <name>../</name>
     <keepFiles>
       <file>.cvsignore</file>
       <file>README.QCMGPerl</file>
     </keepFiles>
   </directory>
   <directory>
     <name>../src</name>
     <keepFiles>
       <file>README.src</file>
     </keepFiles>
   </directory>
 </directoriesToBeCleaned>

A single E<lt>projectE<gt> element follows and it can contain one or more
project-specific blocks as seen here:

 <projects>
   <project>
     <name>QCMGPerl</name>
     <dir>qcmg-perl/</dir>
     <files>
        <file>CHANGES</file>
       <file>INSTALL</file>
        <file>LICENSE</file>
        <file>Makefile.PL</file>
        <file>MANIFEST</file>
     </files>
   </project>

   <project>
     <name>RNA-Mate</name>
     <dir>rna_mate/</dir>
     <files>
       <file>CHANGES</file>
        <file>INSTALL</file>
        <file>LICENSE</file>
        <file>Makefile.PL</file>
        <file>MANIFEST</file>
     </files>
   </project>
 </projects>

=head2 Commandline Options

=over

=item B<-p | --project>

Subproject to be "flipped" to.  Current valid values are: I<qcmg_perl>,
I<seqtools>, I<rna_mate>.
For more details on how to setup a new project, see the section I<Setting
up a new Subproject>.

=item B<-c | --config>

This option should never need to be set by user unless they are
experimenting with an alternate config file.  The default value is
flipper.xml.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will flip QCMGPerl to the rna_mate
project:

  flipper.pl -p rna_mate

B<N.B.> The spaces between the options (B<-p>, B<-c> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: flipper.pl 1113 2011-09-22 14:13:32Z j.pearson $


=head1 COPYRIGHT

  L<http://bioinformatics.qcmg.org/>

=cut
